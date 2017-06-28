package com.clicktale.pipeline.sessionsfinalizer.contracts

import scala.util._
import com.typesafe.scalalogging.LazyLogging
import com.clicktale.pipeline.sessionsfinalizer.contracts.FinalizerService._

trait FinalizerService extends LazyLogging {
  def getRequeueIntervalMs: Int
  def enqueue(session: Session): Unit
  def loadExpiredSessionsBatch(): Seq[Session]
  def requeueRequired(session: Session): Boolean
  def publishMetrics(sessions: Seq[Session], skipped: Seq[Session]): Unit

  def runRequeue(session: Session): Unit = {
    logger.debug(s"performing requeue")

    val requeueResult = Try(() => {
      val sessions = loadExpiredSessionsBatch()
      val requeueSet = sessions.filter(requeueRequired)
      requeueSet.toParArray.foreach(enqueue)
      publishMetrics(sessions, sessions.diff(requeueSet))
    })

    requeueResult match {
      case Success(x) => logger.debug(s"requeue is finished")
      case Failure(x) => logger.error(s"failed to requeue $x")
    }
  }
}

object FinalizerService {
  case class Session(subsId: Int, pid: Int, sid: Int)
}