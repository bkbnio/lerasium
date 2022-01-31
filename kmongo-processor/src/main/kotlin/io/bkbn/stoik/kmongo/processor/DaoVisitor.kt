package io.bkbn.stoik.kmongo.processor

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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import io.bkbn.stoik.core.Domain
import io.bkbn.stoik.core.dao.Dao
import io.bkbn.stoik.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.stoik.utils.KotlinPoetUtils.toCreateRequestClass
import io.bkbn.stoik.utils.KotlinPoetUtils.toEntityClass
import io.bkbn.stoik.utils.KotlinPoetUtils.toResponseClass
import io.bkbn.stoik.utils.KotlinPoetUtils.toUpdateRequestClass
import io.bkbn.stoik.utils.StoikUtils.findParentDomain
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import java.util.UUID

@OptIn(KotlinPoetKspPreview::class)
class DaoVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  companion object {
    private val GetCollection = MemberName("org.litote.kmongo", "getCollection")
    private val FindOneById = MemberName("org.litote.kmongo", "findOneById")
    private val DeleteOneById = MemberName("org.litote.kmongo", "deleteOneById")
    private val Save = MemberName("org.litote.kmongo", "save")
    private val toLDT = MemberName("kotlinx.datetime", "toLocalDateTime")
  }

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Table", classDeclaration)
      return
    }

    val domain = classDeclaration.findParentDomain()

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
      addCreateFunction(cd, crc, rc, ec)
      addReadFunction(rc)
      addUpdateFunction(cd, urc, rc)
      addDeleteFunction()
    }.build())
  }

  private fun TypeSpec.Builder.addCreateFunction(
    cd: KSClassDeclaration,
    requestClass: ClassName,
    responseClass: ClassName,
    entityClass: ClassName
  ) {
    val props = cd.getAllProperties().toList()
    addFunction(FunSpec.builder("create").apply {
      addModifiers(KModifier.OVERRIDE)
      addParameter("request", requestClass)
      returns(responseClass)
      addCode(CodeBlock.builder().apply {
        addStatement("val now = %T.now().%M(%T.UTC)", Clock.System::class, toLDT, TimeZone::class)
        val fieldAssignments = props.joinToString(separator = ",\n") {
          "${it.simpleName.getShortName()} = request.${it.simpleName.getShortName()}"
        }
        addStatement(
          "val entity = %T(\nid = %T.randomUUID(),\n$fieldAssignments,\ncreatedAt = now,\nupdatedAt = now\n)",
          entityClass,
          UUID::class
        )
        addStatement("collection.%M(entity)", Save)
        addStatement("return entity.toResponse()")
      }.build())
    }.build())
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
    val props = cd.getAllProperties().toList()
    addFunction(FunSpec.builder("update").apply {
      addModifiers(KModifier.OVERRIDE)
      addParameter("id", UUID::class)
      addParameter("request", requestClass)
      returns(responseClass)
      addCode(CodeBlock.builder().apply {
        addStatement("val entity = collection.%M(id) ?: error(%P)", FindOneById, "Unable to get entity with id: \$id")
        addStatement("val now = %T.now().%M(%T.UTC)", Clock.System::class, toLDT, TimeZone::class)
        props.forEach { property ->
          val propName = property.simpleName.getShortName()
          addControlFlow("request.%L?.let", propName) {
            addStatement("entity.%L = it", propName)
          }
        }
        addStatement("entity.updatedAt = now")
        addStatement("collection.%M(entity)", Save)
        addStatement("return entity.toResponse()")
      }.build())
    }.build())
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
}
