package io.bkbn.lerasium.rdbms.processor

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
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toTypeName
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.model.Entity
import io.bkbn.lerasium.persistence.CompositeIndex
import io.bkbn.lerasium.persistence.Index
import io.bkbn.lerasium.rdbms.Column
import io.bkbn.lerasium.rdbms.ForeignKey
import io.bkbn.lerasium.rdbms.OneToMany
import io.bkbn.lerasium.rdbms.VarChar
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.KotlinPoetUtils.toEntityClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toResponseClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toTableClass
import io.bkbn.lerasium.utils.LerasiumUtils.findParentDomain
import io.bkbn.lerasium.utils.StringUtils.camelToSnakeCase
import io.bkbn.lerasium.utils.StringUtils.pascalToSnakeCase
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.SizedIterable
import java.util.UUID
import org.jetbrains.exposed.sql.Column as ExposedColumn

@OptIn(KspExperimental::class, KotlinPoetKspPreview::class)
class TableVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  companion object {
    private const val DEFAULT_VARCHAR_SIZE = 128
    val exposedColumn = ClassName("org.jetbrains.exposed.sql", "Column")
    val exposedDateTime = MemberName("org.jetbrains.exposed.sql.kotlin.datetime", "datetime")
    val memberProps = MemberName("kotlin.reflect.full", "memberProperties")
    val valueParams = MemberName("kotlin.reflect.full", "valueParameters")
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
    val properties = cd.getAllProperties().filterNot { it.isAnnotationPresent(OneToMany::class) }.toList()
    addType(TypeSpec.objectBuilder(domain.name.plus("Table")).apply {
      addOriginatingKSFile(cd.containingFile!!)
      superclass(UUIDTable::class)
      addSuperclassConstructorParameter("%S", domain.name.pascalToSnakeCase())
      properties.forEach { property ->
        val columnAnnotation: Column? = property.getAnnotationsByType(Column::class).firstOrNull()
        val fieldName = property.simpleName.asString()
        val columnName = columnAnnotation?.name ?: fieldName
        val columnType = if (property.isAnnotationPresent(ForeignKey::class)) {
          ExposedColumn::class.asTypeName()
            .parameterizedBy(EntityID::class.asTypeName().parameterizedBy(UUID::class.asTypeName()))
        } else {
          property.toColumnType()
        }
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

      if (cd.isAnnotationPresent(CompositeIndex::class)) {
        addInitializerBlock(CodeBlock.builder().apply {
          cd.getAnnotationsByType(CompositeIndex::class).forEach { ci ->
            require(ci.fields.size > 1) {
              "Composite index must have at least 2 fields, if applying single field index, use @Index"
            }
            addStatement("index(%L, %L)", ci.unique, ci.fields.joinToString(", "))
          }
        }.build())
      }
    }.build())
  }

  private fun FileSpec.Builder.addEntity(cd: KSClassDeclaration, domain: Domain) {
    addType(TypeSpec.classBuilder(domain.name.plus("Entity")).apply {
      addEntityTypeInfo(cd, domain)
      addResponseConverter(cd, domain)
      addEntityProperties(cd, domain)
      addRelations(cd)
    }.build())
  }

  private fun TypeSpec.Builder.addEntityTypeInfo(cd: KSClassDeclaration, domain: Domain) {
    addOriginatingKSFile(cd.containingFile!!)
    addSuperinterface(Entity::class.asClassName().parameterizedBy(domain.toResponseClass()))
    superclass(UUIDEntity::class)
    addSuperclassConstructorParameter("id")
    primaryConstructor(FunSpec.constructorBuilder().apply {
      addParameter("id", EntityID::class.parameterizedBy(UUID::class))
    }.build())
    addType(TypeSpec.companionObjectBuilder().apply {
      superclass(UUIDEntityClass::class.asTypeName().parameterizedBy(domain.toEntityClass()))
      addSuperclassConstructorParameter("%T", domain.toTableClass())
    }.build())
  }

