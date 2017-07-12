package com.clicktale.pipeline.sessionsfinalizer

import java.io._
import scala.util._
import java.nio.file._
import scala.concurrent._
import com.google.inject._
import java.util.concurrent._
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
  private final val appConf = "app.conf"

  override def configure(): Unit = {
    val logFile = new File("./logback.xml")
    if (logFile.exists) System.setProperty("logback.configurationFile", logFile.getCanonicalPath)
    logger.info(s"logback loaded: ${logFile.getCanonicalPath} ${logFile.exists}")
  }

  @Provides
  @Singleton def getActorSystem: ActorSystem = {
    ActorSystem("sefer")
  }

  @Provides
  @Singleton def getMaterializer(implicit @Inject system: ActorSystem): ActorMaterializer = {
    ActorMaterializer()
  }

  @Provides
  @Singleton def getConfig: Config = {
    val logFile = new File("./app.conf")
    logger.info(s"config loaded: ${logFile.getCanonicalPath} ${logFile.exists}")
    if (logFile.exists) ConfigFactory.parseFile(logFile) else ConfigFactory.load("app.conf")
  }

  @Provides
  @Singleton def getIoExecutor(@Inject config: Config): ExecutionContext = {
    ExecutionContext.fromExecutor(Executors.
      newFixedThreadPool(config.getInt("conf.ioThreadPoolSize")))
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
    (s: Seq[Session]) => aerospike.exists(s)
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
      def publishMetrics(metrics: Metrics): Unit = sendMetricsHandler(metrics)
      def loadExpiredSessionsBatch(): Seq[Try[Session]] = loadSessionsHandler()
      def enqueue(sessions: Seq[Session]): Seq[Try[Unit]] = enqueueHandler(sessions)
      def requeueRequired(session: Seq[Session]): Try[Seq[Session]] = requeueRequiredHandler(session)
    }
  }

  @Provides
  @Singleton def getScheduler(@Inject system: ActorSystem,
                              @Inject finalizer: FinalizerService,
                              @Inject executionContext: ExecutionContext): ActorRef = {
    system.actorOf(Props(new ActorScheduler(finalizer, executionContext)))
  }

}

object Injector {
  type SendMetricsHandler = Metrics => Unit
  type LoadSessionsHandler = () => Seq[Try[Session]]
  type EnqueueHandler = Seq[Session] => Seq[Try[Unit]]
  type RequeueRequiredHandler = Seq[Session] => Try[Seq[Session]]
}
