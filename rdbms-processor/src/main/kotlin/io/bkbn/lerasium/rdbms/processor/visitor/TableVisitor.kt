package io.bkbn.lerasium.rdbms.processor.visitor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.AnnotationSpec
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
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.core.converter.ConvertTo
import io.bkbn.lerasium.core.model.DomainProvider
import io.bkbn.lerasium.rdbms.ForeignKey
import io.bkbn.lerasium.rdbms.Table
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addObjectInstantiation
import io.bkbn.lerasium.utils.KotlinPoetUtils.collectProperties
import io.bkbn.lerasium.utils.KotlinPoetUtils.toParameter
import io.bkbn.lerasium.utils.KotlinPoetUtils.toProperty
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import io.bkbn.lerasium.utils.StringUtils.decapitalized
import io.bkbn.lerasium.utils.StringUtils.pascalToSnakeCase
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.komapper.annotation.EnumType
import org.komapper.annotation.KomapperCreatedAt
import org.komapper.annotation.KomapperEntity
import org.komapper.annotation.KomapperEnum
import org.komapper.annotation.KomapperId
import org.komapper.annotation.KomapperTable
import org.komapper.annotation.KomapperUpdatedAt
import org.komapper.annotation.KomapperVersion
import java.util.UUID

@OptIn(KspExperimental::class)
class TableVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  private companion object {
    val memberProps = MemberName("kotlin.reflect.full", "memberProperties")
    val valueParams = MemberName("kotlin.reflect.full", "valueParameters")
  }

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Table", classDeclaration)
      return
    }

    val domain = classDeclaration.getDomain()
    val charter = LerasiumCharter(classDeclaration = classDeclaration, domain = domain)

    fileBuilder.addEntity(charter)
  }

  private fun FileSpec.Builder.addEntity(charter: LerasiumCharter) {
    val table = charter.classDeclaration.getAnnotationsByType(Table::class).first()
    addType(TypeSpec.classBuilder(charter.domain.name.plus("Table")).apply {
      addOriginatingKSFile(charter.classDeclaration.containingFile!!)
      addSuperinterface(ConvertTo::class.asClassName().parameterizedBy(charter.domainClass))
      addModifiers(KModifier.DATA)
      addAnnotation(AnnotationSpec.builder(KomapperEntity::class).apply {
        addMember("aliases = [%S]", charter.domain.name.decapitalized())
      }.build())
      addAnnotation(AnnotationSpec.builder(KomapperTable::class).apply {
        val tableName = table.name.ifBlank { charter.domain.name.pascalToSnakeCase() }
        addMember("name = %S", tableName)
      }.build())
      addPrimaryConstructor(charter)
      addProperties(charter)
      addDomainConverter(charter)
    }.build())
  }

  private fun TypeSpec.Builder.addPrimaryConstructor(charter: LerasiumCharter) {
    val properties = charter.classDeclaration.collectProperties()
    val foreignKeyProps = charter.classDeclaration.getAllProperties()
      .filter { it.isAnnotationPresent(ForeignKey::class) }
    primaryConstructor(FunSpec.constructorBuilder().apply {
      properties.scalars.filterNot { it.simpleName.getShortName() == "id" }
        .forEach { prop -> addParameter(prop.toParameter()) }
      foreignKeyProps.forEach { prop -> addParameter(prop.simpleName.getShortName(), UUID::class) }
      properties.enums.forEach { prop ->
        addParameter(ParameterSpec.builder(prop.simpleName.getShortName(), prop.type.toTypeName()).apply {
          addAnnotation(AnnotationSpec.builder(KomapperEnum::class).apply {
            addMember("type = %T.NAME", EnumType::class)
          }.build())
        }.build())
      }
      addParameter(ParameterSpec.builder("id", UUID::class).apply {
        addAnnotation(KomapperId::class)
        defaultValue("%T.randomUUID()", UUID::class)
      }.build())
      addParameter(ParameterSpec.builder("version", Int::class).apply {
        addAnnotation(KomapperVersion::class)
        defaultValue("0")
      }.build())
      addParameter(ParameterSpec.builder("createdAt", Instant::class.asTypeName().copy(nullable = true)).apply {
        addAnnotation(KomapperCreatedAt::class)
        defaultValue("null")
      }.build())
      addParameter(ParameterSpec.builder("updatedAt", Instant::class.asTypeName().copy(nullable = true)).apply {
        addAnnotation(KomapperUpdatedAt::class)
        defaultValue("null")
      }.build())
    }.build())
  }

  private fun TypeSpec.Builder.addProperties(charter: LerasiumCharter) {
    val properties = charter.classDeclaration.collectProperties()
    val foreignKeyProps = charter.classDeclaration.getAllProperties()
      .filter { it.isAnnotationPresent(ForeignKey::class) }
    properties.scalars.forEach { prop -> addProperty(prop.toProperty()) }
    foreignKeyProps.forEach { prop ->
      addProperty(PropertySpec.builder(prop.simpleName.getShortName(), UUID::class).apply {
        initializer(prop.simpleName.getShortName())
      }.build())
    }
    properties.enums.forEach { prop ->
      addProperty(PropertySpec.builder(prop.simpleName.getShortName(), prop.type.toTypeName()).apply {
        initializer(prop.simpleName.getShortName())
      }.build())
    }
    addProperty(PropertySpec.builder("id", UUID::class).apply {
      initializer("id")
    }.build())
    addProperty(PropertySpec.builder("version", Int::class).apply {
      initializer("version")
    }.build())
    addProperty(PropertySpec.builder("createdAt", Instant::class.asTypeName().copy(nullable = true)).apply {
      initializer("createdAt")
    }.build())
    addProperty(PropertySpec.builder("updatedAt", Instant::class.asTypeName().copy(nullable = true)).apply {
      initializer("updatedAt")
    }.build())
  }

  private fun TypeSpec.Builder.addDomainConverter(charter: LerasiumCharter) {
    val properties = charter.classDeclaration.collectProperties()
    val relationProps = charter.classDeclaration.getAllProperties()
      .filter { it.isAnnotationPresent(Relation::class) }
    addFunction(FunSpec.builder("to").apply {
      addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
      returns(charter.domainClass)
      addCodeBlock {
        addObjectInstantiation(charter.domainClass, returnInstance = true) {
          properties.scalars.forEach { prop ->
            addStatement("%L = %L,", prop.simpleName.getShortName(), prop.simpleName.getShortName())
          }
          relationProps.forEach { prop ->
            val name = prop.simpleName.getShortName()
            addStatement("%L = %T.from(%L),", name, DomainProvider::class, name)
          }
          properties.enums.forEach { prop ->
            addStatement("%L = %L,", prop.simpleName.getShortName(), prop.simpleName.getShortName())
          }
        }
      }
    }.build())
  }
}
