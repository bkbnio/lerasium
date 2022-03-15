package io.bkbn.lerasium.api.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toTypeName
import io.bkbn.kompendium.annotations.Param
import io.bkbn.kompendium.annotations.ParamType
import io.bkbn.lerasium.api.GetBy
import io.bkbn.lerasium.utils.LerasiumUtils.findParentDomain
import io.bkbn.lerasium.utils.StringUtils.capitalized

@OptIn(KotlinPoetKspPreview::class, KspExperimental::class)
class QueryModelVisitor(private val fileBuilder: FileSpec.Builder, private val logger: KSPLogger) : KSVisitorVoid() {

  override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
    if (classDeclaration.classKind != ClassKind.INTERFACE) {
      logger.error("Only an interface can be decorated with @Api", classDeclaration)
      return
    }

    fileBuilder.apply {
      addGetByQueries(classDeclaration)
    }
  }

  @Suppress("MagicNumber")
  private fun FileSpec.Builder.addGetByQueries(cd: KSClassDeclaration) {
    val domain = cd.findParentDomain()
    cd.getAllProperties().filter { it.isAnnotationPresent(GetBy::class) }.forEach { prop ->
      val name = prop.simpleName.getShortName()
      val getBy = prop.getAnnotationsByType(GetBy::class).first()
      addType(TypeSpec.classBuilder("Get${domain.name}By${name.capitalized()}Query").apply {
        addOriginatingKSFile(cd.containingFile!!)
        addModifiers(KModifier.DATA)
        primaryConstructor(FunSpec.constructorBuilder().apply {
          addParameter(ParameterSpec.builder(name, prop.type.toTypeName()).build())
          if (!getBy.unique) {
            addParameter(ParameterSpec.builder("chunk", Int::class).apply {
              defaultValue(CodeBlock.of("%L", 100))
            }.build())
            addParameter(ParameterSpec.builder("offset", Int::class).apply {
              defaultValue(CodeBlock.of("%L", 0))
            }.build())
          }
        }.build())
        addProperty(PropertySpec.builder(name, prop.type.toTypeName()).apply {
          addAnnotation(AnnotationSpec.builder(Param::class).apply {
            addMember(CodeBlock.of("%T.PATH", ParamType::class))
          }.build())
          initializer(name)
        }.build())
        if (!getBy.unique) {
          addProperty(PropertySpec.builder("chunk", Int::class).apply {
            addAnnotation(AnnotationSpec.builder(Param::class).apply {
              addMember(CodeBlock.of("%T.QUERY", ParamType::class))
            }.build())
            initializer(CodeBlock.of("chunk"))
          }.build())
          addProperty(PropertySpec.builder("offset", Int::class).apply {
            addAnnotation(AnnotationSpec.builder(Param::class).apply {
              addMember(CodeBlock.of("%T.QUERY", ParamType::class))
            }.build())
            initializer(CodeBlock.of("offset"))
          }.build())
        }
        // TODO
      }.build())
    }
  }

}
