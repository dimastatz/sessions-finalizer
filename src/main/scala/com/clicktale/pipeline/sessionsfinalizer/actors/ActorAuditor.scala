package com.clicktale.pipeline.sessionsfinalizer.actors

import java.time._
import scala.util._
import akka.actor.Actor
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging.LazyLogging
import com.clicktale.pipeline.sessionsfinalizer.actors.ActorAuditor._
import com.clicktale.pipeline.sessionsfinalizer.contracts.FinalizerService._

class ActorAuditor extends Actor with LazyLogging{
  private val auditsLog = LoggerFactory.getLogger("audits")
  private val metricsLog = LoggerFactory.getLogger("metrics")

  def receive: Receive = {
    case x: Metrics => handleMetrics(x)
    case _ => logger.error("ActorAuditor received unknown message")
  }

  private def handleMetrics(x: Metrics) = {
    Try(writeMetrics(x)) match {
      case Success(i) =>
      case Failure(i) => logger.error(s"Failed to handle metrics $i")
    }
  }

  private def writeMetrics(x: Metrics) = {
    val summary = toSummaryList(x.audits.failed, failedState) :::
        toSummaryList(x.audits.skipped, skippedState) :::
        toSummaryList(x.audits.processed, processedState)

    // write overall metrics
    formatOverallMetrics(x)
    // write audits
    summary.foreach(formatAudit)
    // write metrics by subsID/pid
    summary.groupBy(i => (i.subsId, i.pid)).foreach(i => formatMetrics(i._1, i._2))
  }

  private def formatMetrics(key: (Int, Int), summary: List[ProcessingSummary]) = {
    val total = summary.length
    val failed = summary.count(x => x.state == failedState)
    val skipped = summary.count(x => x.state == skippedState)
    val processed = summary.count(x => x.state == processedState)

    metricsLog.info(s"subsid:${key._1} pid:${key._2} " +
      s"timestamp:${LocalDateTime.now(ZoneId.of("UTC"))} " +
      s"total:$total failed:$failed skipped:$skipped processed:$processed")
  }

  private def formatOverallMetrics(metrics: Metrics) = {
    metricsLog.info(s"subsid:0 pid:0 loadBatchTime:${metrics.time.loadBatch} " +
      s"timestamp:${LocalDateTime.now(ZoneId.of("UTC"))} " +
      s"requeueRequiredTime:${metrics.time.requeueRequired} enqueueTime:${metrics.time.enqueue}")
  }

  private def formatAudit(audit: ProcessingSummary) = {
    auditsLog.info(s"subsid:${audit.subsId} pid:${audit.pid} timestamp:${audit.timeStamp} data:${audit.state}")
  }
}

object ActorAuditor {
  final val failedState = "failed"
  final val skippedState = "skipped"
  final val processedState = "processed"

  case class ProcessingSummary(pid: Int,
                               sid: Long,
                               subsId: Int,
                               state: String,
                               timeStamp: LocalDateTime)

  def toSummaryList(x: Seq[Session], state: String): List[ProcessingSummary] = {
    x.map(i => toSummary(i, state)).toList
  }

  def toSummary(x: Session, state: String): ProcessingSummary = {
    ProcessingSummary(x.pid, x.sid, x.subsId, state, LocalDateTime.now(ZoneId.of("UTC")))
  }

}

