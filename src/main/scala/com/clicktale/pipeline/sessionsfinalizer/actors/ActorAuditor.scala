package com.clicktale.pipeline.sessionsfinalizer.actors

import java.time._
import akka.actor.Actor
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.LazyLogging
import com.clicktale.pipeline.sessionsfinalizer.actors.ActorAuditor._
import com.clicktale.pipeline.sessionsfinalizer.contracts.FinalizerService._

class ActorAuditor extends Actor with LazyLogging{
  private val auditsLog = LoggerFactory.getLogger("audits")
  private val metricsLog = LoggerFactory.getLogger("metrics")

  def receive: Receive = {
    case x: Metrics => processMetrics(x)
    case _ => logger.error("ActorAuditor received unknown message")
  }

  private def processMetrics(x: Metrics) = {
    val summary = toSummaryList(x.audits.failed, failedState) :::
        toSummaryList(x.audits.skipped, skippedState) :::
        toSummaryList(x.audits.processed, processedState)

    writeAudit(summary)

    summary
      .groupBy(i => (i.subsId, i.pid))
      .foreach(i => writeMetrics(i._1, i._2))

    // special metric for overall performance
    metricsLog.info(s"subsId:0 pid:0 total:${x.unprocessedCount} " +
      s"timestamp:${ZonedDateTime.now(utc)} failed:0 skipped:0 processed:${summary.length}")
  }

  private def writeMetrics(key: (Int, Int), summary: List[ProcessingSummary]) = {
    val total = summary.length
    val failed = summary.count(x => x.state == failedState)
    val skipped = summary.count(x => x.state == skippedState)
    val processed = summary.count(x => x.state == processedState)

    metricsLog.info(s"subsId:${key._1} pid:${key._2} timestamp:${ZonedDateTime.now(utc)}" +
      s"total:$total failed:$failed skipped:$skipped processed:$processed")
  }

  private def writeAudit(summary: List[ProcessingSummary]) = {
    summary.foreach(x => auditsLog.info(
      s"subsid:${x.subsId} pid:${x.pid} timestamp:${x.timeStamp} data:${x.state}"))
  }
}

object ActorAuditor {
  final val failedState = "failed"
  final val skippedState = "skipped"
  final val processedState = "processed"
  final val utc: ZoneId = ZoneId.of("UTC")

  case class ProcessingSummary(pid: Int,
                               sid: Long,
                               subsId: Int,
                               state: String,
                               timeStamp: ZonedDateTime)

  def toSummaryList(x: Seq[Session], state: String): List[ProcessingSummary] = {
    x.map(i => toSummary(i, state)).toList
  }

  def toSummary(x: Session, state: String): ProcessingSummary = {
    ProcessingSummary(x.pid, x.sid, x.subsId, state, ZonedDateTime.now(utc))
  }

}

