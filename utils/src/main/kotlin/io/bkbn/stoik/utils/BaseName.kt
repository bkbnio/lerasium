package io.bkbn.stoik.utils

import com.squareup.kotlinpoet.ClassName
import io.bkbn.stoik.core.Metadata
import io.bkbn.stoik.utils.StringUtils.capitalized

typealias BaseName = String

fun BaseName.toCreateRequestClass(): ClassName =
  ClassName(Metadata.BASE_PACKAGE, this.capitalized().plus("CreateRequest"))

fun BaseName.toUpdateRequestClass(): ClassName =
  ClassName(Metadata.BASE_PACKAGE, this.capitalized().plus("UpdateRequest"))

fun BaseName.toResponseClass(): ClassName = ClassName(Metadata.BASE_PACKAGE, this.capitalized().plus("Response"))