  private fun TypeSpec.Builder.addEntityProperties(cd: KSClassDeclaration, domain: Domain) {
    val properties = cd.getAllProperties().filterNot { it.isAnnotationPresent(OneToMany::class) }.toList()
    properties.forEach { property ->
      val fieldName = property.simpleName.asString()
      val fieldType = property.type.toTypeName()
      if (property.isAnnotationPresent(ForeignKey::class)) {
        val fkDomain =
          (property.type.resolve().declaration as KSClassDeclaration).getAnnotationsByType(Domain::class).first()
        addProperty(PropertySpec.builder(fieldName, fkDomain.toEntityClass()).apply {
          delegate("%T referencedOn %T.%L", fkDomain.toEntityClass(), domain.toTableClass(), fieldName)
          mutable()
        }.build())
      } else {
        addProperty(PropertySpec.builder(fieldName, fieldType).apply {
          delegate("%T.$fieldName", domain.toTableClass())
          mutable()
        }.build())
      }

    }
    addProperty(PropertySpec.builder("createdAt", LocalDateTime::class).apply {
      delegate("%T.createdAt", domain.toTableClass())
      mutable()
    }.build())
    addProperty(PropertySpec.builder("updatedAt", LocalDateTime::class).apply {
      delegate("%T.updatedAt", domain.toTableClass())
      mutable()
    }.build())
  }

  private fun TypeSpec.Builder.addRelations(cd: KSClassDeclaration) {
    val properties = cd.getAllProperties().filter { it.isAnnotationPresent(OneToMany::class) }.toList()
    properties.forEach { prop ->
      val name = prop.simpleName.getShortName()
      val otm = prop.getAnnotationsByType(OneToMany::class).first()
      val propClazz = prop.type.resolve().declaration as KSClassDeclaration
      require(propClazz.isAnnotationPresent(Domain::class)) {
        "You are trying to build a relation to $propClazz which is not annotated with @Domain"
      }
      val refDomain = propClazz.getAnnotationsByType(Domain::class).first()
      addProperty(
        PropertySpec.builder(
          name,
          SizedIterable::class.asClassName().parameterizedBy(refDomain.toEntityClass())
        ).apply {
          delegate("%T referrersOn %T.${otm.refColumn}", refDomain.toEntityClass(), refDomain.toTableClass())
        }.build()
      )
    }
  }

  private fun TypeSpec.Builder.addResponseConverter(cd: KSClassDeclaration, domain: Domain) {
    val properties = cd.getAllProperties().filterNot { it.isAnnotationPresent(OneToMany::class) }.toList()
    addFunction(FunSpec.builder("toResponse").apply {
      addModifiers(KModifier.OVERRIDE)
      returns(domain.toResponseClass())
      addCode(CodeBlock.builder().apply {
        addControlFlow("return with(::%T)", domain.toResponseClass()) {
          addStatement(
            "val propertiesByName = %T::class.%M.associateBy { it.name }",
            domain.toEntityClass(),
            memberProps
          )
          addControlFlow("val params = %M.associateWith", valueParams) {
            addControlFlow("when (it.name)") {
              addStatement("%T::id.name -> id.value", domain.toResponseClass())
              properties.filter {
                (it.type.resolve().declaration as KSClassDeclaration).isAnnotationPresent(Domain::class)
              }.forEach { prop ->
                val n = prop.simpleName.getShortName()
                addStatement("%T::$n.name -> $n.toResponse()", domain.toEntityClass())
              }
              addStatement("else -> propertiesByName[it.name]?.get(this@%L)", domain.toEntityClass().simpleName)
            }
          }
          addStatement("callBy(params)")
        }
      }.build())
    }.build())
  }

  private fun KSPropertyDeclaration.toColumnType(): TypeName {
    val columnBase = ClassName("org.jetbrains.exposed.sql", "Column")
    return columnBase.parameterizedBy(type.toTypeName())
  }

