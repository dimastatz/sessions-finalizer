package com.clicktale.pipeline.sessionsfinalizer

import org.scalatest.WordSpecLike
import com.clicktale.pipeline.sessionsfinalizer.repositories.AerospikeSessionsRepository


class TestsAerospikeRepository extends WordSpecLike{
  private val repository = AerospikeSessionsRepository.create(TestUtils.config)
  "Aerospike repository" must {
    "Create item" in {
      val sid = TestUtils.getSid
      val data = "data".getBytes
      repository.write(sid, data, 60)
      val out = repository.read(sid)
      assert(out.isSuccess)
    }
    "Read batch" in {
      val data = "data".getBytes
      var sids = Range(0, 100).map(i => TestUtils.getSid)
      sids.toParArray.foreach(i => repository.write(i, data, 60))
      val out = repository.readBatch(sids.toArray)
      assert(out.isSuccess)
    }
  }
}
