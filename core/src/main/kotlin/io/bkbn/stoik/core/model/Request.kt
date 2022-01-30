package io.bkbn.stoik.core.model

sealed interface Request {
  interface Create : Request
  interface Update : Request
}
