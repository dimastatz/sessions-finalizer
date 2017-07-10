package com.clicktale.pipeline.sessionsfinalizer.repositories

import java.time._
import scala.util._
import java.util.UUID
import com.google.gson._
import com.typesafe.config._
import collection.JavaConverters._
import org.apache.kafka.clients.producer._
import org.apache.kafka.clients.consumer._
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.clients.CommonClientConfigs
import com.clicktale.pipeline.sessionsfinalizer.contracts.FinalizerService._
import com.clicktale.pipeline.sessionsfinalizer.repositories.KafkaSessionsRepository._

class KafkaSessionsRepository(config: KafkaConfig) {
  private val consumer = createConsumer()
  private val producer = createProducer()

  // config
  def loadExpiredSessionsBatch(): Seq[Try[Session]] = {
    val records = consumer.poll(config.pollTimeout)
    val sessions = records.asScala.map(getSession).toSeq

    if(allExpired(sessions.filter(_.isSuccess).map(_.get).toList)) {
      if(sessions.exists(_.isSuccess))consumer.commitAsync()
      sessions
    }
    else {
      List()
    }
  }

  // return future
  def publishSessionData(session: Session): Unit = {
    val data = serializer.toJson(session)
    val record = new ProducerRecord[String, String](config.topics, session.sid.toString, data)
    producer.send(record)
  }

  def getOffsetData: Seq[Offset] = {
    val partitions = consumer.assignment()
    val eof = consumer.endOffsets(partitions).asScala.map(i => (i._1, i._2.toLong))
    val current = partitions.asScala.map(i => (i, consumer.committed(i).offset()))
    val list: List[(TopicPartition, Long)] = eof.toList ::: current.toList

    list.groupBy(i => i._1).map(i => createOffset(i._2)).toList
  }

  def createOffset(list: List[(TopicPartition, Long)]): Offset = {
    val ordered = list.sortBy(i => i._2)
    Offset(ordered.head._1.toString, ordered.head._2, ordered.last._2)
  }

  private def allExpired(x: Seq[Session]): Boolean = {
    Try(x.count(isExpired) == x.length) match{
      case Success(i) => i
      case Failure(i) => true
    }
  }

  private def isExpired(x: Session): Boolean = {
    val now = LocalDateTime.now(ZoneId.of("UTC"))
    val createDate = getSessionCreateDate(x.sid)
    Duration.between(createDate, now).toMinutes > config.expirationMins
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

  case class Offset(name: String, committed: Long, eof: Long)

  case class KafkaConfig(topics: String,
                         brokers: String,
                         groupId: String,
                         clientId: String,
                         maxPollSize: Int,
                         pollTimeout: Int,
                         autoCommit: Boolean,
                         offsetReset: String,
                         expirationMins: Int,
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
      config.getInt("conf.kafka.maxpolltimeout"),
      config.getBoolean("conf.kafka.autoCommit"),
      config.getString("conf.kafka.offsetReset"),
      config.getInt("conf.kafka.expirationMins"),
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
