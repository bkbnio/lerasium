package io.bkbn.stoik.exposed.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import io.bkbn.stoik.exposed.Column
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
      val containingFile = classDeclaration.containingFile ?: error("Could not identify originating file :(")
      val properties: Sequence<KSPropertyDeclaration> = classDeclaration.getAllProperties()
        .filter { it.validate() }

      fileBuilder.createTableObject(tableName, tableObjectName, containingFile, properties)
    }
  }

  @OptIn(KspExperimental::class)
  private fun FileSpec.Builder.createTableObject(
    tableName: String,
    tableObjectName: String,
    containingFile: KSFile,
    properties: Sequence<KSPropertyDeclaration>
  ) = addType(TypeSpec.objectBuilder(tableObjectName).apply {
    addOriginatingKSFile(containingFile)
    superclass(ClassName("org.jetbrains.exposed.dao.id", "UUIDTable"))
    addSuperclassConstructorParameter("%S", tableName)
    properties.forEach { property ->
      val columnAnnotation: Column = property.getAnnotationsByType(Column::class).first()
      val fieldName = property.simpleName.asString()
      val columnName = columnAnnotation.name.ifBlank { property.simpleName.asString() }
      val columnType = property.toColumnType()
      addProperty(PropertySpec.builder(fieldName, columnType).apply {
        setColumnInitializer(columnName, property)
      }.build())
    }
  }.build())

  private fun KSPropertyDeclaration.toColumnType(): TypeName {
    val columnBase = ClassName("org.jetbrains.exposed.sql", "Column")
    return columnBase.parameterizedBy(type.toTypeName())
  }

  private fun PropertySpec.Builder.setColumnInitializer(columnName: String, property: KSPropertyDeclaration) {
    when (property.type.toTypeName()) {
      String::class.asTypeName() -> initializer("varchar(%S, %L)", columnName, DEFAULT_VARCHAR_SIZE)
      Int::class.asTypeName() -> initializer("integer(%S)", columnName)
      Boolean::class.asTypeName() -> initializer("bool(%S)", columnName)
      else -> TODO()
    }
  }
}
