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
import com.squareup.kotlinpoet.FunSpec
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
import io.bkbn.stoik.exposed.VarChar
import io.bkbn.stoik.utils.StringUtils.snakeToUpperCamelCase
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

@OptIn(KotlinPoetKspPreview::class)
class ExposedProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  options: Map<String, String>
) : SymbolProcessor {

  companion object {
    private const val DEFAULT_VARCHAR_SIZE = 128
    private const val BASE_PACKAGE_NAME = "io.bkbn.stoik.generated"
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
      val entityName = tableName.snakeToUpperCamelCase().plus("Entity")
      val containingFile = classDeclaration.containingFile ?: error("Could not identify originating file :(")
      val properties: Sequence<KSPropertyDeclaration> = classDeclaration.getAllProperties()
        .filter { it.validate() }

      fileBuilder.createTableObject(tableName, tableObjectName, containingFile, properties)
      fileBuilder.createEntityClass(entityName, tableObjectName, containingFile, properties)
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
      val columnAnnotation: Column? = property.getAnnotationsByType(Column::class).firstOrNull()
      val fieldName = property.simpleName.asString()
      val columnName = columnAnnotation?.name ?: fieldName
      val columnType = property.toColumnType()
      addProperty(PropertySpec.builder(fieldName, columnType).apply {
        setColumnInitializer(columnName, property)
      }.build())
    }
  }.build())

  private fun FileSpec.Builder.createEntityClass(
    entityName: String,
    tableObjectName: String,
    containingFile: KSFile,
    properties: Sequence<KSPropertyDeclaration>,
  ) = addType(TypeSpec.classBuilder(entityName).apply {
    addOriginatingKSFile(containingFile)
    superclass(UUIDEntity::class)
    addSuperclassConstructorParameter("id")
    addEntityConstructor()
    addEntityCompanion(entityName, tableObjectName)
    val tableObject = ClassName(BASE_PACKAGE_NAME, tableObjectName)
    properties.forEach { property ->
      val fieldName = property.simpleName.asString()
      val fieldType = property.type.toTypeName()
      addProperty(PropertySpec.builder(fieldName, fieldType).apply {
        delegate("%T.$fieldName", tableObject)
        mutable()
      }.build())
    }
  }.build())

  private fun TypeSpec.Builder.addEntityConstructor() {
    primaryConstructor(FunSpec.constructorBuilder().apply {
      addParameter("id", EntityID::class.asTypeName().parameterizedBy(UUID::class.asTypeName()))
    }.build())
  }

  private fun TypeSpec.Builder.addEntityCompanion(entityName: String, tableObjectName: String) {
    addType(TypeSpec.companionObjectBuilder().apply {
      val entityClass = ClassName(BASE_PACKAGE_NAME, entityName)
      val tableObject = ClassName(BASE_PACKAGE_NAME, tableObjectName)
      superclass(UUIDEntityClass::class.asTypeName().parameterizedBy(entityClass))
      addSuperclassConstructorParameter("%T", tableObject)
    }.build())
  }

  private fun KSPropertyDeclaration.toColumnType(): TypeName {
    val columnBase = ClassName("org.jetbrains.exposed.sql", "Column")
    return columnBase.parameterizedBy(type.toTypeName())
  }

  private fun PropertySpec.Builder.setColumnInitializer(columnName: String, property: KSPropertyDeclaration) {
    when (property.type.toTypeName()) {
      String::class.asTypeName() -> initializer("varchar(%S, %L)", columnName, determineVarCharSize(property))
      Int::class.asTypeName() -> initializer("integer(%S)", columnName)
      Long::class.asTypeName() -> initializer("long(%S)", columnName)
      Boolean::class.asTypeName() -> initializer("bool(%S)", columnName)
      Float::class.asTypeName() -> initializer("float(%S)", columnName)
      else -> TODO()
    }
  }

  @OptIn(KspExperimental::class)
  private fun determineVarCharSize(property: KSPropertyDeclaration): Int {
    val varCharAnnotation = property.getAnnotationsByType(VarChar::class).firstOrNull()
    return varCharAnnotation?.size ?: DEFAULT_VARCHAR_SIZE
  }
}
