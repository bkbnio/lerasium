package io.bkbn.lerasium.core.model

import io.bkbn.lerasium.core.converter.ResponseConverter

@Deprecated("To remove once domain is fully established")
interface Entity<R : IOResponse> : ResponseConverter<R>
