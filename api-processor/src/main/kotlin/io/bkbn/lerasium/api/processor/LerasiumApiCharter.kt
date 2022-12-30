package io.bkbn.lerasium.api.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.isAnnotationPresent
import io.bkbn.lerasium.api.GetBy
import io.bkbn.lerasium.utils.LerasiumCharter
import io.bkbn.lerasium.utils.StringUtils.camelToSnakeCase

@OptIn(KspExperimental::class)
val LerasiumCharter.hasQueries: Boolean
  get() = cd.getAllProperties().any { it.isAnnotationPresent(GetBy::class) }

val LerasiumCharter.authSlug: String
  get() = "jwt_auth_${domain.name.camelToSnakeCase()}"
