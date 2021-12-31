package io.bkbn.stoik.dao.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.writeTo
import io.bkbn.stoik.dao.core.Dao
import kotlinx.serialization.Serializable

@OptIn(KotlinPoetKspPreview::class, KspExperimental::class)
class DaoProcessor(
  private val codeGenerator: CodeGenerator,
  private val logger: KSPLogger,
  options: Map<String, String>
) : SymbolProcessor {

  init {
    logger.info(options.toString())
  }

  override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = resolver
      .getSymbolsWithAnnotation(Dao::class.qualifiedName!!)
      .filterIsInstance<KSClassDeclaration>()

    if (!symbols.iterator().hasNext()) return emptyList()

    symbols.forEach {
      val fb = FileSpec.builder("io.bkbn.stoik.generated", "Dao")
      it.accept(Visitor(fb), Unit)
      val fs = fb.build()
      fs.writeTo(codeGenerator, false)
    }

    return symbols.filterNot { it.validate() }.toList()
  }

  inner class Visitor(private val fileBuilder: FileSpec.Builder) : KSVisitorVoid() {
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
      if (classDeclaration.classKind != ClassKind.INTERFACE) {
        logger.error("Only an interface can be decorated with @Dao", classDeclaration)
        return
      }

      val dao: Dao = classDeclaration.getAnnotationsByType(Dao::class).first()

      val entityName = dao.name
      val daoName = "${entityName}Dao"

      fileBuilder.addType(TypeSpec.classBuilder(daoName).apply {
        addModifiers(KModifier.OPEN)
        // todo create
        // todo read
        // todo update
        // todo delete
      }.build())

      fileBuilder.addType(TypeSpec.classBuilder("Create${entityName}Request").apply {
        addAnnotation(Serializable::class)
        // todo
      }.build())

      fileBuilder.addType(TypeSpec.classBuilder("Update${entityName}Request").apply {
        addAnnotation(Serializable::class)
        // todo
      }.build())

      fileBuilder.addType(TypeSpec.classBuilder("${entityName}Response").apply {
        addAnnotation(Serializable::class)
        // todo
      }.build())
    }
  }
}
