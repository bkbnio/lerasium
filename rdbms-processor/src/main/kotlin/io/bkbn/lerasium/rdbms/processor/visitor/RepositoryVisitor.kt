package io.bkbn.lerasium.rdbms.processor.visitor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.core.auth.CrudAction
import io.bkbn.lerasium.core.auth.Password
import io.bkbn.lerasium.core.auth.RbacPolicyProvider
import io.bkbn.lerasium.core.auth.Username
import io.bkbn.lerasium.core.exception.UnauthorizedException
import io.bkbn.lerasium.core.request.ActorRequestContext
import io.bkbn.lerasium.core.request.AnonymousRequestContext
import io.bkbn.lerasium.core.request.RequestContext
import io.bkbn.lerasium.rdbms.ForeignKey
import io.bkbn.lerasium.rdbms.Table
import io.bkbn.lerasium.utils.KotlinPoetUtils.PERSISTENCE_CONFIG_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.POLICY_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.TABLE_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.KotlinPoetUtils.addObjectInstantiation
import io.bkbn.lerasium.utils.KotlinPoetUtils.collectProperties
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.findCompanionObject
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
      addPolicyEnforcement(charter)
      if (charter.isActor) addAuthenticationFunction(charter)
      charter.classDeclaration.findCompanionObject()?.let { addPermissionQueries(charter) }
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
      addParameter("context", RequestContext::class)
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
          addStatement("policyEnforcement(context, result.id, %T.READ)", CrudAction::class)
          addStatement("result.to()")
        }
      }
    }.build())
  }

  private fun TypeSpec.Builder.addReadFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("read").apply {
      addModifiers(KModifier.SUSPEND)
      returns(charter.domainClass)
      addParameter("context", RequestContext::class)
      addParameter("id", UUID::class)
      addCodeBlock {
        addControlFlow("return db.withTransaction") {
          addStatement("policyEnforcement(context, id, %T.READ)", CrudAction::class)
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
      addParameter("context", RequestContext::class)
      addParameter("id", UUID::class)
      addParameter("request", charter.apiUpdateRequestClass)
      addCodeBlock {
        addControlFlow("return db.withTransaction") {
          addStatement("policyEnforcement(context, id, %T.UPDATE)", CrudAction::class)
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
      addParameter("context", RequestContext::class)
      addParameter("id", UUID::class)
      addCodeBlock {
        addControlFlow("db.withTransaction") {
          addStatement("policyEnforcement(context, id, %T.DELETE)", CrudAction::class)
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

  private fun TypeSpec.Builder.addPolicyEnforcement(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("policyEnforcement").apply {
      val rbacPolicies = charter.classDeclaration.findCompanionObject()
        ?.getAllProperties()
        ?.filter { it.type.resolve().toClassName() == RbacPolicyProvider::class.asClassName() }
        ?.toList()
        ?: emptyList()
      if (rbacPolicies.isEmpty()) {
        addComment("No policies were identified for resource, block will be empty")
      }
      addModifiers(KModifier.SUSPEND, KModifier.PRIVATE)
      addParameter("context", RequestContext::class)
      addParameter("resource", UUID::class)
      addParameter("action", CrudAction::class)
      addCodeBlock {
        addControlFlow("when (context)") {
          addStatement("is %T -> {}", AnonymousRequestContext::class)
          addControlFlow("is %T<*> -> when (context.actor)", ActorRequestContext::class) {
            rbacPolicies.forEach { prop ->
              val (actor) = prop.type.resolve().arguments
              val actorEnum = ClassName(POLICY_PACKAGE_NAME, "Actor")
              val name = actor.type!!.resolve().toClassName().simpleName.uppercase()
              val enforcement = "${prop.simpleName.getShortName()}Enforcement"
              addStatement("%T.%L -> %L(%L, %L, %L)", actorEnum, name, enforcement, "context", "resource", "action")
            }
          }
        }
      }
    }.build())
  }

  private fun TypeSpec.Builder.addPermissionQueries(charter: LerasiumCharter) {
    val companionObject = charter.classDeclaration.findCompanionObject()
      ?: error("Need companion object to construct permission queries")
    val rbacPolicies = companionObject.getAllProperties()
      .filter { it.type.resolve().toClassName() == RbacPolicyProvider::class.asClassName() }
    rbacPolicies.forEach { addRbacPolicyEnforcement(charter, it) }
  }

  private fun TypeSpec.Builder.addRbacPolicyEnforcement(
    charter: LerasiumCharter,
    rbacDeclaration: KSPropertyDeclaration
  ) {
    val (actor, action, roleResource, role, resource) = rbacDeclaration.type.resolve().arguments

    require(actor.type!!.isDomain() && roleResource.type!!.isDomain() && resource.type!!.isDomain()) {
      "Actor, role resource, and resource must be each be a domain"
    }

    // todo check if resource matches charter?

    addFunction(FunSpec.builder("${rbacDeclaration.simpleName.getShortName()}Enforcement").apply {
      addModifiers(KModifier.SUSPEND, KModifier.PRIVATE)
      addParameter("context", ActorRequestContext::class.asTypeName().parameterizedBy(STAR))
      addParameter("resourceId", UUID::class)
      addParameter("action", CrudAction::class)

      if (actor.isTable() && roleResource.isTable() && resource.isTable()) {
        addRbacPolicyJoinQuery(charter, rbacDeclaration)
      } else {
        TODO()
      }

    }.build())
  }

  private fun FunSpec.Builder.addRbacPolicyJoinQuery(charter: LerasiumCharter, rbacDeclaration: KSPropertyDeclaration) {
    val (actor, _, roleResource, role, resource) = rbacDeclaration.type.resolve().arguments
    val actorDomain = actor.type!!.getDomain()
    val roleDomain = roleResource.type!!.getDomain()
    val roleResourceActorProp = roleResource.findMatchingDomainProperty(actor)
    var roleResourceResourceProp = roleResource.findMatchingDomainPropertyOrNull(resource)

    val isForeignKey = if (roleResourceResourceProp == null) {
      roleResourceResourceProp = roleResource.findCommonForeignKey(resource)
      true
    } else {
      false
    }

    val roleResourceRoleProp = roleResource.findMatchingProperty(role)

    addCodeBlock {
      addStatement(
        "val actorMeta = %T.%M",
        Meta::class,
        MemberName(TABLE_PACKAGE_NAME, actorDomain.name.decapitalized())
      )
      addStatement("val roleMeta = %T.%M", Meta::class, MemberName(TABLE_PACKAGE_NAME, roleDomain.name.decapitalized()))
      addControlFlow("val authorization = db.runQuery") {
        addStatement("%T.from(actorMeta)", QueryDsl::class)
        addControlFlow(".where") {
          addStatement("actorMeta.id eq context.actorId")
        }
        addControlFlow(".innerJoin(roleMeta)") {
          addStatement("roleMeta.%L eq actorMeta.id", roleResourceActorProp.simpleName.getShortName())
        }
        addControlFlow(".innerJoin(resource)") {
          val name = roleResourceResourceProp.simpleName.getShortName()
          if (isForeignKey) {
            addStatement("resource.%L eq roleMeta.%L", name, name)
          } else {
            addStatement("resource.id eq roleMeta.%L", name)
          }
        }
        addControlFlow(".where") {
          addStatement("resource.id eq resourceId")
        }
        addStatement(".include(resource, actorMeta, roleMeta)")
      }

      addStatement("val actor = authorization[actorMeta].single().to()")
      addStatement("val role = authorization[roleMeta].single().to()")
      addStatement("val entity = authorization[resource].single().to()")
      addControlFlow(
        "if(!%T.%L.policy.enforce(actor, action, role.%L, entity))",
        charter.classDeclaration.toClassName(),
        rbacDeclaration.simpleName.getShortName(),
        roleResourceRoleProp.simpleName.getShortName()
      ) {
        addStatement("throw %T(%S)", UnauthorizedException::class, "Actor is not authorized to perform this action")
      }
    }
  }

  private fun KSTypeArgument.isTable(): Boolean =
    ((this.type as KSTypeReference).resolve().declaration as KSClassDeclaration).isAnnotationPresent(Table::class)

  private fun KSTypeArgument.findMatchingProperty(typeArg: KSTypeArgument) = findMatchingPropertyOrNull(typeArg)
    ?: error("Could not find matching property for type argument ${typeArg.type}")

  private fun KSTypeArgument.findMatchingDomainProperty(typeArg: KSTypeArgument) =
    findMatchingDomainPropertyOrNull(typeArg)
      ?: error("Could not find matching property for type argument ${typeArg.type}")

  private fun KSTypeArgument.findMatchingPropertyOrNull(typeArg: KSTypeArgument) = this.toClassDeclaration()
    .getAllProperties()
    .filter {
      it.type.resolve().declaration as KSClassDeclaration ==
        ((typeArg.type as KSTypeReference).resolve().declaration as KSClassDeclaration)
    }.firstOrNull()

  private fun KSTypeArgument.findMatchingDomainPropertyOrNull(typeArg: KSTypeArgument) = this.toClassDeclaration()
    .getAllProperties()
    .filter { it.isAnnotationPresent(Relation::class) }
    .filter {
      it.type.resolve().arguments.map { ta -> ta.type }[1]!!.resolve().declaration ==
        ((typeArg.type as KSTypeReference).resolve().declaration as KSClassDeclaration)
    }.firstOrNull()

  private fun KSTypeArgument.findCommonForeignKey(typeArg: KSTypeArgument) = this.toClassDeclaration()
    .getAllProperties()
    .filter {
      it.toClassDeclaration() in typeArg.toClassDeclaration().getAllProperties().map { p -> p.toClassDeclaration() }
    }.firstOrNull()
    ?: error("Could not find matching property for type argument ${typeArg.type}")

  private fun KSTypeArgument.toClassDeclaration() =
    (this.type as KSTypeReference).resolve().declaration as KSClassDeclaration

  private fun KSPropertyDeclaration.toClassDeclaration() = this.type.resolve().declaration as KSClassDeclaration
}
