package io.bkbn.lerasium.mongo.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class KMongoProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return KMongoProcessor(
      codeGenerator = environment.codeGenerator,
      logger = environment.logger,
      options = environment.options
    )
  }
}
