package io.bkbn.lerasium.core.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class ModelProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return ModelProcessor(
      codeGenerator = environment.codeGenerator,
      logger = environment.logger,
      options = environment.options
    )
  }
}
