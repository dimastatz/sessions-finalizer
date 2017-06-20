package com.clicktale.pipeline.sessionsfinalizer

import scala.util._
import com.clicktale.pipeline.sessionsfinalizer.Controller._

class Controller(service: SessionsFinalizerService) {
  def runRequeue(): Unit = {
    Try(requeue()) match {
      case Success(x) =>
      case Failure(x) =>
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
