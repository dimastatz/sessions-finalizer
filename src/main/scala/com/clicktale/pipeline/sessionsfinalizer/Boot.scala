package com.clicktale.pipeline.sessionsfinalizer

import akka.actor._
import akka.stream._
import akka.http.scaladsl._
import com.typesafe.scalalogging._
import com.clicktale.pipeline.sessionsfinalizer.actors._

object Boot extends LazyLogging {
  def main(args: Array[String]): Unit = {
    import system.dispatcher
    implicit val system = ActorSystem("sefer")
    implicit val materializer = initMaterializer()

    val config = Configuration.load()
    logger.debug(s"config is loaded for env ${config.environment}")

    val controller = new Controller(null)
    logger.debug(s"Controller initialized")

    val actorScheduler = system.actorOf(Props(new ActorScheduler(controller)))
    logger.debug(s"Scheduler initialized")

    val binding = Http().bindAndHandle(
      Routes.routingTable, config.address.host, config.address.port)
    logger.debug(s"services is bind to ${config.address.host} ${config.address.port}")

    sys addShutdownHook {
      logger.debug(s"sessions-finalizer service is terminating.")
      binding.flatMap(_.unbind()).onComplete(_ => system.terminate())
    }
  }

  private def initMaterializer()(implicit system: ActorSystem) = {
    val decider: Supervision.Decider = {
      case _: Exception => Supervision.Resume
      case _ => Supervision.Stop
    }
    val settings = ActorMaterializerSettings(system)
    ActorMaterializer(settings.withSupervisionStrategy(decider))
  }
}
