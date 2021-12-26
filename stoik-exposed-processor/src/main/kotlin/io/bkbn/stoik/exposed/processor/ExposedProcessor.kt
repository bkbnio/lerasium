package io.bkbn.stoik.exposed.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.writeTo
import io.bkbn.stoik.exposed.Table
import io.bkbn.stoik.exposed.processor.util.StringHelpers.snakeToUpperCamelCase

@OptIn(KotlinPoetKspPreview::class)
class ExposedProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  options: Map<String, String>
) : SymbolProcessor {

  companion object {
    private const val DEFAULT_VARCHAR_SIZE = 128
  }

  init {
    logger.info(options.toString())
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = resolver
      .getSymbolsWithAnnotation(Table::class.qualifiedName!!)
      .filterIsInstance<KSClassDeclaration>()

    if (!symbols.iterator().hasNext()) return emptyList()

    symbols.forEach {
      val fb = FileSpec.builder("io.bkbn.stoik.generated", "Tables")
      it.accept(Visitor(fb), Unit)
      val fs = fb.build()
      fs.writeTo(codeGenerator, false)
    }

    return symbols.filterNot { it.validate() }.toList()
  }

  inner class Visitor(private val fileBuilder: FileSpec.Builder) : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
      if (classDeclaration.classKind != ClassKind.INTERFACE) {
        logger.error("Only an interface can be decorated with @Table", classDeclaration)
        return
      }

      val annotation: KSAnnotation = classDeclaration.annotations.first {
        it.shortName.asString() == Table::class.simpleName
      }

      val nameArgument: KSValueArgument = annotation.arguments
        .first { arg -> arg.name?.asString() == "name" }

      val tableName = nameArgument.value as String
      val tableObjectName = tableName.snakeToUpperCamelCase().plus("Table")

      fileBuilder.addType(TypeSpec.objectBuilder(tableObjectName).apply {
        addOriginatingKSFile(classDeclaration.containingFile ?: error("Could not identify originating file :("))
        superclass(ClassName("org.jetbrains.exposed.dao.id", "UUIDTable"))
        addSuperclassConstructorParameter("%S", tableName)

        val properties: Sequence<KSPropertyDeclaration> = classDeclaration.getAllProperties()
          .filter { it.validate() }

        properties.forEach { property ->
          // todo get column name from annotation
          val fieldName = property.simpleName.asString()
          val columnType = ClassName("org.jetbrains.exposed.sql", "Column").parameterizedBy(String::class.asTypeName())
          addProperty(PropertySpec.builder(fieldName, columnType).apply {
            initializer("varchar(%S, %L)", fieldName, DEFAULT_VARCHAR_SIZE)
          }.build())
        }

      }.build())
    }
  }

  // org.jetbrains.exposed.sql.Column
  // val firstName: Column<String> = varchar("first_name", 128)
}
