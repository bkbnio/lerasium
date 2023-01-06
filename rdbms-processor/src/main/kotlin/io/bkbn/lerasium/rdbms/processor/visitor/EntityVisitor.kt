package io.bkbn.lerasium.rdbms.processor.visitor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toTypeName
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.converter.ConvertTo
import io.bkbn.lerasium.rdbms.ForeignKey
import io.bkbn.lerasium.rdbms.ManyToMany
import io.bkbn.lerasium.rdbms.OneToMany
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addObjectInstantiation
import io.bkbn.lerasium.utils.KotlinPoetUtils.toEntityClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toTableClass
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.getCollectionType
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SizedIterable
import java.util.UUID

@OptIn(KspExperimental::class)
class EntityVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

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
    addType(TypeSpec.classBuilder(charter.domain.name.plus("Entity")).apply {
      addEntityTypeInfo(charter)
//      addResponseConverter(charter)
      addDomainConverter(charter)
      addEntityProperties(charter)
      addRelations(charter)
    }.build())
  }

  private fun TypeSpec.Builder.addEntityTypeInfo(charter: LerasiumCharter) {
    addOriginatingKSFile(charter.classDeclaration.containingFile!!)
    addSuperinterface(ConvertTo::class.asClassName().parameterizedBy(charter.domainClass))
    superclass(UUIDEntity::class)
    addSuperclassConstructorParameter("id")
    primaryConstructor(FunSpec.constructorBuilder().apply {
      addParameter("id", EntityID::class.parameterizedBy(UUID::class))
    }.build())
    addType(TypeSpec.companionObjectBuilder().apply {
      superclass(UUIDEntityClass::class.asTypeName().parameterizedBy(charter.entityClass))
      addSuperclassConstructorParameter("%T", charter.tableClass)
    }.build())
  }

  private fun TypeSpec.Builder.addEntityProperties(charter: LerasiumCharter) {
    val properties = charter.classDeclaration.getAllProperties()
      .filterNot { it.isAnnotationPresent(OneToMany::class) }.toList()
      .filterNot { it.isAnnotationPresent(ManyToMany::class) }.toList()
      .filterNot { it.simpleName.getShortName() == "id" }
    properties.forEach { property ->
      val fieldName = property.simpleName.asString()
      val fieldType = property.type.toTypeName()
      if (property.isAnnotationPresent(ForeignKey::class)) {
        val fkDomain =
          (property.type.resolve().declaration as KSClassDeclaration).getAnnotationsByType(Domain::class).first()
        addProperty(PropertySpec.builder(fieldName, fkDomain.toEntityClass()).apply {
          delegate("%T referencedOn %T.%L", fkDomain.toEntityClass(), charter.tableClass, fieldName)
          mutable()
        }.build())
      } else {
        addProperty(PropertySpec.builder(fieldName, fieldType).apply {
          delegate("%T.$fieldName", charter.tableClass)
          mutable()
        }.build())
      }

    }
    addProperty(PropertySpec.builder("createdAt", LocalDateTime::class).apply {
      delegate("%T.createdAt", charter.tableClass)
      mutable()
    }.build())
    addProperty(PropertySpec.builder("updatedAt", LocalDateTime::class).apply {
      delegate("%T.updatedAt", charter.tableClass)
      mutable()
    }.build())
  }

  private fun TypeSpec.Builder.addRelations(charter: LerasiumCharter) {
    charter.classDeclaration.getAllProperties()
      .filter { it.isAnnotationPresent(OneToMany::class) }
      .forEach { addOneToMany(it) }
    charter.classDeclaration.getAllProperties()
      .filter { it.isAnnotationPresent(ManyToMany::class) }
      .forEach { addManyToMany(it) }
  }

  private fun TypeSpec.Builder.addOneToMany(prop: KSPropertyDeclaration) {
    val name = prop.simpleName.getShortName()
    val otm = prop.getAnnotationsByType(OneToMany::class).first()
    val refDomain = prop.getCollectionType().getDomain()
    addProperty(
      PropertySpec.builder(
        name,
        SizedIterable::class.asClassName().parameterizedBy(refDomain.toEntityClass())
      ).apply {
        delegate("%T referrersOn %T.${otm.refColumn}", refDomain.toEntityClass(), refDomain.toTableClass())
      }.build()
    )
  }

  private fun TypeSpec.Builder.addManyToMany(prop: KSPropertyDeclaration) {
    val name = prop.simpleName.getShortName()
    val mapDomain = (prop.annotations.toList().first().arguments.first().value as KSType)
      .declaration.getAnnotationsByType(Domain::class).first()
    val refDomain = prop.getCollectionType().getDomain()
    addProperty(
      PropertySpec.builder(
        name,
        SizedIterable::class.asClassName().parameterizedBy(refDomain.toEntityClass())
      ).apply {
        delegate("%T via %T", refDomain.toEntityClass(), mapDomain.toTableClass())
      }.build()
    )
  }

  private fun TypeSpec.Builder.addDomainConverter(charter: LerasiumCharter) {
    val scalarProperties = charter.classDeclaration.getAllProperties()
      .filterNot { it.isAnnotationPresent(OneToMany::class) }
      .filterNot { it.isAnnotationPresent(ManyToMany::class) }
      .filterNot { it.isAnnotationPresent(ForeignKey::class) }
      .filterNot { it.simpleName.getShortName() == "id" }
    val oneToManyProperties = charter.classDeclaration.getAllProperties()
      .filter { it.isAnnotationPresent(OneToMany::class) }
    val manyToManyProperties = charter.classDeclaration.getAllProperties()
      .filter { it.isAnnotationPresent(ManyToMany::class) }
    val foreignKeyProperties = charter.classDeclaration.getAllProperties()
      .filter { it.isAnnotationPresent(ForeignKey::class) }
    addFunction(FunSpec.builder("to").apply {
      addModifiers(KModifier.OVERRIDE)
      returns(charter.domainClass)
      addCodeBlock {
        addObjectInstantiation(charter.domainClass, returnInstance = true) {
          addStatement("id = this.id.value,")
          scalarProperties.forEach { property ->
            val name = property.simpleName.getShortName()
            addStatement("%L = this.%L,", name, name)
          }
          oneToManyProperties.forEach { property ->
            val name = property.simpleName.getShortName()
            addStatement("%L = this.%L.map { it.to() },", name, name)
          }
          manyToManyProperties.forEach { property ->
            val name = property.simpleName.getShortName()
            addStatement("%L = this.%L.map { it.to() },", name, name)
          }
          foreignKeyProperties.forEach { property ->
            val name = property.simpleName.getShortName()
            addStatement("%L = this.%L.to(),", name, name)
          }
        }
      }
    }.build())
  }
}
