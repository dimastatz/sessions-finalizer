package com.clicktale.pipeline.sessionsfinalizer.contracts

import scala.util._
import java.time.LocalDateTime
import com.typesafe.scalalogging.LazyLogging
import com.clicktale.pipeline.sessionsfinalizer.contracts.FinalizerService._

trait FinalizerService extends LazyLogging {
  def getRequeueIntervalMs: Int
  def enqueue(session: Session): Unit
  def publishMetrics(metrics: Metrics): Unit
  def loadExpiredSessionsBatch(): Seq[Try[Session]]
  def requeueRequired(session: Session): Boolean

  def runRequeue(): Unit = {
    val requeueResult = Try({
      val batch = loadExpiredSessionsBatch()
      val requeueSet = batch.filter(i => i.isSuccess && requeueRequired(i.get))
      requeueSet.filter(_.isSuccess).map(_.get).toParArray.foreach(enqueue)
      publishMetrics(createMetrics(batch, requeueSet))
    })

    requeueResult match {
      case Success(x) => logger.debug(s"requeue performed: $x")
      case Failure(x) => logger.error(s"failed to requeue $x")
    }
  }

  def logFailedSessions(sessions: Seq[Try[Session]]): Seq[Session] = {
    logger.error(s"deserialize failed ${sessions.filter(_.isFailure).mkString(",")}")
    sessions.filter(_.isSuccess).map(_.get)
  }
}

object FinalizerService {
  private val epoch = LocalDateTime.of(2015, 1, 1, 0, 0, 0)
  case class Session(subsId: Int, pid: Int, sid: Long)
  case class Metrics(audits: Audits, failed: Boolean, unprocessedCount: Int)
  case class Audits(skipped: Seq[Session], processed: Seq[Session], failed: Seq[Session])

  def getSessionCreateDate(sid: Long): LocalDateTime = {
    val milliseconds = sid >> 14
    epoch.plusSeconds(milliseconds/1000)
  }

  def createMetrics(batch: Seq[Try[Session]], processed: Seq[Try[Session]]): Metrics = {
    val audits = Audits(List(),List(),List())
    Metrics(audits, false, 0)
  }
}