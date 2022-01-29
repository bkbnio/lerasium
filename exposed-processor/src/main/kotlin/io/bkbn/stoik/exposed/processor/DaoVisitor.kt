package io.bkbn.stoik.exposed.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import io.bkbn.stoik.core.Domain
import io.bkbn.stoik.core.dao.Dao
import io.bkbn.stoik.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.stoik.utils.KotlinPoetUtils.toCreateRequestClass
import io.bkbn.stoik.utils.KotlinPoetUtils.toEntityClass
import io.bkbn.stoik.utils.KotlinPoetUtils.toResponseClass
import io.bkbn.stoik.utils.KotlinPoetUtils.toUpdateRequestClass
import io.bkbn.stoik.utils.StoikUtils.findParentDomain
import kotlinx.datetime.Clock
import java.util.UUID

class DaoVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  companion object {
    private val Transaction = MemberName("org.jetbrains.exposed.sql.transactions", "transaction")
  }

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Table", classDeclaration)
      return
    }

    val domain = classDeclaration.findParentDomain()

    fileBuilder.addDao(classDeclaration, domain)
  }

  // """Unable to get entity with id $id"""
  private fun FileSpec.Builder.addDao(cd: KSClassDeclaration, domain: Domain) {
    val crc = domain.toCreateRequestClass()
    val urc = domain.toUpdateRequestClass()
    val rc = domain.toResponseClass()
    val ec = domain.toEntityClass()
    addType(TypeSpec.classBuilder(domain.name.plus("Dao")).apply {
      addSuperinterface(Dao::class.asTypeName().parameterizedBy(crc, urc, rc, ec))
      addCreateFunction(cd, crc, rc, ec)
      addReadFunction(rc, ec)
      addUpdateFunction(cd, urc, rc, ec)
      addDeleteFunction(ec)
    }.build())
  }

  private fun TypeSpec.Builder.addCreateFunction(
    cd: KSClassDeclaration,
    requestClass: ClassName,
    responseClass: ClassName,
    entityClass: ClassName
  ) {
    val props = cd.getAllProperties().toList()
    addFunction(FunSpec.builder("create").apply {
      addModifiers(KModifier.OVERRIDE)
      addParameter("request", requestClass)
      returns(responseClass)
      addCode(CodeBlock.builder().apply {
        addControlFlow("return %M", Transaction) {
          addStatement("val now = %T.now()", Clock::class)
          addControlFlow("val entity = %M", Transaction) {
            addControlFlow("%T.new", entityClass) {
              props.forEach { property ->
                val propName = property.simpleName.getShortName()
                addStatement("$propName = request.$propName")
              }
              addStatement("createdAt = now")
              addStatement("updatedAt = now")
            }
          }
          addStatement("entity.toResponse()")
        }
      }.build())
    }.build())
  }

  private fun TypeSpec.Builder.addReadFunction(responseClass: ClassName, entityClass: ClassName) {
    addFunction(FunSpec.builder("read").apply {
      addModifiers(KModifier.OVERRIDE)
      returns(responseClass)
      addParameter("id", UUID::class)
      addCode(CodeBlock.builder().apply {
        addControlFlow("return %M", Transaction) {
          addStatement("val entity = %T.findById(id) ?: error(%P)", entityClass, "Unable to get entity with id: \$id")
          addStatement("entity.toResponse()")
        }
      }.build())
    }.build())
  }

  private fun TypeSpec.Builder.addUpdateFunction(
    cd: KSClassDeclaration,
    requestClass: ClassName,
    responseClass: ClassName,
    entityClass: ClassName
  ) {
    val props = cd.getAllProperties().toList()
    addFunction(FunSpec.builder("update").apply {
      addModifiers(KModifier.OVERRIDE)
      addParameter("request", requestClass)
      addParameter("id", UUID::class)
      returns(responseClass)
      addCode(CodeBlock.builder().apply {
        addControlFlow("return %M", Transaction) {
          addStatement("val now = %T.now()", Clock::class)
          addStatement("val entity = %T.findById(id) ?: error(%P)", entityClass, "Unable to get entity with id: \$id")
          props.forEach { property ->
            val propName = property.simpleName.getShortName()
            addControlFlow("request.%L?.let", propName) {
              addStatement("entity.%L = it", propName)
            }
          }
          addStatement("entity.updatedAt = now")
          addStatement("entity.toResponse()")
        }
      }.build())
    }.build())
  }

  private fun TypeSpec.Builder.addDeleteFunction(entityClass: ClassName) {
    addFunction(FunSpec.builder("delete").apply {
      addModifiers(KModifier.OVERRIDE)
      addCode(CodeBlock.builder().apply {
        addStatement("val entity = %T.findById(id) ?: error(%P)", entityClass, "Unable to get entity with id: \$id")
        addStatement("entity.delete()")
      }.build())
    }.build())
  }
}
