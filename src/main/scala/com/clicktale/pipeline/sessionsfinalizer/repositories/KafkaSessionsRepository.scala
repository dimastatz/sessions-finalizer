package com.clicktale.pipeline.sessionsfinalizer.repositories

import java.util._
import scala.util._
import com.google.gson._
import com.typesafe.config._
import collection.JavaConverters._
import org.apache.kafka.clients.producer._
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.clients.CommonClientConfigs
import com.clicktale.pipeline.sessionsfinalizer.contracts.FinalizerService.Session
import com.clicktale.pipeline.sessionsfinalizer.repositories.KafkaSessionsRepository._

class KafkaSessionsRepository(config: KafkaConfig) {
  private final val topic = config.topics
  private val consumer = createConsumer()
  private val producer = createProducer()

  def loadExpiredSessionsBatch(): Seq[Try[Session]] = {
    val records = consumer.poll(1000)
    if(!records.isEmpty) consumer.commitAsync()
    records.asScala.map(getSession).toSeq
  }

  def publishSessionData(session: Session): Unit = {
    val data = serializer.toJson(session)
    val record = new ProducerRecord[String, String](topic, session.sid.toString, data)
    producer.send(record)
  }

  private def createProducer(): KafkaProducer[String, String] = {
    val props = new java.util.Properties()
    props.put("bootstrap.servers", config.brokers)
    props.put("client.id", config.clientId)
    props.put("enable.auto.commit", config.autoCommit.toString)
    props.put("auto.offset.reset", config.offsetReset)
    props.put("compression.type", config.compressionType)
    props.put("key.serializer", config.keySerializer)
    props.put("value.serializer", config.valueSerializer)

    if(config.securityEnabled) defineSSL(props)
    new KafkaProducer[String, String](props)
  }

  private def createConsumer() = {
    val props = new java.util.Properties()
    props.put("bootstrap.servers", config.brokers)
    props.put("group.id", config.groupId)
    props.put("client.id", config.clientId)
    props.put("enable.auto.commit", config.autoCommit.toString)
    props.put("auto.offset.reset", config.offsetReset)
    props.put("key.deserializer", config.keyDeserializer)
    props.put("value.deserializer", config.valueDeserializer)
    props.put("compression.type", config.compressionType)
    props.put("max.poll.records", config.maxPollSize.toString)
    
    if(config.securityEnabled) defineSSL(props)
    val consumer = new KafkaConsumer[String, String](props)
    val topicsCollection = config.topics.split(",").toSeq.asJavaCollection
    consumer.subscribe(topicsCollection)
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
  val serializer: Gson = new GsonBuilder().create()
  private def getClientId = UUID.randomUUID().toString

  case class KafkaConfig(topics: String,
                         brokers: String,
                         groupId: String,
                         clientId: String,
                         maxPollSize: Int,
                         autoCommit: Boolean,
                         offsetReset: String,
                         keySerializer: String,
                         valueSerializer: String,
                         keyDeserializer: String,
                         valueDeserializer: String,
                         compressionType: String,
                         securityEnabled: Boolean,
                         sslPassword: String,
                         sslEncryptionFile: String)



  def createKafkaConfig(config: Config): KafkaConfig = {
    KafkaConfig(
      config.getString("conf.kafka.topics"),
      config.getString("conf.kafka.brokers"),
      config.getString("conf.kafka.groupId"),
      getClientId,
      config.getInt("conf.kafka.maxpollrecords"),
      config.getBoolean("conf.kafka.autoCommit"),
      config.getString("conf.kafka.offsetReset"),
      config.getString("conf.kafka.keySerializer"),
      config.getString("conf.kafka.valueSerializer"),
      config.getString("conf.kafka.keyDeserializer"),
      config.getString("conf.kafka.valueDeserializer"),
      config.getString("conf.kafka.compression"),
      config.getBoolean("conf.kafka.useSSL"),
      config.getString("conf.kafka.password"),
      config.getString("conf.kafka.encryptionFile"))
  }

  def create(config: Config): KafkaSessionsRepository = {
    new KafkaSessionsRepository(createKafkaConfig(config))
  }

  def getSession(data: ConsumerRecord[String, String]): Try[Session] = {
    Try(serializer.fromJson(data.value, classOf[Session])) match {
      case Success(x) => Success(x)
      case Failure(x) => Failure(new Exception(data.key + "-" + data.value, x))
    }
  }
}
