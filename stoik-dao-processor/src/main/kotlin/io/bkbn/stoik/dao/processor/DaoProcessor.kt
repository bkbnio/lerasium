package io.bkbn.stoik.dao.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import io.bkbn.stoik.dao.core.Dao
import kotlinx.serialization.Serializable
import java.util.UUID

@OptIn(KotlinPoetKspPreview::class, KspExperimental::class)
class DaoProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  options: Map<String, String>
) : SymbolProcessor {

  init {
    logger.info(options.toString())
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = resolver
      .getSymbolsWithAnnotation(Dao::class.qualifiedName!!)
      .filterIsInstance<KSClassDeclaration>()

    if (!symbols.iterator().hasNext()) return emptyList()

    symbols.forEach {
      val fb = FileSpec.builder("io.bkbn.stoik.generated", "Dao")
      it.accept(Visitor(fb), Unit)
      val fs = fb.build()
      fs.writeTo(codeGenerator, false)
    }

    return symbols.filterNot { it.validate() }.toList()
  }

  inner class Visitor(private val fileBuilder: FileSpec.Builder) : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
      if (classDeclaration.classKind != ClassKind.INTERFACE) {
        logger.error("Only an interface can be decorated with @Dao", classDeclaration)
        return
      }

      val dao: Dao = classDeclaration.getAnnotationsByType(Dao::class).first()

      val entityName = dao.name
      val daoName = "${entityName}Dao"

      val properties: Sequence<KSPropertyDeclaration> = classDeclaration.getAllProperties()
        .filter { it.validate() }

      fileBuilder.addType(TypeSpec.classBuilder(daoName).apply {
        addModifiers(KModifier.OPEN)
        addCreateFunction(entityName)
        addReadFunction(entityName)
        addUpdateFunction(entityName)
        addDeleteFunction()
      }.build())

      fileBuilder.addCreateRequestType(entityName, properties)
      fileBuilder.addUpdateRequestType(entityName, properties)
      fileBuilder.addResponseType(entityName, properties)
    }

    private fun TypeSpec.Builder.addCreateFunction(
      entityName: String
    ) = addFunction(FunSpec.builder("create").apply {
      addParameter("request", ClassName("io.bkbn.stoik.generated", "Create${entityName}Request"))
      returns(ClassName("io.bkbn.stoik.generated", "${entityName}Response"))
      addComment("TODO")
    }.build())

    private fun TypeSpec.Builder.addReadFunction(
      entityName: String
    ) = addFunction(FunSpec.builder("read").apply {
      addParameter("id", UUID::class)
      returns(ClassName("io.bkbn.stoik.generated", "${entityName}Response"))
      addComment("TODO")
    }.build())

    private fun TypeSpec.Builder.addUpdateFunction(
      entityName: String
    ) = addFunction(FunSpec.builder("update").apply {
      addParameter("id", UUID::class)
      addParameter("request", ClassName("io.bkbn.stoik.generated", "Update${entityName}Request"))
      returns(ClassName("io.bkbn.stoik.generated", "${entityName}Response"))
      addComment("TODO")
    }.build())

    private fun TypeSpec.Builder.addDeleteFunction() = addFunction(FunSpec.builder("delete").apply {
      addParameter("id", UUID::class)
      returns(Unit::class)
      addComment("TODO")
    }.build())

    private fun FileSpec.Builder.addCreateRequestType(
      entityName: String,
      properties: Sequence<KSPropertyDeclaration>
    ) = addType(TypeSpec.classBuilder("Create${entityName}Request").apply {
      addAnnotation(Serializable::class)
      addModifiers(KModifier.DATA)
      addBaseProperties(properties)
    }.build())

    private fun FileSpec.Builder.addUpdateRequestType(
      entityName: String,
      properties: Sequence<KSPropertyDeclaration>
    ) = addType(TypeSpec.classBuilder("Update${entityName}Request").apply {
      addAnnotation(Serializable::class)
      addModifiers(KModifier.DATA)
      primaryConstructor(FunSpec.constructorBuilder().apply {
        properties.forEach { property ->
          addParameter(
            ParameterSpec.builder(
              property.simpleName.asString(),
              property.type.toTypeName().copy(nullable = true)
            ).build()
          )
        }
      }.build())
      properties.forEach { property ->
        addProperty(
          PropertySpec.builder(
            property.simpleName.asString(),
            property.type.toTypeName().copy(nullable = true)
          ).apply {
            initializer(property.simpleName.asString())
          }.build()
        )
      }
    }.build())

    private fun FileSpec.Builder.addResponseType(
      entityName: String,
      properties: Sequence<KSPropertyDeclaration>
    ) = addType(TypeSpec.classBuilder("${entityName}Response").apply {
      addAnnotation(Serializable::class)
      addModifiers(KModifier.DATA)
      addBaseProperties(properties)
    }.build())

    private fun TypeSpec.Builder.addBaseProperties(properties: Sequence<KSPropertyDeclaration>) {
      primaryConstructor(FunSpec.constructorBuilder().apply {
        properties.forEach { property ->
          addParameter(ParameterSpec.builder(property.simpleName.asString(), property.type.toTypeName()).build())
        }
      }.build())
      properties.forEach { property ->
        addProperty(PropertySpec.builder(property.simpleName.asString(), property.type.toTypeName()).apply {
          initializer(property.simpleName.asString())
        }.build())
      }
    }
  }
}
