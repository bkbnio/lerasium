package io.bkbn.lerasium.api.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import io.bkbn.lerasium.api.GetBy
import io.bkbn.lerasium.utils.LerasiumCharter

@OptIn(KspExperimental::class)
val LerasiumCharter.hasQueries: Boolean
  get() = cd.getAllProperties().any { it.isAnnotationPresent(GetBy::class) }
