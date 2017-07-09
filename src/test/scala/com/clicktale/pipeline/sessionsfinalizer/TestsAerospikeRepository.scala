package com.clicktale.pipeline.sessionsfinalizer

import com.clicktale.pipeline.sessionsfinalizer.contracts.FinalizerService.Session
import org.scalatest.WordSpecLike
import com.clicktale.pipeline.sessionsfinalizer.repositories.AerospikeSessionsRepository

class TestsAerospikeRepository extends WordSpecLike{
  private val sidsCount = 10
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
      var sids = Range(0, sidsCount).map(i => TestUtils.getSid)
      sids.toParArray.foreach(i => repository.write(i, data, 60))
      val extendedList = sids.toList ::: List(0L, 1L)
      Thread.sleep(200)
      val out = repository.readBatch(extendedList.toArray)
      assert(out.get.filter(_._2.length == 0).toArray.length == 2)
      assert(out.get.filter(_._2.length > 0).toArray.length == sidsCount)
    }
    "Exists in aerospike" in {
      val data = "data".getBytes
      var sids = Range(0, sidsCount).map(i => TestUtils.getSid)
      sids.toParArray.foreach(i => repository.write(i, data, 60))
      val extendedList = sids.toList ::: List(0L, 1L)
      Thread.sleep(200)
      val existing = repository.exists(extendedList.map(i => Session(1,1,i)))
      assert(existing.get.length == sidsCount)
    }

  }
}
