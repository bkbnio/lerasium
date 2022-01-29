package io.bkbn.stoik.core.model

import io.bkbn.stoik.core.converter.ResponseConverter

interface Entity<T, R : Response> : ResponseConverter<T, R>
