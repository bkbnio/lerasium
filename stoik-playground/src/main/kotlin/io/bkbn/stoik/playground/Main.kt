package io.bkbn.stoik.playground

import io.bkbn.stoik.generated.UserApi.userController
import io.ktor.application.Application
import io.ktor.routing.route
import io.ktor.routing.routing

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
  routing {
    route("/user") {
      userController()
    }
  }
}
