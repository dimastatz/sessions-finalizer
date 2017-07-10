package com.clicktale.pipeline.sessionsfinalizer


import scala.util._
import org.scalatest._
import com.clicktale.pipeline.sessionsfinalizer.repositories._
import com.clicktale.pipeline.sessionsfinalizer.contracts.FinalizerService
import com.clicktale.pipeline.sessionsfinalizer.contracts.FinalizerService._

class TestsSessionFinalizerService extends WordSpecLike {
  "SessionFinalizer" must {
    "Produce item" in {
      if (!TestUtils.isDevMachine) Succeeded

      val sidsTotalCount = 100
      val sidsToRequeueCount = 50
      val rabbit = RabbitRepository.create(TestUtils.config)
      val kafka = KafkaSessionsRepository.create(TestUtils.config)
      val aerospike = AerospikeSessionsRepository.create(TestUtils.config)
      val sessionFinalizer: FinalizerService = new {} with FinalizerService {
        def getRequeueIntervalMs: Int = TestUtils.config.getInt("conf.requeueIntervalMs")
        def publishMetrics(metrics: Metrics): Unit = println(metrics)
        def enqueue(sessions: Seq[Session]): Seq[Try[Unit]] = rabbit.publish(sessions)
        def loadExpiredSessionsBatch(): Seq[Try[Session]] = kafka.loadExpiredSessionsBatch()
        def requeueRequired(sessions: Seq[Session]): Try[Seq[Session]] = aerospike.exists(sessions)
      }

      // create expired sids
      val sessionsToPublish = Range(0, sidsTotalCount)
        .map(i => (i % 3) + 1)
        .map(i => Session(i, i, TestUtils.getSid(31)))

      sessionsToPublish.foreach(kafka.publishSessionData)
      // add to aerospike
      val sessionsToRequeue = sessionsToPublish.take(sidsToRequeueCount)
      sessionsToRequeue.foreach(i => aerospike.write(i.sid, TestUtils.toString(i).getBytes, 1000))

      for (i <- 1 to 3) {
        Thread.sleep(1000)
        sessionFinalizer.requeue(true)
      }
    }
  }
}

