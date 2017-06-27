package com.clicktale.pipeline.sessionsfinalizer

import scala.util._
import java.nio.file._
import scala.concurrent._
import com.google.inject._
import akka.stream.ActorMaterializer
import net.codingwell.scalaguice.ScalaModule
import com.typesafe.scalalogging.LazyLogging
import akka.actor.{ActorRef, ActorSystem, Props}
import com.typesafe.config.{Config, ConfigFactory}
import com.clicktale.pipeline.sessionsfinalizer.actors._
import com.clicktale.pipeline.sessionsfinalizer.Injector._
import com.clicktale.pipeline.sessionsfinalizer.contracts._
import com.clicktale.pipeline.sessionsfinalizer.contracts.RoutingService._
import com.clicktale.pipeline.sessionsfinalizer.contracts.SessionsFinalizerService.Session

class Injector extends AbstractModule with ScalaModule with LazyLogging{
  private val config = loadSystemConfig()
  implicit val system = ActorSystem("sefer")

  override def configure(): Unit = {
    bind[Config].toInstance(config)
    bind[ActorSystem].toInstance(system)
    bind[ActorMaterializer].toInstance(ActorMaterializer())
    bind[ExecutionContextExecutor].toInstance(system.dispatcher)
    bind[ExecutionContext].toInstance(scala.concurrent.ExecutionContext.global)
  }

  @Provides
  @Singleton def getRouter(@Inject ctx: ExecutionContext): RoutingService = {
    val port = config.getInt("conf.port")
    val host = config.getString("conf.host")

    // create router and inject getAddress
    val service = new Empty with RoutingService {
      override def getAddress = NetworkAddress(port, host)
    }
    service
  }

  @Provides
  @Singleton def getFinalizer(@Inject ctx: ExecutionContext): SessionsFinalizerService = {
    new Empty with SessionsFinalizerService {
      def getRequeueIntervalMs: Int = 1000
      def enqueue(session: Session): Unit = {}
      def loadExpiredSessionsBatch(): Seq[Session] = List()
      def requeueRequired(session: Session): Boolean = false
      def publishMetrics(sessions: Seq[Session], skipped: Seq[Session]): Unit = {}
    }
  }

  @Provides
  @Singleton def getScheduler(@Inject finalizer: SessionsFinalizerService): ActorRef = {
    logger.debug("creating scheduler")
    system.actorOf(Props(new ActorScheduler(finalizer)))
  }
}

object Injector {
  class Empty

  def loadSystemConfig(): Config = {
    val appConf = "app.conf"
    val logFile = "./logback.xml"
    val logSettings = "logback.configurationFile"

    Try(Paths.get(logFile)).map(i => System.setProperty(logSettings, logFile))
    Try(ConfigFactory.load(s"./$appConf")).getOrElse(ConfigFactory.load(appConf))
  }
}
