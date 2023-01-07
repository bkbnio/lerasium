package io.bkbn.lerasium.rdbms.processor.visitor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.core.auth.Password
import io.bkbn.lerasium.core.auth.Username
import io.bkbn.lerasium.rdbms.ForeignKey
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.KotlinPoetUtils.toEntityClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toParameter
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import io.bkbn.lerasium.utils.LerasiumUtils.isDomain
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import java.util.UUID

@OptIn(KspExperimental::class)
class RepositoryVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  companion object {
    private val Transaction = MemberName("org.jetbrains.exposed.sql.transactions", "transaction")
    private val toLDT = MemberName("kotlinx.datetime", "toLocalDateTime")
  }

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Table", classDeclaration)
      return
    }

    val domain = classDeclaration.getDomain()
    val charter = LerasiumCharter(domain, classDeclaration)

    fileBuilder.addRepository(charter)
  }

  private fun FileSpec.Builder.addRepository(charter: LerasiumCharter) {
    addType(TypeSpec.objectBuilder(charter.domain.name.plus("Repository")).apply {
      addOriginatingKSFile(charter.classDeclaration.containingFile!!)
      addCreateFunction(charter)
      addReadFunction(charter)
      addUpdateFunction(charter)
      addDeleteFunction(charter)
      if (charter.isActor) addAuthenticationFunction(charter)
    }.build())
  }

  private fun TypeSpec.Builder.addCreateFunction(charter: LerasiumCharter) {
    val scalarProperties = charter.classDeclaration.getAllProperties()
      .filterNot { it.simpleName.getShortName() == "id" }
      .filterNot { it.type.isDomain() }
      .filterNot { it.isAnnotationPresent(Relation::class) }
    val foreignKeys = charter.classDeclaration.getAllProperties()
      .filter { it.isAnnotationPresent(ForeignKey::class) }
    addFunction(FunSpec.builder("create").apply {
      returns(charter.domainClass)
      addParameter("request", charter.apiCreateRequestClass)
      addCodeBlock {
        addControlFlow("return %M", Transaction) {
          addStatement("val now = %T.now().%M(%T.UTC)", Clock.System::class, toLDT, TimeZone::class)
          addControlFlow("val entity = %T.new", charter.entityClass) {
            scalarProperties.forEach {
              val n = it.simpleName.getShortName()
              addStatement("this.%L = request.%L", n, n)
            }
            foreignKeys.forEach {
              val n = it.simpleName.getShortName()
              addStatement(
                "this.%L = %T.findById(request.%L) ?: error(%P)",
                n,
                it.type.getDomain().toEntityClass(),
                n,
                "Invalid foreign key"
              )
            }
            addStatement("this.createdAt = now")
            addStatement("this.updatedAt = now")
          }
          addStatement("entity.to()")
        }
      }
    }.build())
  }

  private fun TypeSpec.Builder.addReadFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("read").apply {
      returns(charter.domainClass)
      addParameter("id", UUID::class)
      addCodeBlock {
        addControlFlow("return %M", Transaction) {
          addStatement(
            "val entity = %T.findById(id) ?: error(%P)",
            charter.entityClass,
            "Unable to get entity with id: \$id"
          )
          addStatement("entity.to()")
        }
      }
    }.build())
  }

  private fun TypeSpec.Builder.addUpdateFunction(charter: LerasiumCharter) {
    val scalarProperties = charter.classDeclaration.getAllProperties()
      .filterNot { it.simpleName.getShortName() == "id" }
      .filterNot { it.type.isDomain() }
      .filterNot { it.isAnnotationPresent(Relation::class) }
    val foreignKeys = charter.classDeclaration.getAllProperties()
      .filter { it.isAnnotationPresent(ForeignKey::class) }
    addFunction(FunSpec.builder("update").apply {
      returns(charter.domainClass)
      addParameter("id", UUID::class)
      addParameter("request", charter.apiUpdateRequestClass)
      addCodeBlock {
        addControlFlow("return %M", Transaction) {
          addStatement("val now = %T.now().%M(%T.UTC)", Clock.System::class, toLDT, TimeZone::class)
          addStatement(
            "val entity = %T.findById(id) ?: error(%P)",
            charter.entityClass,
            "Unable to get entity with id: \$id"
          )
          scalarProperties.forEach {
            val n = it.simpleName.getShortName()
            addStatement("request.%L?.let { entity.%L = it }", n, n)
          }
          foreignKeys.forEach {
            val n = it.simpleName.getShortName()
            addStatement(
              "request.%L?.let { entity.%L = %T.findById(it) ?: error(%P) }",
              n,
              n,
              it.type.getDomain().toEntityClass(),
              "Unable to get entity with id: \$it"
            )
          }
          addStatement("entity.updatedAt = now")
          addStatement("entity.to()")
        }
      }
    }.build())
  }

  private fun TypeSpec.Builder.addDeleteFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("delete").apply {
      addParameter("id", UUID::class)
      addCodeBlock {
        addControlFlow("return %M", Transaction) {
          addStatement(
            "val entity = %T.findById(id) ?: error(%P)",
            charter.entityClass,
            "Unable to get entity with id: \$id"
          )
          addStatement("entity.delete()")
        }
      }
    }.build())
  }

  private fun TypeSpec.Builder.addAuthenticationFunction(charter: LerasiumCharter) {
    val usernameProp = charter.classDeclaration.getAllProperties().find { it.isAnnotationPresent(Username::class) }
      ?: error("No username property found for ${charter.classDeclaration.qualifiedName}")
    val passwordProp = charter.classDeclaration.getAllProperties().find { it.isAnnotationPresent(Password::class) }
      ?: error("No password property found for ${charter.classDeclaration.qualifiedName}")
    addFunction(FunSpec.builder("authenticate").apply {
      addParameter("username", String::class)
      addParameter("password", String::class)
      returns(charter.domainClass)
      addCodeBlock {
        addControlFlow("return %M", Transaction) {
          addStatement(
            "val entity = %T.find { %T.%L eq username }.firstOrNull() ?: error(%P)",
            charter.entityClass,
            charter.tableClass,
            usernameProp.simpleName.getShortName(),
            "No ${charter.domain.name} found with username: \$username"
          )
          addStatement(
            "if (entity.%L != password) error(%P)",
            passwordProp.simpleName.getShortName(),
            "Incorrect password"
          )
          addStatement("entity.to()")
        }
      }
    }.build())
  }
}
