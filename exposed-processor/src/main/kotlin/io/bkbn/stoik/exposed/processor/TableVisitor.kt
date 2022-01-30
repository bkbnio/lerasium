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
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toTypeName
import io.bkbn.stoik.core.Domain
import io.bkbn.stoik.core.model.Entity
import io.bkbn.stoik.exposed.Column
import io.bkbn.stoik.exposed.VarChar
import io.bkbn.stoik.utils.KotlinPoetUtils.toResponseClass
import io.bkbn.stoik.utils.StoikUtils.findParentDomain
import io.bkbn.stoik.utils.StringUtils.camelToSnakeCase
import io.bkbn.stoik.utils.StringUtils.pascalToSnakeCase
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import java.util.UUID

@OptIn(KspExperimental::class, KotlinPoetKspPreview::class)
class TableVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  companion object {
    private const val DEFAULT_VARCHAR_SIZE = 128
    val exposedColumn = ClassName("org.jetbrains.exposed.sql", "Column")
    val exposedDateTime = MemberName("org.jetbrains.exposed.sql.kotlin.datetime", "datetime")
  }

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Table", classDeclaration)
      return
    }

    val domain = classDeclaration.findParentDomain()

    fileBuilder.addTable(classDeclaration, domain)
    fileBuilder.addEntity(classDeclaration, domain)
  }

  private fun FileSpec.Builder.addTable(cd: KSClassDeclaration, domain: Domain) {
    val properties = cd.getAllProperties().toList()
    addType(TypeSpec.objectBuilder(domain.name.plus("Table")).apply {
      addOriginatingKSFile(cd.containingFile!!)
      superclass(UUIDTable::class)
      addSuperclassConstructorParameter("%S", domain.name.pascalToSnakeCase())
      properties.forEach { property ->
        val columnAnnotation: Column? = property.getAnnotationsByType(Column::class).firstOrNull()
        val fieldName = property.simpleName.asString()
        val columnName = columnAnnotation?.name ?: fieldName
        val columnType = property.toColumnType()
        addProperty(PropertySpec.builder(fieldName, columnType).apply {
          setColumnInitializer(columnName, property)
        }.build())
      }
      addProperty(
        PropertySpec.builder("createdAt", exposedColumn.parameterizedBy(LocalDateTime::class.asTypeName())).apply {
          initializer("%M(%S)", exposedDateTime, "created_at")
        }.build()
      )
      addProperty(
        PropertySpec.builder("updatedAt", exposedColumn.parameterizedBy(LocalDateTime::class.asTypeName())).apply {
          initializer("%M(%S)", exposedDateTime, "updated_at")
        }.build()
      )
    }.build())
  }

  private fun FileSpec.Builder.addEntity(cd: KSClassDeclaration, domain: Domain) {
    val properties = cd.getAllProperties().toList()
    val tableObject = ClassName(this.packageName, domain.name.plus("Table"))
    val entityClass = ClassName(this.packageName, domain.name.plus("Entity"))
    addType(TypeSpec.classBuilder(domain.name.plus("Entity")).apply {
      addOriginatingKSFile(cd.containingFile!!)
      addSuperinterface(Entity::class.asClassName().parameterizedBy(domain.toResponseClass()))
      superclass(UUIDEntity::class)
      addSuperclassConstructorParameter("id")
      primaryConstructor(FunSpec.constructorBuilder().apply {
        addParameter("id", EntityID::class.parameterizedBy(UUID::class))
      }.build())
      addType(TypeSpec.companionObjectBuilder().apply {
        superclass(UUIDEntityClass::class.asTypeName().parameterizedBy(entityClass))
        addSuperclassConstructorParameter("%T", tableObject)
      }.build())
      addFunction(FunSpec.builder("toResponse").apply {
        addModifiers(KModifier.OVERRIDE)
        returns(domain.toResponseClass())
        addCode("TODO()")
      }.build())
      properties.forEach { property ->
        val fieldName = property.simpleName.asString()
        val fieldType = property.type.toTypeName()
        addProperty(PropertySpec.builder(fieldName, fieldType).apply {
          delegate("%T.$fieldName", tableObject)
          mutable()
        }.build())
      }
      addProperty(PropertySpec.builder("createdAt", LocalDateTime::class).apply {
        delegate("%T.createdAt", tableObject)
        mutable()
      }.build())
      addProperty(PropertySpec.builder("updatedAt", LocalDateTime::class).apply {
        delegate("%T.updatedAt", tableObject)
        mutable()
      }.build())
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
