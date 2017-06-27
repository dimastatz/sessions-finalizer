package com.clicktale.pipeline.sessionsfinalizer.contracts

import akka.http.scaladsl.server._
import com.typesafe.scalalogging._
import akka.http.scaladsl.server.Directives._

trait RoutingService extends LazyLogging{
  def getDefaultRoute: Route = get {
    pathPrefix("") {
      complete("Welcome to sessions-finalizer service")
    }
  }
  def getHealthRoute: Route = {
    pathPrefix("health" / IntNumber) { v =>
      get {
        extractMethod { m =>
          complete("running")
        }
      }
    }
  }
}

object RoutingService{
  class Empty {}
  def getRoutes: Route = {
    val service = new Empty with RoutingService
    service.getDefaultRoute ~ service.getHealthRoute
  }
}
