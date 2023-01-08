package io.bkbn.lerasium.rdbms.processor.visitor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.core.auth.Password
import io.bkbn.lerasium.core.auth.Username
import io.bkbn.lerasium.rdbms.ForeignKey
import io.bkbn.lerasium.utils.KotlinPoetUtils.PERSISTENCE_CONFIG_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.TABLE_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.KotlinPoetUtils.addObjectInstantiation
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import io.bkbn.lerasium.utils.LerasiumUtils.isDomain
import io.bkbn.lerasium.utils.StringUtils.decapitalized
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.r2dbc.R2dbcDatabase
import java.util.UUID

@OptIn(KspExperimental::class)
class RepositoryVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  companion object {
    private val Single = MemberName("org.komapper.core.dsl.query", "single")
    private val AndThen = MemberName("org.komapper.core.dsl.query", "andThen")
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
      addProperty(PropertySpec.builder("db", R2dbcDatabase::class).apply {
        addModifiers(KModifier.PRIVATE)
        initializer("%T.database", ClassName(PERSISTENCE_CONFIG_PACKAGE_NAME, "PostgresConfig"))
      }.build())
      addProperty(
        PropertySpec.builder("resource", ClassName(TABLE_PACKAGE_NAME, "_${charter.domain.name}Table")).apply {
          addModifiers(KModifier.PRIVATE)
          initializer("%T.%M", Meta::class, MemberName(TABLE_PACKAGE_NAME, charter.domain.name.decapitalized()))
        }.build()
      )
      addOriginatingKSFile(charter.classDeclaration.containingFile!!)
      addCreateFunction(charter)
      addReadFunction(charter)
      addUpdateFunction(charter)
      addDeleteFunction()
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
      addModifiers(KModifier.SUSPEND)
      returns(charter.domainClass)
      addParameter("request", charter.apiCreateRequestClass)
      addCodeBlock {
        addControlFlow("return db.withTransaction") {
          addControlFlow("val result = db.runQuery") {
            addStatement("%T.insert(resource).single(", QueryDsl::class)
            indent()
            addObjectInstantiation(charter.tableClass) {
              scalarProperties.forEach { prop ->
                val name = prop.simpleName.getShortName()
                addStatement("%L = request.%L,", name, name)
              }
              foreignKeys.forEach { prop ->
                val name = prop.simpleName.getShortName()
                addStatement("%L = request.%L,", name, name)
              }
            }
            unindent()
            addStatement(")")
          }
          addStatement("result.to()")
        }
      }
    }.build())
  }

  private fun TypeSpec.Builder.addReadFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("read").apply {
      addModifiers(KModifier.SUSPEND)
      returns(charter.domainClass)
      addParameter("id", UUID::class)
      addCodeBlock {
        addControlFlow("return db.withTransaction") {
          addControlFlow("val result = db.runQuery") {
            addControlFlow("val query = %T.from(resource).where", QueryDsl::class) {
              addStatement("resource.id eq id")
            }
            addStatement("query.%M()", Single)
          }
          addStatement("result.to()")
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
      addModifiers(KModifier.SUSPEND)
      returns(charter.domainClass)
      addParameter("id", UUID::class)
      addParameter("request", charter.apiUpdateRequestClass)
      addCodeBlock {
        addControlFlow("return db.withTransaction") {
          addControlFlow("val result = db.runQuery") {
            addStatement("%T.update(resource)", QueryDsl::class)
            indent()
            addControlFlow(".set") {
              scalarProperties.forEach { prop ->
                val name = prop.simpleName.getShortName()
                addStatement("request.%L?.let { v -> it.%L to v }", name, name)
              }
              foreignKeys.forEach { prop ->
                val name = prop.simpleName.getShortName()
                addStatement("request.%L?.let { v -> it.%L to v }", name, name)
              }
            }
            addControlFlow(".where") {
              addStatement("resource.id eq id")
            }
            addStatement(".%M(%T.from(resource).where { resource.id eq id }.single())", AndThen, QueryDsl::class)
            unindent()
          }
          addStatement("result.to()")
        }
      }
    }.build())
  }

  private fun TypeSpec.Builder.addDeleteFunction() {
    addFunction(FunSpec.builder("delete").apply {
      addModifiers(KModifier.SUSPEND)
      addParameter("id", UUID::class)
      addCodeBlock {
        addControlFlow("return db.withTransaction") {
          addControlFlow("db.runQuery") {
            addStatement("%T.delete(resource).where { resource.id eq id }", QueryDsl::class)
          }
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
      addModifiers(KModifier.SUSPEND)
      addParameter("username", String::class)
      addParameter("password", String::class)
      returns(charter.domainClass)
      addCodeBlock {
        addControlFlow("return db.withTransaction") {
          addControlFlow("val result = db.runQuery") {
            addControlFlow("val query = %T.from(resource).where", QueryDsl::class) {
              addStatement("resource.%L eq username", usernameProp.simpleName.getShortName())
              addStatement("resource.%L eq password", passwordProp.simpleName.getShortName())
            }
            addStatement("query.%M()", Single)
          }
          addStatement("result.to()")
        }
      }
    }.build())
  }
}
