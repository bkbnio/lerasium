package io.bkbn.lerasium.permissions.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class PermissionProcessorProvider : SymbolProcessorProvider {
  override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
    return PermissionProcessor(
      codeGenerator = environment.codeGenerator,
      logger = environment.logger,
      options = environment.options
    )
  }
}
