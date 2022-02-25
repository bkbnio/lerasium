package io.bkbn.lerasium.api.processor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import io.bkbn.kompendium.core.metadata.RequestInfo
import io.bkbn.kompendium.core.metadata.ResponseInfo
import io.bkbn.kompendium.core.metadata.method.DeleteInfo
import io.bkbn.kompendium.core.metadata.method.GetInfo
import io.bkbn.kompendium.core.metadata.method.PostInfo
import io.bkbn.kompendium.core.metadata.method.PutInfo
import io.bkbn.lerasium.api.model.GetByIdParams
import io.bkbn.lerasium.core.Domain
import io.bkbn.lerasium.core.model.CountResponse
import io.bkbn.lerasium.utils.KotlinPoetUtils.addObjectInstantiation
import io.bkbn.lerasium.utils.KotlinPoetUtils.toCreateRequestClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toResponseClass
import io.bkbn.lerasium.utils.KotlinPoetUtils.toUpdateRequestClass
import io.bkbn.lerasium.utils.LerasiumUtils.findParentDomain
import io.ktor.http.HttpStatusCode

@OptIn(KotlinPoetKspPreview::class)
class TocVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Api", classDeclaration)
      return
    }

    val domain = classDeclaration.findParentDomain()
    val tocName = domain.name.plus("ToC")

    fileBuilder.addType(TypeSpec.objectBuilder(tocName).apply {
      addOriginatingKSFile(classDeclaration.containingFile!!)
      addCreateInfo(domain)
      addCountInfo(domain)
      addReadInfo(domain)
      addUpdateInfo(domain)
      addDeleteInfo(domain)
    }.build())
  }

  private fun TypeSpec.Builder.addReadInfo(domain: Domain) {
    val readPropType = GetInfo::class.asClassName()
      .parameterizedBy(GetByIdParams::class.asClassName(), domain.name.toResponseClass())
    addProperty(PropertySpec.builder("get${domain.name}", readPropType).apply {
      initializer(CodeBlock.builder().apply {
        addObjectInstantiation(readPropType) {
          addStatement("summary = %S,", "Get ${domain.name} by ID")
          addStatement("description = %S,", "Retrieves a ${domain.name} by id")
          add("responseInfo = ")
          addObjectInstantiation(ResponseInfo::class.asClassName(), trailingComma = true) {
            addStatement("status = %T.OK,", HttpStatusCode::class)
            addStatement("description = %S", "The ${domain.name} was retrieved successfully")
          }
          addStatement("tags = setOf(%S)", domain.name)
        }
      }.build())
    }.build())
  }

  private fun TypeSpec.Builder.addCreateInfo(domain: Domain) {
    val createPropType = PostInfo::class.asClassName()
      .parameterizedBy(Unit::class.asClassName(), domain.toCreateRequestClass(), domain.toResponseClass())
    addProperty(PropertySpec.builder("create${domain.name}", createPropType).apply {
      initializer(CodeBlock.builder().apply {
        addObjectInstantiation(createPropType) {
          addStatement("summary = %S,", "Create ${domain.name}")
          addStatement("description = %S,", "Creates a new ${domain.name}")
          add("requestInfo = ")
          addObjectInstantiation(RequestInfo::class.asTypeName(), trailingComma = true) {
            addStatement("description = %S,", "Details required to create a new ${domain.name}")
          }
          add("responseInfo = ")
          addObjectInstantiation(ResponseInfo::class.asClassName(), trailingComma = true) {
            addStatement("status = %T.Created,", HttpStatusCode::class)
            addStatement("description = %S", "The ${domain.name} was retrieved successfully")
          }
          addStatement("tags = setOf(%S)", domain.name)
        }
      }.build())
    }.build())
  }

  private fun TypeSpec.Builder.addCountInfo(domain: Domain) {
    val countPropType = GetInfo::class.asClassName()
      .parameterizedBy(Unit::class.asClassName(), CountResponse::class.asClassName())
    addProperty(PropertySpec.builder("countAll${domain.name}", countPropType).apply {
      initializer(CodeBlock.builder().apply {
        addObjectInstantiation(countPropType) {
          addStatement("summary = %S,", "Count ${domain.name}")
          addStatement("description = %S,", "Counts total ${domain.name} entities")
          add("responseInfo = ")
          addObjectInstantiation(ResponseInfo::class.asClassName(), trailingComma = true) {
            addStatement("status = %T.OK,", HttpStatusCode::class)
            addStatement("description = %S", "Successfully retrieved the total ${domain.name} entity count")
          }
          addStatement("tags = setOf(%S)", domain.name)
        }
      }.build())
    }.build())
  }

  private fun TypeSpec.Builder.addUpdateInfo(domain: Domain) {
    val updatePropType = PutInfo::class.asClassName()
      .parameterizedBy(GetByIdParams::class.asTypeName(), domain.toUpdateRequestClass(), domain.toResponseClass())
    addProperty(PropertySpec.builder("update${domain.name}", updatePropType).apply {
      initializer(CodeBlock.builder().apply {
        addObjectInstantiation(updatePropType) {
          addStatement("summary = %S,", "Update ${domain.name}")
          addStatement("description = %S,", "Updates an existing ${domain.name}")
          add("requestInfo = ")
          addObjectInstantiation(RequestInfo::class.asTypeName(), trailingComma = true) {
            addStatement(
              "description = %S,",
              "Takes an provided fields and overrides the corresponding ${domain.name} info"
            )
          }
          add("responseInfo = ")
          addObjectInstantiation(ResponseInfo::class.asClassName(), trailingComma = true) {
            addStatement("status = %T.Created,", HttpStatusCode::class)
            addStatement("description = %S", "The ${domain.name} was updated successfully")
          }
          addStatement("tags = setOf(%S)", domain.name)
        }
      }.build())
    }.build())
  }

  private fun TypeSpec.Builder.addDeleteInfo(domain: Domain) {
    val deletePropType = DeleteInfo::class.asClassName()
      .parameterizedBy(GetByIdParams::class.asTypeName(), Unit::class.asTypeName())
    addProperty(PropertySpec.builder("delete${domain.name}", deletePropType).apply {
      initializer(CodeBlock.builder().apply {
        addObjectInstantiation(deletePropType) {
          addStatement("summary = %S,", "Delete ${domain.name} by ID")
          addStatement("description = %S,", "Deletes an existing ${domain.name}")
          add("responseInfo = ")
          addObjectInstantiation(ResponseInfo::class.asTypeName(), trailingComma = true) {
            addStatement("status = %T.NoContent,", HttpStatusCode::class)
            addStatement("description = %S", "Successfully deleted ${domain.name}")
          }
          addStatement("tags = setOf(%S)", domain.name)
        }
      }.build())
    }.build())
  }
}
