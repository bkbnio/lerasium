package io.bkbn.lerasium.core.model

import io.bkbn.lerasium.core.converter.ResponseConverter

interface Entity<R : Response> : ResponseConverter<R>
