package com.clicktale.pipeline.sessionsfinalizer

import scala.util._
import java.nio.file._
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
import com.clicktale.pipeline.sessionsfinalizer.contracts.FinalizerService._
import com.clicktale.pipeline.sessionsfinalizer.repositories.RabbitRepository

class Injector extends AbstractModule with ScalaModule with LazyLogging {
  override def configure(): Unit = logger.debug("injector configured")

  @Provides
  @Singleton def getActorSystem: ActorSystem = ActorSystem("sefer")

  @Provides
  @Singleton def getMaterializer(implicit @Inject system: ActorSystem) = ActorMaterializer()

  @Provides
  @Singleton def getConfig: Config = {
    val appConf = "app.conf"
    val logFile = "./logback.xml"
    val logSettings = "logback.configurationFile"

    Try(Paths.get(logFile)).map(i => System.setProperty(logSettings, logFile))
    Try(ConfigFactory.load(s"./$appConf")).getOrElse(ConfigFactory.load(appConf))
  }

  @Provides
  @Singleton def getEngueueHandler(@Inject config: Config): EnqueueHandler = {
    val repository = RabbitRepository.create(config)
    repository.publish
  }

  @Provides
  @Singleton def getRouter(@Inject config: Config): RoutingService = {
    new {} with RoutingService {
      override def getAddress =
        NetworkAddress(config.getInt("conf.port"), config.getString("conf.host"))
    }
  }

  @Provides
  @Singleton def getFinalizer(@Inject config: Config,
                              @Inject enqueueHandler: EnqueueHandler): FinalizerService = {

    new {} with FinalizerService {
      def getRequeueIntervalMs: Int = config.getInt("conf.requeueIntervalMs")
      def enqueue(session: Session): Unit = enqueueHandler
      def loadExpiredSessionsBatch(): Seq[Session] = List()
      def requeueRequired(session: Session): Boolean = false
      def publishMetrics(sessions: Seq[Session], skipped: Seq[Session]): Unit = {}
    }
  }

  @Provides
  @Singleton def getScheduler(@Inject system: ActorSystem,
                              @Inject finalizer: FinalizerService): ActorRef = {
    system.actorOf(Props(new ActorScheduler(finalizer)))
  }
}

object Injector {
  type EnqueueHandler = (Session) => Unit
}
