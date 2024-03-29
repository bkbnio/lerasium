package io.bkbn.lerasium.utils

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import io.bkbn.lerasium.core.Sensitive

@OptIn(KspExperimental::class)
data class PropertyWrapper(
  val scalars: Sequence<KSPropertyDeclaration>,
  val relations: Sequence<KSPropertyDeclaration>,
  val nested: Sequence<KSPropertyDeclaration>,
  val enums: Sequence<KSPropertyDeclaration>,
) {
  fun filterSensitive() = PropertyWrapper(
    scalars.filterNot { it.isAnnotationPresent(Sensitive::class) },
    relations.filterNot { it.isAnnotationPresent(Sensitive::class) },
    nested.filterNot { it.isAnnotationPresent(Sensitive::class) },
    enums.filterNot { it.isAnnotationPresent(Sensitive::class) },
  )

  fun filterId() = PropertyWrapper(
    scalars.filterNot { it.simpleName.asString() == "id" },
    relations,
    nested,
    enums,
  )
}
