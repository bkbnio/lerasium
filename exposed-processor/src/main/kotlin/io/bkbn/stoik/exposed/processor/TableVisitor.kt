package io.bkbn.stoik.exposed.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toTypeName
import io.bkbn.stoik.exposed.Column
import io.bkbn.stoik.exposed.VarChar
import io.bkbn.stoik.utils.StoikUtils.findParentDomain
import io.bkbn.stoik.utils.StringUtils.camelToSnakeCase
import io.bkbn.stoik.utils.StringUtils.pascalToSnakeCase
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID

@OptIn(KspExperimental::class, KotlinPoetKspPreview::class)
class TableVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  companion object {
    private const val DEFAULT_VARCHAR_SIZE = 128
  }

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Table", classDeclaration)
      return
    }

    val domainName = classDeclaration.findParentDomain().name

    fileBuilder.addTable(classDeclaration, domainName)
    fileBuilder.addEntity(classDeclaration, domainName)
  }

  private fun FileSpec.Builder.addTable(cd: KSClassDeclaration, domainName: String) {
    val properties = cd.getAllProperties().toList()
    addType(TypeSpec.objectBuilder(domainName.plus("Table")).apply {
      superclass(UUIDTable::class)
      addSuperclassConstructorParameter("%S", domainName.pascalToSnakeCase())
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
  }

  private fun FileSpec.Builder.addEntity(cd: KSClassDeclaration, domainName: String) {
    val properties = cd.getAllProperties().toList()
    val tableObject = ClassName(this.packageName, domainName.plus("Table"))
    val entityClass = ClassName(this.packageName, domainName.plus("Entity"))
    addType(TypeSpec.classBuilder(domainName.plus("Entity")).apply {
      superclass(UUIDEntity::class)
      addSuperclassConstructorParameter("id")
      primaryConstructor(FunSpec.constructorBuilder().apply {
        addParameter("id", EntityID::class.parameterizedBy(UUID::class))
      }.build())
      addType(TypeSpec.companionObjectBuilder().apply {
        superclass(UUIDEntityClass::class.asTypeName().parameterizedBy(entityClass))
        addSuperclassConstructorParameter("%T", tableObject)
      }.build())
      properties.forEach { property ->
        val fieldName = property.simpleName.asString()
        val fieldType = property.type.toTypeName()
        addProperty(PropertySpec.builder(fieldName, fieldType).apply {
          delegate("%T.$fieldName", tableObject)
          mutable()
        }.build())
      }
    }.build())
  }

  private fun KSPropertyDeclaration.toColumnType(): TypeName {
    val columnBase = ClassName("org.jetbrains.exposed.sql", "Column")
    return columnBase.parameterizedBy(type.toTypeName())
  }

  private fun PropertySpec.Builder.setColumnInitializer(fieldName: String, property: KSPropertyDeclaration) {
    val columnName = fieldName.camelToSnakeCase()
    when (property.type.toTypeName()) {
      String::class.asTypeName() -> initializer("varchar(%S, %L)", columnName, determineVarCharSize(property))
      Int::class.asTypeName() -> initializer("integer(%S)", columnName)
      Long::class.asTypeName() -> initializer("long(%S)", columnName)
      Boolean::class.asTypeName() -> initializer("bool(%S)", columnName)
      Float::class.asTypeName() -> initializer("float(%S)", columnName)
      else -> TODO()
    }
  }

  private fun determineVarCharSize(property: KSPropertyDeclaration): Int {
    val varCharAnnotation = property.getAnnotationsByType(VarChar::class).firstOrNull()
    return varCharAnnotation?.size ?: DEFAULT_VARCHAR_SIZE
  }
}
