package io.bkbn.lerasium.mongo.processor.visitor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
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
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import io.bkbn.lerasium.core.request.RequestContext
import io.bkbn.lerasium.persistence.CompositeIndex
import io.bkbn.lerasium.persistence.Index
import io.bkbn.lerasium.utils.KotlinPoetUtils.PERSISTENCE_CONFIG_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlowWithTrailingComma
import io.bkbn.lerasium.utils.KotlinPoetUtils.addObjectInstantiation
import io.bkbn.lerasium.utils.KotlinPoetUtils.isSupportedScalar
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import io.bkbn.lerasium.utils.NestedLerasiumCharter
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.coroutine.CoroutineDatabase
import java.util.UUID

@OptIn(KspExperimental::class)
class RepositoryVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  companion object {
    private val toLDT = MemberName("kotlinx.datetime", "toLocalDateTime")
    private val RunBlocking = MemberName("kotlinx.coroutines", "runBlocking")
  }

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Table", classDeclaration)
      return
    }

    val domain = classDeclaration.getDomain()
    val charter = LerasiumCharter(domain, classDeclaration)

    fileBuilder.addRepository(charter)
  }

  private fun FileSpec.Builder.addRepository(charter: LerasiumCharter) {
    addType(TypeSpec.objectBuilder(charter.domain.name.plus("Repository")).apply {
      addOriginatingKSFile(charter.classDeclaration.containingFile!!)

      addProperty(PropertySpec.builder("db", CoroutineDatabase::class).apply {
        addModifiers(KModifier.PRIVATE)
        initializer("%T.documentDatabase", ClassName(PERSISTENCE_CONFIG_PACKAGE_NAME, "MongoConfig"))
      }.build())

      addProperty(
        PropertySpec.builder(
          "collection",
          CoroutineCollection::class.asTypeName().parameterizedBy(charter.documentClass)
        ).apply {
          addModifiers(KModifier.PRIVATE)
          initializer("db.getCollection()")
        }.build()
      )

      val hasIndices = charter.classDeclaration.getAllProperties().any { it.isAnnotationPresent(Index::class) }
        || charter.classDeclaration.isAnnotationPresent(CompositeIndex::class)

      if (hasIndices) {
        addInitializerBlock(CodeBlock.builder().apply {
          addControlFlow("%M", RunBlocking) {
            charter.classDeclaration.getAllProperties()
              .filter { it.isAnnotationPresent(Index::class) }
              .forEach { indexedProp ->
                val index = indexedProp.getAnnotationsByType(Index::class).first()
                val name = indexedProp.simpleName.getShortName()
                when (index.unique) {
                  true -> addStatement("collection.ensureUniqueIndex(%T::$name)", charter.documentClass)
                  false -> addStatement("collection.ensureIndex(%T::$name)", charter.documentClass)
                }
              }
            charter.classDeclaration.getAnnotationsByType(CompositeIndex::class).forEach { ci ->
              require(ci.fields.size > 1) {
                "Composite index must have at least 2 fields, if applying single field index, use @Index"
              }
              val indexStatement = ci.fields.joinToString(separator = ", ") { "%T::$it" }
              val args = ci.fields.map { charter.documentClass }.toTypedArray()
              when (ci.unique) {
                true -> addStatement("collection.ensureUniqueIndex($indexStatement)", *args)
                false -> addStatement("collection.ensureIndex($indexStatement)", *args)
              }
            }
          }
        }.build())
      }

      addCreateFunction(charter)
      addReadFunction(charter)
      addUpdateFunction(charter)
      addDeleteFunction()
    }.build())
  }

  private fun TypeSpec.Builder.addCreateFunction(charter: LerasiumCharter) {
    val scalarProps = charter.classDeclaration.getAllProperties()
      .filter { it.type.isSupportedScalar() }
      .filterNot { it.simpleName.getShortName() == "id" }
    val nestedProps = charter.classDeclaration.getAllProperties()
      .filterNot { it in scalarProps }
      .filterNot { it.simpleName.getShortName() == "id" }
    addFunction(FunSpec.builder("create").apply {
      returns(charter.domainClass)
      addModifiers(KModifier.SUSPEND)
      addParameter("context", RequestContext::class)
      addParameter("request", charter.apiCreateRequestClass)
      addCodeBlock {
        addStatement("val now = %T.now().%M(%T.UTC)", Clock.System::class, toLDT, TimeZone::class)
        addObjectInstantiation(charter.documentClass, assignment = "val document") {
          addStatement("id = %T.randomUUID(),", UUID::class)
          scalarProps.forEach { prop ->
            val name = prop.simpleName.getShortName()
            addStatement("$name = request.$name,", charter.documentClass)
          }
          nestedProps.forEach { prop -> convertToNestedCreateDocument(charter, prop, "request") }
          addStatement("createdAt = now,")
          addStatement("updatedAt = now,")
        }
      }
      addStatement("collection.save(document)")
      addStatement("return document.to()")
    }.build())
  }

  private fun CodeBlock.Builder.convertToNestedCreateDocument(
    charter: LerasiumCharter,
    property: KSPropertyDeclaration,
    grandparentPropName: String,
  ) {
    val entityClassDeclaration = property.type.resolve().declaration as KSClassDeclaration
    val updatedCharter = NestedLerasiumCharter(classDeclaration = entityClassDeclaration, parentCharter = charter)
    val scalarProps = entityClassDeclaration.getAllProperties()
      .filter { it.type.isSupportedScalar() }
    val nestedProps = entityClassDeclaration.getAllProperties()
      .filterNot { it in scalarProps }
    val parentPropName = property.simpleName.getShortName()
    addControlFlowWithTrailingComma(
      "%L = %L.%L.let { %L ->",
      parentPropName,
      grandparentPropName,
      parentPropName,
      parentPropName
    ) {
      addObjectInstantiation(updatedCharter.documentClass) {
        scalarProps.forEach { prop ->
          val propName = prop.simpleName.getShortName()
          addStatement("%L = %L.%L,", propName, parentPropName, propName)
        }
        nestedProps.forEach { prop ->
          convertToNestedCreateDocument(updatedCharter, prop, parentPropName)
        }
      }
    }
  }

  private fun TypeSpec.Builder.addReadFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("read").apply {
      addModifiers(KModifier.SUSPEND)
      returns(charter.domainClass)
      addParameter("context", RequestContext::class)
      addParameter("id", UUID::class)
      addStatement("val document = collection.findOneById(id) ?: error(%P)", "Unable to get entity with id: \$id")
      addStatement("return document.to()")
    }.build())
  }

  private fun TypeSpec.Builder.addUpdateFunction(charter: LerasiumCharter) {
    val scalarProps = charter.classDeclaration.getAllProperties()
      .filter { it.type.isSupportedScalar() }
      .filterNot { it.simpleName.getShortName() == "id" }
    val nestedProps = charter.classDeclaration.getAllProperties()
      .filterNot { it in scalarProps }
      .filterNot { it.simpleName.getShortName() == "id" }
    addFunction(FunSpec.builder("update").apply {
      addModifiers(KModifier.SUSPEND)
      returns(charter.domainClass)
      addParameter("context", RequestContext::class)
      addParameter("id", UUID::class)
      addParameter("request", charter.apiUpdateRequestClass)
      addCodeBlock {
        addStatement("val document = collection.findOneById(id) ?: error(%P)", "Unable to get entity with id: \$id")
        addStatement("val now = %T.now().%M(%T.UTC)", Clock.System::class, toLDT, TimeZone::class)
        addControlFlow("document.apply") {
          scalarProps.forEach { prop ->
            val name = prop.simpleName.getShortName()
            addStatement("request.%L?.let { %L = it }", name, name)
          }
          nestedProps.forEach { prop ->
            val name = prop.simpleName.getShortName()
            addControlFlow("request.%L?.let", name) {
              convertToNestedUpdateDocument(charter, prop)
            }
          }
        }
        addStatement("document.updatedAt = now")
        addStatement("collection.save(document)")
        addStatement("return document.to()")
      }
    }.build())
  }

  private fun CodeBlock.Builder.convertToNestedUpdateDocument(
    charter: LerasiumCharter,
    property: KSPropertyDeclaration,
  ) {
    val entityClassDeclaration = property.type.resolve().declaration as KSClassDeclaration
    val updatedCharter = NestedLerasiumCharter(classDeclaration = entityClassDeclaration, parentCharter = charter)
    val scalarProps = entityClassDeclaration.getAllProperties()
      .filter { it.type.isSupportedScalar() }
    val nestedProps = entityClassDeclaration.getAllProperties()
      .filterNot { it in scalarProps }
    val parentPropName = property.simpleName.getShortName()
    addControlFlow("%L.apply", parentPropName) {
      scalarProps.forEach { prop ->
        val propName = prop.simpleName.getShortName()
        addStatement("it.%L?.let { %L = it }", propName, propName)
      }
      nestedProps.forEach { prop ->
        val propName = prop.simpleName.getShortName()
        addControlFlow("it.%L?.let", propName) {
          convertToNestedUpdateDocument(updatedCharter, prop)
        }
      }
    }
  }

  private fun TypeSpec.Builder.addDeleteFunction() {
    addFunction(FunSpec.builder("delete").apply {
      addModifiers(KModifier.SUSPEND)
      addParameter("context", RequestContext::class)
      addParameter("id", UUID::class)
      addStatement("collection.deleteOneById(id)")
    }.build())
  }
}
