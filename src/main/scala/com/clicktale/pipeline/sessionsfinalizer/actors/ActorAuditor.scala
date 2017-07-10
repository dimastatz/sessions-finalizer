package com.clicktale.pipeline.sessionsfinalizer.actors

import java.time._
import scala.util._
import akka.actor.Actor
import com.google.gson._
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
    metricsLog.info(serializer.toJson(MetricsFormat(
      key._1,
      key._2,
      summary.length,
      summary.count(x => x.state == processedState),
      summary.count(x => x.state == skippedState),
      summary.count(x => x.state == failedState))))
  }

  private def formatOverallMetrics(metrics: Metrics) = {
    val format = OverallMetricsFormat(
      metrics.time.enqueue,
      metrics.time.loadBatch,
      metrics.time.enqueue)

    metricsLog.info(serializer.toJson(format))
  }

  private def formatAudit(audit: ProcessingSummary) = auditsLog.info(serializer.toJson(audit))
}

object ActorAuditor {
  final val failedState = "failed"
  final val skippedState = "skipped"
  final val processedState = "processed"
  val serializer: Gson = new GsonBuilder().create()

  case class ProcessingSummary(pid: Int,
                               sid: Long,
                               subsId: Int,
                               state: String,
                               timeStamp: String)

  case class OverallMetricsFormat(enqueueTime: Long,
                                  loadBatchTime: Long,
                                  requeueRequiredTime: Long,
                                  pid: Int = 0,
                                  subsId: Int = 0,
                                  timeStamp: String = LocalDateTime.now(ZoneId.of("UTC")).toString)

  case class MetricsFormat(subsId: Long,
                           pid: Long,
                           total: Int,
                           processed: Int,
                           skipped: Int,
                           failed: Int,
                           timeStamp: String = LocalDateTime.now(ZoneId.of("UTC")).toString)

  def toSummaryList(x: Seq[Session], state: String): List[ProcessingSummary] = {
    x.map(i => toSummary(i, state)).toList
  }

  def toSummary(x: Session, state: String): ProcessingSummary = {
    ProcessingSummary(x.pid, x.sid, x.subsId, state, LocalDateTime.now(ZoneId.of("UTC")).toString)
  }
}

