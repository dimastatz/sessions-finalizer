package com.clicktale.pipeline.sessionsfinalizer.repositories

import com.typesafe.config._
import collection.JavaConverters._
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.clients.CommonClientConfigs
import com.clicktale.pipeline.sessionsfinalizer.repositories.KafkaSessionsRepository._
import com.clicktale.pipeline.sessionsfinalizer.contracts.SessionsFinalizerService.Session

class KafkaSessionsRepository(config: KafkaConfig) {
  private val consumer = createConsumer()

  def loadExpiredSessionsBatch(): Seq[Session] = {
    val records = consumer.poll(1000)
    consumer.commitAsync()
    records.asScala.map(i => Session(0,0,0)).toSeq
  }

  private def createConsumer() = {
    val props = new java.util.Properties()
    props.put("bootstrap.servers", config.brokers)
    props.put("group.id", config.groupId)
    props.put("client.id", config.clientId)
    props.put("enable.auto.commit", config.autoCommit.toString)
    props.put("auto.offset.reset", config.offsetReset)
    props.put("key.deserializer", config.keySerializer)
    props.put("value.deserializer", config.valueSerializer)
    props.put("compression.type", config.compressionType)
    props.put("max.poll.records", config.maxPollSize.toString)
    
    if(config.securityEnabled) defineSSL(props)
    val consumer = new KafkaConsumer[String, String](props)
    consumer.subscribe(config.topics.split(",").toList.asJavaCollection)
    consumer
  }

  private def defineSSL(props: java.util.Properties) = {
    props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL")
    props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG,  config.sslPassword)
    props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, config.sslEncryptionFile)
    props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, config.sslPassword)
    props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, config.sslPassword)
    props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, config.sslEncryptionFile)
  }
}

object KafkaSessionsRepository {
  case class KafkaConfig(topics: String,
                         brokers: String,
                         groupId: String,
                         clientId: String,
                         maxPollSize: Int,
                         autoCommit: Boolean,
                         offsetReset: String,
                         keySerializer: String,
                         valueSerializer: String,
                         compressionType: String,
                         securityEnabled: Boolean,
                         sslPassword: String,
                         sslEncryptionFile: String)

  def createKafkaConfig(config: Config): KafkaConfig = {
    KafkaConfig(
      config.getString("conf.kafka.topics"),
      config.getString("conf.kafka.brokers"),
      config.getString("conf.kafka.groupId"),
      java.util.UUID.randomUUID().toString,
      config.getInt("conf.kafka.maxpollrecords"),
      config.getBoolean("conf.kafka.autoCommit"),
      config.getString("conf.kafka.offsetReset"),
      config.getString("conf.kafka.keySerializer"),
        config.getString("conf.kafka.offsetReset"),
      "",
      false,
      "",
      "")
  }
}
