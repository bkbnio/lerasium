package io.bkbn.lerasium.mongo.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoDatabase
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import io.bkbn.lerasium.mongo.Document
import io.bkbn.lerasium.mongo.processor.visitor.RepositoryVisitor
import io.bkbn.lerasium.mongo.processor.visitor.RootDocumentVisitor
import io.bkbn.lerasium.utils.KotlinPoetUtils.DOCUMENT_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.PERSISTENCE_CONFIG_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.REPOSITORY_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import org.bson.UuidRepresentation
import org.litote.kmongo.KMongo

class KMongoProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  options: Map<String, String>
) : SymbolProcessor {

  init {
    logger.info(options.toString())
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = resolver
      .getSymbolsWithAnnotation(Document::class.qualifiedName!!)
      .filterIsInstance<KSClassDeclaration>()

    if (!symbols.iterator().hasNext()) return emptyList()

    symbols.forEach {
      val domain = it.getDomain()
      val fb = FileSpec.builder(DOCUMENT_PACKAGE_NAME, domain.name.plus("Document"))
      it.accept(RootDocumentVisitor(fb, logger), Unit)
      val fs = fb.build()
      fs.writeTo(codeGenerator, false)
    }

    symbols.forEach {
      val domain = it.getDomain()
      val fb = FileSpec.builder(REPOSITORY_PACKAGE_NAME, domain.name.plus("Repository"))
      it.accept(RepositoryVisitor(fb, logger), Unit)
      val fs = fb.build()
      fs.writeTo(codeGenerator, false)
    }

    writeConfigFile()

    return symbols.filterNot { it.validate() }.toList()
  }

  private fun writeConfigFile() {
    val fb = FileSpec.builder(PERSISTENCE_CONFIG_PACKAGE_NAME, "MongoConfig")
    fb.addMongoConfig()
    val fs = fb.build()
    fs.writeTo(codeGenerator, false)
  }

  private fun FileSpec.Builder.addMongoConfig() {
    addType(TypeSpec.objectBuilder("MongoConfig").apply {
      addProperty(PropertySpec.builder("documentDatabase", MongoDatabase::class).apply {
        delegate(CodeBlock.builder().apply {
          addControlFlow("lazy") {
            addControlFlow("val clientSettingBuilder = %T.builder().apply", MongoClientSettings::class) {
              addStatement(
                "applyConnectionString(%T(%S))",
                ConnectionString::class,
                "mongodb://test_user:test_password@localhost:27017"
              )
              addStatement("uuidRepresentation(%T.STANDARD)", UuidRepresentation::class)
            }
            addStatement("val clientSettings = clientSettingBuilder.build()")
            addStatement("val mongoClient = %T.createClient(clientSettings)", KMongo::class)
            addStatement("mongoClient.getDatabase(%S)", "test_db")
          }
        }.build())
      }.build())
    }.build())
  }
}
