package com.clicktale.pipeline.sessionsfinalizer.contracts

import scala.util._
import com.typesafe.scalalogging.LazyLogging
import com.clicktale.pipeline.sessionsfinalizer.contracts.SessionsFinalizerService._

trait SessionsFinalizerService extends LazyLogging {
  def getRequeueIntervalMs: Int
  def enqueue(session: Session): Unit
  def loadExpiredSessionsBatch(): Seq[Session]
  def requeueRequired(session: Session): Boolean
  def publishMetrics(sessions: Seq[Session], skipped: Seq[Session]): Unit

  def runRequeue(session: Session): Unit = {
    logger.debug(s"performing requeue")
    Try(SessionsFinalizerService.runRequeue(this)) match {
      case Success(x) => logger.debug(s"requeue is finished")
      case Failure(x) => logger.error(s"failed to requeue $x")
    }
  }
}

object SessionsFinalizerService {
  case class Session(subsId: Int, pid: Int, sid: Int)

  def getDefault: SessionsFinalizerService = {
    null
  }

  def runRequeue(service: SessionsFinalizerService): Unit ={
    val sessions = service.loadExpiredSessionsBatch()
    val requeueSet = sessions.filter(service.requeueRequired)
    requeueSet.toParArray.foreach(service.enqueue)
    service.publishMetrics(sessions, sessions.diff(requeueSet))
  }
}