package com.clicktale.pipeline.sessionsfinalizer.actors

import akka.actor._
import scala.concurrent._
import com.typesafe.scalalogging._
import scala.concurrent.duration._
import com.clicktale.pipeline.sessionsfinalizer.actors.ActorScheduler._
import com.clicktale.pipeline.sessionsfinalizer.contracts.SessionsFinalizerService

class ActorScheduler(finalizer: SessionsFinalizerService) extends Actor with LazyLogging{
  private implicit val executor = ExecutionContext.Implicits.global
  val cancellable: Cancellable = startTimer()

  def receive: Receive = {
    case Tick => logger.debug("Tick")
    case _ => logger.error("ActorScheduler recieved unknown message")
  }

  private def startTimer() = {
    val interval = finalizer.getRequeueIntervalMs
    context.system.scheduler.schedule(interval milliseconds, interval milliseconds, self, Tick)
  }
}

object ActorScheduler {
  case object Tick
}