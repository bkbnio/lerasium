package io.bkbn.lerasium.rdbms.processor

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
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.dao.Dao
import io.bkbn.lerasium.core.model.CountResponse
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.KotlinPoetUtils.toCreateRequestClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toResponseClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toUpdateRequestClass
import io.bkbn.lerasium.utils.LerasiumUtils.findParentDomain
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import java.util.UUID

@OptIn(KotlinPoetKspPreview::class)
class DaoVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  companion object {
    private val Transaction = MemberName("org.jetbrains.exposed.sql.transactions", "transaction")
    private val toLDT = MemberName("kotlinx.datetime", "toLocalDateTime")
  }

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Table", classDeclaration)
      return
    }

    val domain = classDeclaration.findParentDomain()

    fileBuilder.addDao(classDeclaration, domain)
  }

  private fun FileSpec.Builder.addDao(cd: KSClassDeclaration, domain: Domain) {
    val crc = domain.toCreateRequestClass()
    val urc = domain.toUpdateRequestClass()
    val rc = domain.toResponseClass()
    val ec = ClassName(this.packageName, domain.name.plus("Entity"))
    addType(TypeSpec.classBuilder(domain.name.plus("Dao")).apply {
      addOriginatingKSFile(cd.containingFile!!)
      addSuperinterface(Dao::class.asTypeName().parameterizedBy(ec, rc, crc, urc))
      addCreateFunction(cd, crc, rc, ec)
      addReadFunction(rc, ec)
      addUpdateFunction(cd, urc, rc, ec)
      addDeleteFunction(ec)
      addCountAllFunction(ec)
      addGetAllFunction(ec, rc)
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
          addStatement("val now = %T.now().%M(%T.UTC)", Clock.System::class, toLDT, TimeZone::class)
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

  private fun TypeSpec.Builder.addCountAllFunction(entityClass: ClassName) {
    addFunction(FunSpec.builder("countAll").apply {
      addModifiers(KModifier.OVERRIDE)
      returns(CountResponse::class)
      addCode(CodeBlock.builder().apply {
        addControlFlow("return %M", Transaction) {
          addStatement("val count = %T.count()", entityClass)
          addStatement("%T(count)", CountResponse::class)
        }
      }.build())
    }.build())
  }


  private fun TypeSpec.Builder.addGetAllFunction(entityClass: ClassName, responseClass: ClassName) {
    addFunction(FunSpec.builder("getAll").apply {
      addModifiers(KModifier.OVERRIDE)
      returns(List::class.asClassName().parameterizedBy(responseClass))
      addParameter(ParameterSpec.builder("chunk", Int::class).build())
      addParameter(ParameterSpec.builder("offset", Int::class).build())
      addCode(CodeBlock.builder().apply {
        addControlFlow("return %M", Transaction) {
          addStatement("val entities = %T.all().limit(chunk, offset.toLong())", entityClass)
          addControlFlow("entities.map { entity ->") {
            addStatement("entity.toResponse()")
          }
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
      addParameter("id", UUID::class)
      addParameter("request", requestClass)
      returns(responseClass)
      addCode(CodeBlock.builder().apply {
        addControlFlow("return %M", Transaction) {
          addStatement("val now = %T.now().%M(%T.UTC)", Clock.System::class, toLDT, TimeZone::class)
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
      addParameter("id", UUID::class)
      addCode(CodeBlock.builder().apply {
        addControlFlow("return %M", Transaction) {
          addStatement("val entity = %T.findById(id) ?: error(%P)", entityClass, "Unable to get entity with id: \$id")
          addStatement("entity.delete()")
        }
      }.build())
    }.build())
  }
}