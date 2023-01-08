package io.bkbn.lerasium.rdbms.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo
import io.bkbn.lerasium.rdbms.Table
import io.bkbn.lerasium.rdbms.processor.visitor.TableVisitor
import io.bkbn.lerasium.rdbms.processor.visitor.RepositoryVisitor
import io.bkbn.lerasium.utils.KotlinPoetUtils.PERSISTENCE_CONFIG_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.REPOSITORY_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.TABLE_PACKAGE_NAME
import io.bkbn.lerasium.utils.KotlinPoetUtils.addControlFlow
import io.bkbn.lerasium.utils.LerasiumUtils.getDomain
import org.flywaydb.core.Flyway
import org.komapper.r2dbc.R2dbcDatabase

class RdbmsProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  options: Map<String, String>
) : SymbolProcessor {

  init {
    logger.info(options.toString())
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = resolver
      .getSymbolsWithAnnotation(Table::class.qualifiedName!!)
      .filterIsInstance<KSClassDeclaration>()

    if (!symbols.iterator().hasNext()) return emptyList()

    symbols.forEach {
      val domain = it.getDomain()
      val fb = FileSpec.builder(TABLE_PACKAGE_NAME, domain.name.plus("Table"))
      it.accept(TableVisitor(fb, logger), Unit)
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
    val fb = FileSpec.builder(PERSISTENCE_CONFIG_PACKAGE_NAME, "PostgresConfig")
    fb.addPostgresConfig()
    val fs = fb.build()
    fs.writeTo(codeGenerator, false)
  }

  private fun FileSpec.Builder.addPostgresConfig() {
    addType(TypeSpec.objectBuilder("PostgresConfig").apply {
      addProperty(PropertySpec.builder("USER", String::class).apply {
        addModifiers(KModifier.CONST, KModifier.PRIVATE)
        initializer("%S", "test_user")
      }.build())
      addProperty(PropertySpec.builder("PASSWORD", String::class).apply {
        addModifiers(KModifier.CONST, KModifier.PRIVATE)
        initializer("%S", "test_password")
      }.build())
      addProperty(PropertySpec.builder("SYNC_CONNECTION_URI", String::class).apply {
        addModifiers(KModifier.CONST, KModifier.PRIVATE)
        initializer("%P", "jdbc:postgresql://localhost:5432/test_db")
      }.build())
      addProperty(PropertySpec.builder("ASYNC_CONNECTION_URI", String::class).apply {
        addModifiers(KModifier.CONST, KModifier.PRIVATE)
        initializer("%P", "r2dbc:postgresql://\$USER:\$PASSWORD@localhost:5432/test_db")
      }.build())
      addProperty(PropertySpec.builder("flyway", Flyway::class).apply {
        delegate(CodeBlock.builder().apply {
          addControlFlow("lazy") {
            addControlFlow("val config = %T.configure().apply", Flyway::class) {
              addStatement("cleanDisabled(%L)", false)
              addStatement("dataSource(%L, %L, %L)", "SYNC_CONNECTION_URI", "USER", "PASSWORD")
              addStatement("locations(%S)", "db/migration")
            }
            addStatement(
              "config.load() ?: error(%S)",
              "Problem Loading Flyway!! Please verify Database Connection / Migration Info"
            )
          }
        }.build())
      }.build())
      addProperty(PropertySpec.builder("database", R2dbcDatabase::class).apply {
        delegate(CodeBlock.builder().apply {
          addControlFlow("lazy") {
            addStatement("%T(%L)", R2dbcDatabase::class, "ASYNC_CONNECTION_URI")
          }
        }.build())
      }.build())
    }.build())
  }
}
