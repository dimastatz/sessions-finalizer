package com.clicktale.pipeline.sessionsfinalizer

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

object Routes {
  private val health: Route = {
    pathPrefix("health" / IntNumber) { v =>
      get {
        extractMethod { m =>
          complete("running")
        }
      }
    }
  }

  private val default: Route = get {
    pathPrefix("") {
      complete("Welcome to sessions-finalizer service")
    }
  }

  val routingTable: Route = health ~ default
}


