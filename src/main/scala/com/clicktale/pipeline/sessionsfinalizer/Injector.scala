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
import com.clicktale.pipeline.sessionsfinalizer.repositories._
import com.clicktale.pipeline.sessionsfinalizer.contracts.RoutingService._
import com.clicktale.pipeline.sessionsfinalizer.contracts.FinalizerService._

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
  @Singleton def getEnqueueHandler(@Inject config: Config): EnqueueHandler = {
    val repository = RabbitRepository.create(config)
    repository.publish
  }

  @Provides
  @Singleton def getMetricsHandler(@Inject system: ActorSystem): SendMetricsHandler = {
    val auditor = system.actorOf(Props(new ActorAuditor()))
    (metrics: Metrics) => auditor ! metrics
  }


  @Provides
  @Singleton def getLoadSessionsHandler(@Inject config: Config): LoadSessionsHandler = {
    val kafka = KafkaSessionsRepository.create(config)
    kafka.loadExpiredSessionsBatch
  }

  @Provides
  @Singleton def getRequeueRequiredHandler(@Inject config: Config): RequeueRequiredHandler = {
    val aerospike = AerospikeSessionsRepository.create(config)
    (s:Session) => aerospike.read(s.sid).isSuccess
  }

  @Provides
  @Singleton def getRouter(@Inject config: Config): RoutingService = {
    val address = NetworkAddress(config.getInt("conf.port"), config.getString("conf.host"))

    new {} with RoutingService {
      override def getAddress: NetworkAddress = address
    }
  }

  @Provides
  @Singleton def getFinalizer(@Inject config: Config,
                              @Inject enqueueHandler: EnqueueHandler,
                              @Inject loadSessionsHandler: LoadSessionsHandler,
                              @Inject requeueRequiredHandler: RequeueRequiredHandler,
                              @Inject sendMetricsHandler: SendMetricsHandler): FinalizerService = {

    new {} with FinalizerService {
      def getRequeueIntervalMs: Int = config.getInt("conf.requeueIntervalMs")
      def enqueue(session: Session): Unit = enqueueHandler(session)
      def loadExpiredSessionsBatch(): Seq[Try[Session]] = loadSessionsHandler()
      def requeueRequired(session: Session): Boolean = requeueRequiredHandler(session)
      def publishMetrics(metrics: Metrics): Unit = sendMetricsHandler(metrics)
    }
  }

  @Provides
  @Singleton def getScheduler(@Inject system: ActorSystem,
                              @Inject finalizer: FinalizerService): ActorRef = {
    system.actorOf(Props(new ActorScheduler(finalizer)))
  }
}

object Injector {
  type EnqueueHandler = Session => Unit
  type SendMetricsHandler = Metrics => Unit
  type LoadSessionsHandler = () => Seq[Try[Session]]
  type RequeueRequiredHandler = Session => Boolean
}
