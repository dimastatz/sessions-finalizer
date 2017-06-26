package com.clicktale.pipeline.sessionsfinalizer

import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

class Routes(){
  def health(): Route = {
    pathPrefix("health" / IntNumber) { v =>
      get {
        extractMethod { m =>
          complete("running")
        }
      }
    }
  }

  def default(): Route = get {
    pathPrefix("") {
      complete("Welcome to sessions-finalizer service")
    }
  }
}


object Routes {
  def getRoutes: Route = {
    val routes = new Routes()
    routes.default() ~ routes.health()
  }
}


