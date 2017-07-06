package com.clicktale.pipeline.sessionsfinalizer

import java.time._
import org.scalatest._
import com.typesafe.config._
import com.clicktale.pipeline.sessionsfinalizer.repositories._
import com.clicktale.pipeline.sessionsfinalizer.contracts.FinalizerService._

class TestsKafkaRepository extends WordSpecLike {
  private final val utc: ZoneId = ZoneId.of("UTC")
  private val repository = KafkaSessionsRepository.create(TestUtils.config)

  "Kafka repository" must {
    "Produce item" in {
      if (TestUtils.isDevMachine) {
        Range(0, 30).foreach(i =>{
          val session = Session(i%3, i%3, TestUtils.getSid)
          repository.publishSessionData(session)
        })
        Thread.sleep(1000)
        succeed
      }
      else succeed
    }
    "Consume item" in {
      if (TestUtils.isDevMachine) {
        while(true) {
          val batch = repository.loadExpiredSessionsBatch()
          val a = repository.getOffsetData
          println(batch.length)
          a.foreach(println)
        }
      }
      succeed
    }
  }
}
