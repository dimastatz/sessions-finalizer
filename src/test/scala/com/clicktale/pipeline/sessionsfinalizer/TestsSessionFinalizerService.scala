package com.clicktale.pipeline.sessionsfinalizer

import scala.util.Try
import com.clicktale.pipeline.sessionsfinalizer.contracts.FinalizerService
import org.scalatest.{Succeeded, WordSpecLike}
import com.clicktale.pipeline.sessionsfinalizer.repositories._
import com.clicktale.pipeline.sessionsfinalizer.contracts.FinalizerService.{Metrics, Session}

class TestsSessionFinalizerService extends WordSpecLike {
  private val rabbit = RabbitRepository.create(TestUtils.config)
  private val kafka = KafkaSessionsRepository.create(TestUtils.config)
  private val aerospike = AerospikeSessionsRepository.create(TestUtils.config)
  //private sessionFinalizer = initialize()


  "SessionFinalizer" must {
    "Produce item" in {
      if (!TestUtils.isDevMachine) Succeeded


    }
  }
}
