package io.bkbn.lerasium.rdbms.processor.visitor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toTypeName
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.model.Entity
import io.bkbn.lerasium.rdbms.ForeignKey
import io.bkbn.lerasium.rdbms.OneToMany
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.KotlinPoetUtils.toEntityClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toResponseClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toTableClass
import io.bkbn.lerasium.utils.LerasiumUtils.findParentDomain
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SizedIterable
import java.util.UUID

@OptIn(KspExperimental::class, KotlinPoetKspPreview::class)
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

    val domain = classDeclaration.findParentDomain()

    fileBuilder.addEntity(classDeclaration, domain)
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
      val refDomain = prop.type.getDomain()
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
}
