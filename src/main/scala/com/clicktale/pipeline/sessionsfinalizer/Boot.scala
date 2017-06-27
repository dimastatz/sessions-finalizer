package com.clicktale.pipeline.sessionsfinalizer

import akka.http.scaladsl._
import com.google.inject.Guice
import com.typesafe.scalalogging._
import akka.stream.ActorMaterializer
import akka.actor.{ActorRef, ActorSystem}
import scala.concurrent.ExecutionContext
import net.codingwell.scalaguice.InjectorExtensions._
import com.clicktale.pipeline.sessionsfinalizer.contracts._

object Boot extends LazyLogging {
  def main(args: Array[String]): Unit = {
    val injector = Guice.createInjector(new Injector())
    implicit val system = injector.instance[ActorSystem]
    implicit val context = injector.instance[ExecutionContext]
    implicit val materializer = injector.instance[ActorMaterializer]
    logger.debug("injector created")

    val routes = injector.instance[RoutingService]
    val binding = Http().bindAndHandle(routes.getRoutes, routes.getAddress.host, routes.getAddress.port)
    logger.debug(s"service is listening to ${routes.getAddress.port}")

    val scheduler = injector.instance[ActorRef]
    logger.debug(s"scheduler (main) actor is created")

    sys addShutdownHook {
      logger.debug(s"sessions-finalizer service is terminating.")
      binding.flatMap(_.unbind()).onComplete(_ => system.terminate())
    }
  }
}
