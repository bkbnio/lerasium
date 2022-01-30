package io.bkbn.stoik.core.model

import io.bkbn.stoik.core.converter.ResponseConverter

interface Entity<R : Response> : ResponseConverter<R>
