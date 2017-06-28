package com.clicktale.pipeline.sessionsfinalizer

import akka.http.scaladsl._
import com.google.inject.Guice
import com.typesafe.scalalogging._
import akka.stream.ActorMaterializer
import akka.actor.{ActorRef, ActorSystem}
import net.codingwell.scalaguice.InjectorExtensions._
import com.clicktale.pipeline.sessionsfinalizer.contracts._

object Boot extends LazyLogging {
  def main(args: Array[String]): Unit = {
    val injector = Guice.createInjector(new Injector())
    implicit val system = injector.instance[ActorSystem]
    implicit val executionContext = system.dispatcher
    implicit val materializer = injector.instance[ActorMaterializer]
    logger.debug("dependency injection mechanism is created")

    val scheduler = injector.instance[ActorRef]
    logger.debug(s"scheduler (main) actor is running")

    val routes = injector.instance[RoutingService]
    val address = (routes.getAddress.host, routes.getAddress.port)
    val binding = Http().bindAndHandle(routes.getRoutes, address._1, address._2)
    logger.debug(s"service is listening to network on ${routes.getAddress}")

    sys addShutdownHook {
      logger.debug(s"sessions-finalizer service is terminating.")
      binding.flatMap(_.unbind()).onComplete(_ => system.terminate())
    }
  }
}
