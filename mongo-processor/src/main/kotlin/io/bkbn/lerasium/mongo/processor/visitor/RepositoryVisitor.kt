package io.bkbn.lerasium.mongo.processor.visitor

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.mongodb.client.MongoCollection
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain

class RepositoryVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  companion object {
    private val GetCollection = MemberName("org.litote.kmongo", "getCollection")
    private val FindOneById = MemberName("org.litote.kmongo", "findOneById")
    private val DeleteOneById = MemberName("org.litote.kmongo", "deleteOneById")
    private val EnsureIndex = MemberName("org.litote.kmongo", "ensureIndex")
    private val EnsureUniqueIndex = MemberName("org.litote.kmongo", "ensureUniqueIndex")
    private val Save = MemberName("org.litote.kmongo", "save")
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

      addProperty(
        PropertySpec.builder(
          "collection",
          MongoCollection::class.asTypeName().parameterizedBy(charter.documentClass)
        ).apply {
          addModifiers(KModifier.PRIVATE)
          initializer("db.%M()", GetCollection)
        }.build()
      )

      addCreateFunction(charter)
      addReadFunction(charter)
      addUpdateFunction(charter)
      addDeleteFunction(charter)
    }.build())
  }

  private fun TypeSpec.Builder.addCreateFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("create").apply {
      returns(charter.domainClass)
      addStatement("TODO()")
    }.build())
  }

  private fun TypeSpec.Builder.addReadFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("read").apply {
      returns(charter.domainClass)
      addStatement("TODO()")
    }.build())
  }

  private fun TypeSpec.Builder.addUpdateFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("update").apply {
      returns(charter.domainClass)
      addStatement("TODO()")
    }.build())
  }

  private fun TypeSpec.Builder.addDeleteFunction(charter: LerasiumCharter) {
    addFunction(FunSpec.builder("delete").apply {
      addStatement("TODO()")
    }.build())
  }
}
