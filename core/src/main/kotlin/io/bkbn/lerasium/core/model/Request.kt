package io.bkbn.lerasium.core.model

sealed interface Request {
  interface Create : Request
  interface Update : Request
}
