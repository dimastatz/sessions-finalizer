package com.clicktale.pipeline.sessionsfinalizer

import scala.util._
import com.typesafe.scalalogging._
import com.clicktale.pipeline.sessionsfinalizer.Controller._

class Controller(service: SessionsFinalizerService) extends LazyLogging {
  def runRequeue(): Unit = {
    logger.debug(s"performing requeue")
    Try(requeue()) match {
      case Success(x) => logger.debug(s"requeue is finished")
      case Failure(x) => logger.error(s"failed to requeue $x")
    }
  }

  private def requeue(): Unit = {
    val sessions = service.loadExpiredSessionsBatch()
    val requeueSet = sessions.filter(service.requeueRequired)
    requeueSet.toParArray.foreach(service.requeue)
    service.publishMetrics(sessions, sessions.diff(requeueSet))
  }
}

object Controller {
  case class Session(subsId: Int, pid: Int, sid: Int)

  trait SessionsFinalizerService {
    def requeue(session: Session): Unit
    def requeueRequired(session: Session): Boolean
    def loadExpiredSessionsBatch(): Seq[Session]
    def publishMetrics(sessions: Seq[Session], skipped: Seq[Session]): Unit
  }
}