  private fun PropertySpec.Builder.setColumnInitializer(fieldName: String, property: KSPropertyDeclaration) {
    val columnName = fieldName.camelToSnakeCase()
    if (property.isAnnotationPresent(ForeignKey::class)) {
      handleForeignKey(property)
    } else {
      when (property.type.resolve().declaration.simpleName.getShortName()) {
        "String" -> handleString(columnName, property)
        "Int" -> handleInt(columnName, property)
        "Long" -> handleLong(columnName, property)
        "Boolean" -> handleBoolean(columnName, property)
        "Double" -> handleDouble(columnName, property)
        "Float" -> handleFloat(columnName, property)
        "UUID" -> handleUuid(columnName, property)
        else -> TODO("${property.type} is not yet supported for Table definitions")
      }
    }
  }

  private fun PropertySpec.Builder.handleString(columnName: String, property: KSPropertyDeclaration) {
    val format = StringBuilder()
    format.append("varchar(%S, %L)")
    if (property.type.resolve().toString().contains("?")) format.append(".nullable()")
    if (property.isAnnotationPresent(Index::class)) {
      val index = property.getAnnotationsByType(Index::class).first()
      when (index.unique) {
        true -> format.append(".uniqueIndex()")
        false -> format.append(".index()")
      }
    }
    initializer(format.toString(), columnName, determineVarCharSize(property))
  }

  private fun determineVarCharSize(property: KSPropertyDeclaration): Int {
    val varCharAnnotation = property.getAnnotationsByType(VarChar::class).firstOrNull()
    return varCharAnnotation?.size ?: DEFAULT_VARCHAR_SIZE
  }

  private fun PropertySpec.Builder.handleInt(columnName: String, property: KSPropertyDeclaration) {
    val format = StringBuilder()
    format.append("integer(%S)")
    if (property.type.resolve().toString().contains("?")) format.append(".nullable()")
    initializer(format.toString(), columnName)
  }

  private fun PropertySpec.Builder.handleLong(columnName: String, property: KSPropertyDeclaration) {
    val format = StringBuilder()
    format.append("long(%S)")
    if (property.type.resolve().toString().contains("?")) format.append(".nullable()")
    initializer(format.toString(), columnName)
  }

  private fun PropertySpec.Builder.handleBoolean(columnName: String, property: KSPropertyDeclaration) {
    val format = StringBuilder()
    format.append("bool(%S)")
    if (property.type.resolve().toString().contains("?")) format.append(".nullable()")
    initializer(format.toString(), columnName)
  }

  private fun PropertySpec.Builder.handleDouble(columnName: String, property: KSPropertyDeclaration) {
    val format = StringBuilder()
    format.append("double(%S)")
    if (property.type.resolve().toString().contains("?")) format.append(".nullable()")
    initializer(format.toString(), columnName)
  }

  private fun PropertySpec.Builder.handleFloat(columnName: String, property: KSPropertyDeclaration) {
    val format = StringBuilder()
    format.append("float(%S)")
    if (property.type.resolve().toString().contains("?")) format.append(".nullable()")
    initializer(format.toString(), columnName)
  }

  private fun PropertySpec.Builder.handleUuid(columnName: String, property: KSPropertyDeclaration) {
    val format = StringBuilder()
    format.append("uuid(%S)")
    if (property.type.resolve().toString().contains("?")) format.append(".nullable()")
    initializer(format.toString(), columnName)
  }

  private fun PropertySpec.Builder.handleForeignKey(property: KSPropertyDeclaration) {
    val fk = property.getAnnotationsByType(ForeignKey::class).first()
    val domain = (property.type.resolve().declaration as KSClassDeclaration).getAnnotationsByType(Domain::class).first()
    val format = StringBuilder()
    format.append("reference(%S, %T)")
    if (property.type.resolve().toString().contains("?")) format.append(".nullable()")
    initializer(format.toString(), fk.field, domain.toTableClass())
  }
}
