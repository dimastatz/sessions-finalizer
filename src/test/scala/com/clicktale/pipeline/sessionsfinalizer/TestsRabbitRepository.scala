package com.clicktale.pipeline.sessionsfinalizer

import org.scalatest._
import com.clicktale.pipeline.sessionsfinalizer.repositories._

class TestsRabbitRepository extends WordSpecLike {
  private val repository = RabbitRepository.create(TestUtils.config)

  "Rabbit Repository" must {
    "Produce item" in {
      if (!TestUtils.isDevMachine) Succeeded
      else {
        val sids = Range(0, 100).map(i => TestUtils.getSid)
        sids.foreach(i => repository.publish(1, 1, i))
      }
    }
  }
}
