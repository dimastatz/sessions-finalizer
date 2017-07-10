package com.clicktale.pipeline.sessionsfinalizer.contracts

import akka.http.scaladsl.server._
import com.typesafe.scalalogging._
import akka.http.scaladsl.server.Directives._
import com.clicktale.pipeline.sessionsfinalizer.contracts.RoutingService._

// kafka, rabbit, as
trait RoutingService extends LazyLogging{
  def getAddress: NetworkAddress

  def getRoutes: Route = {
    getHealthRoute ~ getDefaultRoute
  }

  def getDefaultRoute: Route = get {
    pathPrefix("") {
      complete("Welcome to sessions-finalizer service")
    }
  }

  def getHealthRoute: Route = {
    pathPrefix("health" / IntNumber) { v =>
      get {
        extractMethod { m => complete("0") }
      }
    }
  }
}

object RoutingService {
  case class NetworkAddress(port: Int, host: String)
}

