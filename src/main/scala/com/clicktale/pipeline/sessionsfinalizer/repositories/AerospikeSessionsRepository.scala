package com.clicktale.pipeline.sessionsfinalizer.repositories

import scala.util._
import com.typesafe.config._
import com.aerospike.client._
import com.aerospike.client.async._
import com.aerospike.client.policy._
import com.clicktale.pipeline.sessionsfinalizer.repositories.AerospikeSessionsRepository._

class AerospikeSessionsRepository(config: AerospikeConfig) {
  val client: AsyncClient = initializeClient()

  def read(sid: Long): Try[Array[Byte]] = Try({
    val key = new Key(config.namespace, config.setName, sid)
    val record = client.get(client.readPolicyDefault, key)
    val data = record.getValue(config.binName)
    data.asInstanceOf[Array[Byte]]
  }) match {
    case Success(x) => Success(x)
    case Failure(x) => Failure(new Exception(s"read failed: $sid", x))
  }

  def write(sid: Long, events: Array[Byte], ttlSec: Int): Try[Unit] = Try({
    val bin = new Bin(config.binName, events)
    val key = new Key(config.namespace, config.setName, sid)
    client.put(getWritePolicy(ttlSec), key, bin)
  }) match {
    case Success(x) => Success(x)
    case Failure(x) => Failure(new Exception(s"write failed: $sid", x))
  }

  def delete(sid: Long): Try[Unit] = Try({
    val key = new Key(config.namespace, config.setName, sid)
    client.delete(client.writePolicyDefault, key)
  }) match {
    case Success(x) => Success(x)
    case Failure(x) => Failure(new Exception(s"delete failed: $sid", x))
  }

  private def getWritePolicy(timeToLiveSec: Int) = {
    val policy = new WritePolicy
    policy.maxRetries = 0
    policy.timeout = config.timeoutSingleMs
    policy.expiration = timeToLiveSec
    policy
  }

  private def initializeClient(): AsyncClient = {
    val asyncPolicy = new AsyncClientPolicy
    asyncPolicy.timeout = config.timeoutSingleMs
    asyncPolicy.readPolicyDefault.timeout = config.timeoutSingleMs
    asyncPolicy.writePolicyDefault.timeout = config.timeoutSingleMs
    asyncPolicy.readPolicyDefault.maxRetries = config.maxRetries
    asyncPolicy.writePolicyDefault.maxRetries = config.maxRetries
    asyncPolicy.asyncMaxCommands = config.maxConcurrentCommands
    asyncPolicy.batchPolicyDefault.timeout =  config.timeoutBatchMs

    val hosts = config.nodes.split(",").map(i => new Host(i, config.port))
    new AsyncClient(new AsyncClientPolicy(), hosts: _*)
  }
}

object AerospikeSessionsRepository {
  case class AerospikeConfig(port: Int,
                             nodes: String,
                             setName: String,
                             binName: String,
                             namespace: String,
                             maxRetries: Int,
                             timeoutBatchMs: Int,
                             timeoutSingleMs: Int,
                             maxConcurrentCommands: Int)

  def createAerospikeConfig(config: Config): AerospikeConfig = {
    AerospikeConfig(
      config.getInt("conf.aerospike.port"),
      config.getString("conf.aerospike.nodes"),
      config.getString("conf.aerospike.setName"),
      config.getString("conf.aerospike.binName"),
      config.getString("conf.aerospike.namespace"),
      config.getInt("conf.aerospike.maxRetries"),
      config.getInt("conf.aerospike.timeoutBatchMs"),
      config.getInt("conf.aerospike.timeoutSingleMs"),
      config.getInt("conf.aerospike.maxConcurrentCommands"))
  }

  def create(config: Config): AerospikeSessionsRepository = {
    new AerospikeSessionsRepository(createAerospikeConfig(config))
  }
}