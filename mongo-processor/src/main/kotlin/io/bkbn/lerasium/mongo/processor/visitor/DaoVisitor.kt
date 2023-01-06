package io.bkbn.lerasium.mongo.processor.visitor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toTypeName
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.dao.Dao
import io.bkbn.lerasium.core.model.CountResponse
import io.bkbn.lerasium.persistence.CompositeIndex
import io.bkbn.lerasium.persistence.Index
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.KotlinPoetUtils.addObjectInstantiation
import io.bkbn.lerasium.utils.KotlinPoetUtils.isSupportedScalar
import io.bkbn.lerasium.utils.KotlinPoetUtils.toCreateRequestClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toEntityClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toResponseClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toUpdateRequestClass
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import java.util.UUID

@OptIn(KspExperimental::class)
class DaoVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  companion object {
    private val GetCollection = MemberName("org.litote.kmongo", "getCollection")
    private val FindOneById = MemberName("org.litote.kmongo", "findOneById")
    private val DeleteOneById = MemberName("org.litote.kmongo", "deleteOneById")
    private val EnsureIndex = MemberName("org.litote.kmongo", "ensureIndex")
    private val EnsureUniqueIndex = MemberName("org.litote.kmongo", "ensureUniqueIndex")
    private val Save = MemberName("org.litote.kmongo", "save")
    private val toLDT = MemberName("kotlinx.datetime", "toLocalDateTime")
  }

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Table", classDeclaration)
      return
    }

    val domain = classDeclaration.getDomain()

    fileBuilder.addDao(classDeclaration, domain)
  }

  private fun FileSpec.Builder.addDao(cd: KSClassDeclaration, domain: Domain) {
    val crc = domain.toCreateRequestClass()
    val urc = domain.toUpdateRequestClass()
    val rc = domain.toResponseClass()
    val ec = domain.toEntityClass()
    addType(TypeSpec.classBuilder(domain.name.plus("Dao")).apply {
      addOriginatingKSFile(cd.containingFile!!)
      addSuperinterface(Dao::class.asTypeName().parameterizedBy(ec, rc, crc, urc))
      primaryConstructor(FunSpec.constructorBuilder().apply {
        addParameter("db", MongoDatabase::class)
      }.build())
      addProperty(PropertySpec.builder("collection", MongoCollection::class.asTypeName().parameterizedBy(ec)).apply {
        addModifiers(KModifier.PRIVATE)
        initializer("db.%M()", GetCollection)
      }.build())
      val hasIndices = cd.getAllProperties().any { it.isAnnotationPresent(Index::class) }
        || cd.isAnnotationPresent(CompositeIndex::class)
      if (hasIndices) {
        addInitializerBlock(CodeBlock.builder().apply {
          cd.getAllProperties()
            .filter { it.isAnnotationPresent(Index::class) }
            .forEach { indexedProp ->
              val index = indexedProp.getAnnotationsByType(Index::class).first()
              val name = indexedProp.simpleName.getShortName()
              when (index.unique) {
                true -> addStatement("collection.%M(%T::$name)", EnsureUniqueIndex, ec)
                false -> addStatement("collection.%M(%T::$name)", EnsureIndex, ec)
              }
            }
          cd.getAnnotationsByType(CompositeIndex::class).forEach { ci ->
            require(ci.fields.size > 1) {
              "Composite index must have at least 2 fields, if applying single field index, use @Index"
            }
            val indexStatement = ci.fields.joinToString(separator = ", ") { "%T::$it" }
            val args = ci.fields.map { ec }.toTypedArray()
            when (ci.unique) {
              true -> addStatement("collection.%M($indexStatement)", EnsureUniqueIndex, *args)
              false -> addStatement("collection.%M($indexStatement)", EnsureIndex, *args)
            }
            // collection.ensureUniqueIndex(ProfileEntity::viewCount, ProfileEntity::mood)
          }
        }.build())
      }
      addCreateFunction(cd, crc, rc, ec)
      addReadFunction(rc)
      addUpdateFunction(cd, urc, rc)
      addDeleteFunction()
      addCountAllFunction()
      addGetAllFunction(rc)
    }.build())
  }

  private fun TypeSpec.Builder.addCreateFunction(
    cd: KSClassDeclaration,
    requestClass: ClassName,
    responseClass: ClassName,
    entityClass: ClassName
  ) {
    addFunction(FunSpec.builder("create").apply {
      addModifiers(KModifier.OVERRIDE)
      addParameter("requests", List::class.asClassName().parameterizedBy(requestClass))
      returns(List::class.asClassName().parameterizedBy(responseClass))
      addCode(CodeBlock.builder().apply {
        addStatement("val now = %T.now().%M(%T.UTC)", Clock.System::class, toLDT, TimeZone::class)
        addControlFlow("val entities = requests.map { request ->") {
          add("val entity = ")
          convertToNewEntity(entityClass, cd, "request", true)
          addStatement("collection.%M(entity)", Save)
          addStatement("entity")
        }
        addStatement("return entities.map { it.toResponse() }")
      }.build())
    }.build())
  }

  private fun CodeBlock.Builder.convertToNewEntity(
    entityClass: ClassName,
    cd: KSClassDeclaration,
    receiverName: String,
    isDomainModel: Boolean = false
  ): Unit = addObjectInstantiation(entityClass) {
    val props = cd.getAllProperties().toList()
    if (isDomainModel) {
      addStatement("id = %T.randomUUID(),", UUID::class)
      addStatement("createdAt = now,")
      addStatement("updatedAt = now,")
    }
    props.forEach { prop ->
      val propName = prop.simpleName.getShortName()
      when (prop.type.isSupportedScalar()) {
        true -> addStatement("$propName = $receiverName.$propName,")
        false -> {
          addControlFlow("$propName = $receiverName.$propName.let { $propName ->") {
            val propTn = (prop.type.toTypeName() as ClassName).toEntityClass()
            val propDef = prop.type.resolve().declaration as KSClassDeclaration
            convertToNewEntity(propTn, propDef, propName)
          }
        }
      }
    }
  }

  private fun TypeSpec.Builder.addReadFunction(responseClass: ClassName) {
    addFunction(FunSpec.builder("read").apply {
      addModifiers(KModifier.OVERRIDE)
      returns(responseClass)
      addParameter("id", UUID::class)
      addCode(CodeBlock.builder().apply {
        addStatement("val entity = collection.%M(id) ?: error(%P)", FindOneById, "Unable to get entity with id: \$id")
        addStatement("return entity.toResponse()")
      }.build())
    }.build())
  }

  private fun TypeSpec.Builder.addUpdateFunction(
    cd: KSClassDeclaration,
    requestClass: ClassName,
    responseClass: ClassName,
  ) {
    addFunction(FunSpec.builder("update").apply {
      addModifiers(KModifier.OVERRIDE)
      addParameter("id", UUID::class)
      addParameter("request", requestClass)
      returns(responseClass)
      addCode(CodeBlock.builder().apply {
        addStatement("val entity = collection.%M(id) ?: error(%P)", FindOneById, "Unable to get entity with id: \$id")
        addStatement("val now = %T.now().%M(%T.UTC)", Clock.System::class, toLDT, TimeZone::class)
        addEntityUpdates(cd)
        addStatement("entity.updatedAt = now")
        addStatement("collection.%M(entity)", Save)
        addStatement("return entity.toResponse()")
      }.build())
    }.build())
  }

  private fun CodeBlock.Builder.addEntityUpdates(
    cd: KSClassDeclaration,
    requestName: String = "request",
    entityName: String = "entity"
  ) {
    val props = cd.getAllProperties().toList()
    props.forEach { property ->
      val propName = property.simpleName.getShortName()
      when (property.type.isSupportedScalar()) {
        true -> {
          addControlFlow("$requestName.%L?.let", propName) {
            addStatement("$entityName.%L = it", propName)
          }
        }

        false -> {
          addControlFlow("$requestName.$propName?.let") {
            addControlFlow("$entityName.$propName.let { $propName ->") {
              val propDef = property.type.resolve().declaration as KSClassDeclaration
              addEntityUpdates(propDef, "it", propName)
            }
          }
        }
      }
    }
  }

  private fun TypeSpec.Builder.addDeleteFunction() {
    addFunction(FunSpec.builder("delete").apply {
      addModifiers(KModifier.OVERRIDE)
      addParameter("id", UUID::class)
      addCode(CodeBlock.builder().apply {
        addStatement("collection.%M(id)", DeleteOneById)
      }.build())
    }.build())
  }

  private fun TypeSpec.Builder.addCountAllFunction() {
    addFunction(FunSpec.builder("countAll").apply {
      addModifiers(KModifier.OVERRIDE)
      returns(CountResponse::class)
      addCode(CodeBlock.builder().apply {
        addStatement("val count = collection.countDocuments()")
        addStatement("return %T(count)", CountResponse::class)
      }.build())
    }.build())
  }

  private fun TypeSpec.Builder.addGetAllFunction(responseClass: ClassName) {
    addFunction(FunSpec.builder("getAll").apply {
      addModifiers(KModifier.OVERRIDE)
      returns(List::class.asClassName().parameterizedBy(responseClass))
      addParameter(ParameterSpec.builder("chunk", Int::class).build())
      addParameter(ParameterSpec.builder("offset", Int::class).build())
      addCode(CodeBlock.builder().apply {
        addStatement("val entities = collection.find().skip(chunk * offset).limit(chunk)")
        addControlFlow("return entities.toList().map { entity ->") {
          addStatement("entity.toResponse()")
        }
      }.build())
    }.build())
  }
}
