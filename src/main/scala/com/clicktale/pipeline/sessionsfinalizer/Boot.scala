package com.clicktale.pipeline.sessionsfinalizer

import scala.util._
import akka.actor._
import akka.stream._
import java.nio.file._
import akka.http.scaladsl._
import com.typesafe.scalalogging._
import com.typesafe.config.{Config, ConfigFactory}
import com.clicktale.pipeline.sessionsfinalizer.actors._

object Boot extends LazyLogging {
  def main(args: Array[String]): Unit = {
    import system.dispatcher
    implicit val system = ActorSystem("sefer")
    implicit val materializer = initMaterializer()

    val config = loadConfig()
    logger.debug(s"config is loaded for env ${config.getString("conf.env")}")

    val controller = new Controller(null)
    logger.debug(s"Controller initialized")

    val actorScheduler = system.actorOf(Props(new ActorScheduler(controller)))
    logger.debug(s"Scheduler initialized")

    val address = Address(config.getString("conf.host"), config.getInt("conf.port"))
    val binding = Http().bindAndHandle(Routes.getRoutes, address.host, address.port)
    logger.debug(s"services is bind to host: ${address.host}, port: ${address.port}")

    sys addShutdownHook {
      logger.debug(s"sessions-finalizer service is terminating.")
      binding.flatMap(_.unbind()).onComplete(_ => system.terminate())
    }
  }

  private def loadConfig() = {
    val appConf = "app.conf"
    val logFile = "./logback.xml"
    val logSettings = "logback.configurationFile"

    Try(Paths.get(logFile)).map(i => System.setProperty(logSettings, logFile))
    Try(ConfigFactory.load(s"./$appConf")).getOrElse(ConfigFactory.load(appConf))
  }

  private def initMaterializer()(implicit system: ActorSystem) = {
    val decider: Supervision.Decider = {
      case _: Exception => Supervision.Resume
      case _ => Supervision.Stop
    }
    val settings = ActorMaterializerSettings(system)
    ActorMaterializer(settings.withSupervisionStrategy(decider))
  }

  case class Address(host: String, port: Int)
}
