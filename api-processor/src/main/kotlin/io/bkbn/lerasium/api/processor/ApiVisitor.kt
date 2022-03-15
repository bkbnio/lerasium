@file:OptIn(KspExperimental::class)

package io.bkbn.lerasium.api.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import io.bkbn.lerasium.api.GetBy
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.Relation
import io.bkbn.lerasium.utils.KotlinPoetUtils.addCodeBlock
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.KotlinPoetUtils.toCreateRequestClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toDaoClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toTocClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toUpdateRequestClass
import io.bkbn.lerasium.utils.LerasiumUtils.findParentDomain
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import io.bkbn.lerasium.utils.StringUtils.capitalized
import io.bkbn.lerasium.utils.StringUtils.decapitalized
import io.ktor.http.HttpStatusCode
import io.ktor.routing.Route
import java.util.Locale
import java.util.UUID

@OptIn(KotlinPoetKspPreview::class)
class ApiVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  companion object {
    val routeMember = MemberName("io.ktor.routing", "route")
    val notarizedGetMember = MemberName("io.bkbn.kompendium.core.Notarized", "notarizedGet")
    val notarizedPostMember = MemberName("io.bkbn.kompendium.core.Notarized", "notarizedPost")
    val notarizedPutMember = MemberName("io.bkbn.kompendium.core.Notarized", "notarizedPut")
    val notarizedDeleteMember = MemberName("io.bkbn.kompendium.core.Notarized", "notarizedDelete")
    val callMember = MemberName("io.ktor.application", "call")
    val receiveMember = MemberName("io.ktor.request", "receive")
    val respondMember = MemberName("io.ktor.response", "respond")
    val respondText = MemberName("io.ktor.response", "respondText")
  }

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Api", classDeclaration)
      return
    }

    val domain = classDeclaration.findParentDomain()
    val apiObjectName = domain.name.plus("Api")

    fileBuilder.addType(TypeSpec.objectBuilder(apiObjectName).apply {
      addOriginatingKSFile(classDeclaration.containingFile!!)
      addController(classDeclaration, domain)
    }.build())
  }

  private fun TypeSpec.Builder.addController(cd: KSClassDeclaration, domain: Domain) {
    val controllerName = domain.name.plus("Controller").replaceFirstChar { it.lowercase(Locale.getDefault()) }
    val baseName = domain.name.replaceFirstChar { it.lowercase(Locale.getDefault()) }
    addFunction(FunSpec.builder(controllerName).apply {
      receiver(Route::class)
      addParameter(ParameterSpec.builder("dao", domain.toDaoClass()).build())
      addCodeBlock {
        addControlFlow("%M(%S)", routeMember, "/$baseName") {
          addCreateRoute(domain)
          addGetAllRoute(domain)
          addControlFlow("%M(%S)", routeMember, "/{id}") {
            addReadRoute(domain)
            addUpdateRoute(domain)
            addDeleteRoute(domain)
            addRelationalRoutes(cd, domain)
          }
          addControlFlow("%M(%S)", routeMember, "/count") {
            addCountRoute(domain)
          }
          addQueries(domain, cd)
        }
      }
    }.build())
  }

  private fun CodeBlock.Builder.addCreateRoute(domain: Domain) {
    add(CodeBlock.builder().apply {
      val toc = MemberName(domain.toTocClass(), "create${domain.name}")
      addControlFlow("%M(%M)", notarizedPostMember, toc) {
        addStatement(
          "val request = %M.%M<%T>()",
          callMember,
          receiveMember,
          List::class.asClassName().parameterizedBy(domain.toCreateRequestClass())
        )
        addStatement("val result = dao.create(request)")
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }.build())
  }

  private fun CodeBlock.Builder.addCountRoute(domain: Domain) {
    add(CodeBlock.builder().apply {
      val toc = MemberName(domain.toTocClass(), "countAll${domain.name}")
      addControlFlow("%M(%M)", notarizedGetMember, toc) {
        addStatement("val result = dao.countAll()")
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }.build())
  }

  private fun CodeBlock.Builder.addGetAllRoute(domain: Domain) {
    add(CodeBlock.builder().apply {
      val toc = MemberName(domain.toTocClass(), "getAll${domain.name}")
      addControlFlow("%M(%M)", notarizedGetMember, toc) {
        addStatement("val chunk = %M.parameters[%S]?.toInt() ?: 100", callMember, "chunk")
        addStatement("val offset = %M.parameters[%S]?.toInt() ?: 0", callMember, "offset")
        addStatement("val result = dao.getAll(chunk, offset)")
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }.build())
  }

  private fun CodeBlock.Builder.addReadRoute(domain: Domain) {
    add(CodeBlock.builder().apply {
      val toc = MemberName(domain.toTocClass(), "get${domain.name}")
      addControlFlow("%M(%M)", notarizedGetMember, toc) {
        addStatement("val id = %T.fromString(%M.parameters[%S])", UUID::class, callMember, "id")
        addStatement("val result = dao.read(id)")
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }.build())
  }

  private fun CodeBlock.Builder.addUpdateRoute(domain: Domain) {
    add(CodeBlock.builder().apply {
      val toc = MemberName(domain.toTocClass(), "update${domain.name}")
      addControlFlow("%M(%M)", notarizedPutMember, toc) {
        addStatement("val id = %T.fromString(%M.parameters[%S])", UUID::class, callMember, "id")
        addStatement("val request = %M.%M<%T>()", callMember, receiveMember, domain.toUpdateRequestClass())
        addStatement("val result = dao.update(id, request)")
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }.build())
  }

  private fun CodeBlock.Builder.addDeleteRoute(domain: Domain) {
    add(CodeBlock.builder().apply {
      val toc = MemberName(domain.toTocClass(), "delete${domain.name}")
      addControlFlow("%M(%M)", notarizedDeleteMember, toc) {
        addStatement("val id = %T.fromString(%M.parameters[%S])", UUID::class, callMember, "id")
        addStatement("dao.delete(id)")
        addStatement("%M.%M(%T.NoContent)", callMember, respondMember, HttpStatusCode::class)
      }
    }.build())
  }

  private fun CodeBlock.Builder.addRelationalRoutes(cd: KSClassDeclaration, domain: Domain) {
    cd.getAllProperties().filter { it.isAnnotationPresent(Relation::class) }.forEach { property ->
      val name = property.simpleName.getShortName()
      val refDomain = property.type.getDomain()
      add(CodeBlock.builder().apply {
        val toc = MemberName(domain.toTocClass(), "get${domain.name}${refDomain.name}")
        addControlFlow("%M(%S)", routeMember, "/${name.decapitalized()}") {
          addControlFlow("%M(%M)", notarizedGetMember, toc) {
            addStatement("val id = %T.fromString(%M.parameters[%S])", UUID::class, callMember, "id")
            addStatement("val chunk = %M.parameters[%S]?.toInt() ?: 100", callMember, "chunk")
            addStatement("val offset = %M.parameters[%S]?.toInt() ?: 0", callMember, "offset")
            addStatement("val result = dao.getAll${name.capitalized()}(id, chunk, offset)")
            addStatement("%M.%M(result)", callMember, respondMember)
          }
        }
      }.build())
    }
  }

  private fun CodeBlock.Builder.addQueries(domain: Domain, cd: KSClassDeclaration) {
    cd.getAllProperties().filter { it.isAnnotationPresent(GetBy::class) }.forEach { prop ->
      val getBy = prop.getAnnotationsByType(GetBy::class).first()
      when (getBy.unique) {
        true -> addUniqueQuery(domain, prop)
        false -> addNonUniqueQuery(domain, prop)
      }
    }
  }

  private fun CodeBlock.Builder.addUniqueQuery(domain: Domain, prop: KSPropertyDeclaration) {
    val name = prop.simpleName.getShortName()
    val toc = MemberName(domain.toTocClass(), "getBy${name.capitalized()}")
    addControlFlow("%M(%S)", routeMember, "/$name/{$name}") {
      addControlFlow("%M(%M)", notarizedGetMember, toc) {
        addStatement("val $name = call.parameters[%S]!!", name)
        addStatement("val result = dao.getBy${name.capitalized()}($name)")
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }
  }

  private fun CodeBlock.Builder.addNonUniqueQuery(domain: Domain, prop: KSPropertyDeclaration) {
    val name = prop.simpleName.getShortName()
    val toc = MemberName(domain.toTocClass(), "getBy${name.capitalized()}")
    addControlFlow("%M(%S)", routeMember, "/$name/{$name}") {
      addControlFlow("%M(%M)", notarizedGetMember, toc) {
        addStatement("val $name = call.parameters[%S]!!", name)
        addStatement("val chunk = %M.parameters[%S]?.toInt() ?: 100", callMember, "chunk")
        addStatement("val offset = %M.parameters[%S]?.toInt() ?: 0", callMember, "offset")
        addStatement("val result = dao.getBy${name.capitalized()}($name, chunk, offset)")
        addStatement("%M.%M(result)", callMember, respondMember)
      }
    }
  }
}
