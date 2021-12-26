package io.bkbn.stoik.exposed.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview

class ExposedProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return ExposedProcessor(
      codeGenerator = environment.codeGenerator,
      logger = environment.logger,
      options = environment.options
    )
  }
}
