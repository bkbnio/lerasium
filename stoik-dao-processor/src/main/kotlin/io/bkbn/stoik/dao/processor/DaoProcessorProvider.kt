package io.bkbn.stoik.dao.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class DaoProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return DaoProcessor(
      codeGenerator = environment.codeGenerator,
      logger = environment.logger,
      options = environment.options
    )
  }
}
