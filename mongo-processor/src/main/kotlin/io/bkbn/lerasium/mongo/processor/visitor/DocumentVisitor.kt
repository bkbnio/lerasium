package io.bkbn.lerasium.mongo.processor.visitor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
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
import com.squareup.kotlinpoet.ksp.toClassName
import io.bkbn.lerasium.core.model.Entity
import io.bkbn.lerasium.utils.KotlinPoetUtils.ENTITY_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.KotlinPoetUtils.isSupportedScalar
import io.bkbn.lerasium.utils.KotlinPoetUtils.toEntityClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toParameter
import io.bkbn.lerasium.utils.KotlinPoetUtils.toProperty
import io.bkbn.lerasium.utils.KotlinPoetUtils.toResponseClass
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

class DocumentVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  companion object {
    val memberProps = MemberName("kotlin.reflect.full", "memberProperties")
    val valueParams = MemberName("kotlin.reflect.full", "valueParameters")
  }

  private lateinit var containingFile: KSFile

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Document", classDeclaration)
      return
    }

    containingFile = classDeclaration.containingFile!!

    val domain = classDeclaration.getDomain()
    fileBuilder.addDocument(classDeclaration, domain.name, true)

    classDeclaration.getAllProperties().toList()
      .filterNot { it.type.isSupportedScalar() }
      .forEach { visitTypeReference(it.type, Unit) }
  }

  override fun visitTypeReference(typeReference: KSTypeReference, data: Unit) {
    val simpleName = typeReference.resolve().toClassName().simpleName
    val classDeclaration = typeReference.resolve().declaration as KSClassDeclaration

    fileBuilder.addDocument(classDeclaration, simpleName)

    classDeclaration.getAllProperties().toList()
      .filterNot { it.type.isSupportedScalar() }
      .forEach { visitTypeReference(it.type, Unit) }
  }

  private fun FileSpec.Builder.addDocument(cd: KSClassDeclaration, name: String, isDomainModel: Boolean = false) {
    val properties = cd.getAllProperties().toList()
    addType(TypeSpec.classBuilder(name.plus("Entity")).apply {
      addOriginatingKSFile(cd.containingFile!!)
      addSuperinterface(Entity::class.asClassName().parameterizedBy(name.toResponseClass()))
      addAnnotation(Serializable::class)
      addModifiers(KModifier.DATA)
      primaryConstructor(FunSpec.constructorBuilder().apply {
        if (isDomainModel) addParameter(ParameterSpec("id", UUID::class.asTypeName()))
        properties.forEach {
          val param = when (it.type.isSupportedScalar()) {
            true -> it.toParameter()
            false -> {
              val n = it.simpleName.getShortName()
              val t = it.type.resolve().toClassName().simpleName.plus("Entity")
              val cn = ClassName(ENTITY_PACKAGE_NAME, t)
              ParameterSpec.builder(n, cn).build()
            }
          }
          addParameter(param)
        }
        if (isDomainModel) addParameter(ParameterSpec("createdAt", LocalDateTime::class.asTypeName()))
        if (isDomainModel) addParameter(ParameterSpec("updatedAt", LocalDateTime::class.asTypeName()))
      }.build())
      properties.forEach {
        val prop = when (it.type.isSupportedScalar()) {
          true -> it.toProperty(true)
          false -> {
            val n = it.simpleName.getShortName()
            val t = it.type.resolve().toClassName().simpleName.plus("Entity")
            val cn = ClassName(ENTITY_PACKAGE_NAME, t)
            PropertySpec.builder(n, cn).apply {
              mutable(true)
              initializer(n)
            }.build()
          }
        }
        addProperty(prop)
      }
      if (isDomainModel) addDomainModelProps()
      addResponseConverter(name, properties)
    }.build())
  }

  private fun TypeSpec.Builder.addDomainModelProps() {
    addProperty(PropertySpec.builder("id", UUID::class.asTypeName()).apply {
      addAnnotation(Contextual::class)
      addAnnotation(AnnotationSpec.builder(SerialName::class).apply {
        addMember("%S", "_id")
      }.build())
      initializer("id")
    }.build())
    addProperty(PropertySpec.builder("createdAt", LocalDateTime::class.asTypeName()).apply {
      mutable()
      initializer("createdAt")
    }.build())
    addProperty(PropertySpec.builder("updatedAt", LocalDateTime::class.asTypeName()).apply {
      mutable()
      initializer("updatedAt")
    }.build())
  }

  private fun TypeSpec.Builder.addResponseConverter(name: String, properties: List<KSPropertyDeclaration>) =
    addFunction(FunSpec.builder("toResponse").apply {
      addModifiers(KModifier.OVERRIDE)
      returns(name.toResponseClass())
      addCode(CodeBlock.builder().apply {
        addControlFlow("return with(::%T)", name.toResponseClass()) {
          addStatement(
            "val propertiesByName = %T::class.%M.associateBy { it.name }",
            name.toEntityClass(),
            memberProps
          )
          addControlFlow("val params = %M.associateWith", valueParams) {
            addControlFlow("when (it.name)") {
              properties.filterNot { it.type.isSupportedScalar() }.forEach {
                val propName = it.simpleName.getShortName()
                addStatement("%S -> $propName.toResponse()", propName)
              }
              addStatement("else -> propertiesByName[it.name]?.get(this@%L)", name.toEntityClass().simpleName)
            }
          }
          addStatement("callBy(params)")
        }
      }.build())
    }.build())
}
