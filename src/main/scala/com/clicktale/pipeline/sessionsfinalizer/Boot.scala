package com.clicktale.pipeline.sessionsfinalizer

import akka.actor._
import akka.stream._
import java.nio.file._
import akka.http.scaladsl._
import com.typesafe.scalalogging._

object Boot extends LazyLogging {
  def main(args: Array[String]): Unit = {
    import system.dispatcher
    implicit val system = ActorSystem("cage")
    implicit val materializer = initMaterializer()

    loadLogback()
    val binding = Http().bindAndHandle(Routes.routingTable, "0.0.0.0", 8080)

    sys addShutdownHook {
      logger.debug(s"sessions-finalizer service is terminating.")
      binding.flatMap(_.unbind()).onComplete(_ => system.terminate())
    }
  }

  private def loadLogback() = {
    if(Files.exists(Paths.get("./logback.xml")))
      System.setProperty("logback.configurationFile", s"./logback.xml")
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
