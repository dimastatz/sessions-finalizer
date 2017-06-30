package com.clicktale.pipeline.sessionsfinalizer.actors

import akka.actor.Actor
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.LazyLogging
import com.clicktale.pipeline.sessionsfinalizer.contracts.FinalizerService._

class ActorAuditor extends Actor with LazyLogging{
  private val auditsLog = LoggerFactory.getLogger("audits")
  private val metricsLog = LoggerFactory.getLogger("metrics")

  def receive: Receive = {
    case x: Metrics => processMetrics(x)
    case _ => logger.error("ActorAuditor received unknown message")
  }

  private def processMetrics(x: Metrics) = {
    auditsLog.info("audit")
    metricsLog.info("metrics")
  }
}

object ActorAuditor{

}

