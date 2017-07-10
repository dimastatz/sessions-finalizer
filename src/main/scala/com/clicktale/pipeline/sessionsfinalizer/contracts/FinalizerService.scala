package com.clicktale.pipeline.sessionsfinalizer.contracts

import scala.util._
import java.time.LocalDateTime
import com.typesafe.scalalogging.LazyLogging
import com.clicktale.pipeline.sessionsfinalizer.contracts.FinalizerService._

trait FinalizerService extends LazyLogging {
  def getRequeueIntervalMs: Int
  def publishMetrics(metrics: Metrics): Unit
  def loadExpiredSessionsBatch(): Seq[Try[Session]]
  def enqueue(session: Seq[Session]): Seq[Try[Unit]]
  def requeueRequired(session: Seq[Session]): Try[Seq[Session]]

  def requeue(sendMetrics: Boolean): Unit = {
    Try(requeue()) match {
      case Success(x) => {
        publishMetrics(x)
        logger.info(s"FinalizerService re-queued: $x")
      }
      case Failure(x) => {
        logger.error(s"FinalizerService failed to requeue $x")
      }
    }
  }

  def requeue(): Metrics = {
    val batchResult = measure(() => loadExpiredSessionsBatch())
    logger.info(s"FinalizerService.loadExpired ${batchResult.data}")

    val sessions = filterFailed(batchResult.data)
    val requeueRequiredResult = measure(() => requeueRequired(sessions))
    logger.info(s"FinalizerService.requeueRequired ${requeueRequiredResult.data}")

    val enqueueResult = measure(() => enqueue(requeueRequiredResult.data.get))
    logger.info(s"FinalizerService.enqueue ${enqueueResult.data}")

    val audit = Audits(requeueRequiredResult.data.get, sessions, List())
    Metrics(audit, failed = false, 0)
  }

  def measure[A](call: () => A): Result[A] = {
    val start = System.nanoTime()
    Result(call(), (System.nanoTime() - start) / 1000000)
  }

  def filterFailed[A](data: Seq[Try[A]]): Seq[A] = {
    val failed = data.filter(_.isFailure)
    if(failed.nonEmpty) logger.warn(s"failed ${failed.mkString(",")}")
    data.filter(_.isSuccess).map(_.get)
  }
}

object FinalizerService {
  private val epoch = LocalDateTime.of(2015, 1, 1, 0, 0, 0)
  case class Result[A](data: A, time: Long)
  case class Session(subsId: Int, pid: Int, sid: Long)
  case class Metrics(audits: Audits, failed: Boolean, unprocessedCount: Int)
  case class Audits(skipped: Seq[Session], processed: Seq[Session], failed: Seq[Session])

  def getSessionCreateDate(sid: Long): LocalDateTime = {
    val milliseconds = sid >> 14
    epoch.plusSeconds(milliseconds/1000)
  }
}