package io.bkbn.lerasium.core.model

sealed interface IORequest {
  interface Create : IORequest
  interface Update : IORequest
}
