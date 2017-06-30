package com.clicktale.pipeline.sessionsfinalizer.repositories

import com.google.gson._
import java.time.ZonedDateTime
import com.typesafe.config.Config
import com.newmotion.akka.rabbitmq
import com.newmotion.akka.rabbitmq.Channel
import com.clicktale.pipeline.sessionsfinalizer.contracts.FinalizerService._
import com.clicktale.pipeline.sessionsfinalizer.repositories.RabbitRepository._

class RabbitRepository(config: RabbitConfig) {
  val channel: Channel = createChannel()

  def publish(session: Session): Unit = {
    val message = RabbitMessage(session.pid, session.subsId, session.sid, session.createDate)
    channel.basicPublish(config.exchange, "", null, serializer.toJson(message).getBytes)
  }

  private def createChannel(): Channel = {
    val factory = new rabbitmq.ConnectionFactory()
    factory.setHost(config.host)
    factory.setPort(config.port)
    factory.setPassword(config.password)
    factory.setVirtualHost(config.virtualHost)
    factory.setUsername(config.username)
    if (config.useSSL) factory.useSslProtocol()
    val connection = factory.newConnection()
    connection.createChannel()
  }
}

object RabbitRepository {
  val serializer: Gson = new GsonBuilder().create()

  case class RabbitMessage(ProjectId: Int,
                           SubscriberId: Int,
                           LiveSessionId: Long,
                           MessageCreateDate: ZonedDateTime,
                           MessageType: Int = 3, // UnclosedSession
                           Version: Int = 1)

  case class RabbitConfig(port: Int,
                          host: String,
                          useSSL: Boolean,
                          username: String,
                          password: String,
                          virtualHost: String,
                          exchange: String)

  def createRabbitConfig(config: Config): RabbitConfig = {
    RabbitConfig(
      config.getInt("conf.rabbit.port"),
      config.getString("conf.rabbit.host"),
      config.getBoolean("conf.rabbit.usessl"),
      config.getString("conf.rabbit.username"),
      config.getString("conf.rabbit.password"),
      config.getString("conf.rabbit.virtualhost"),
      config.getString("conf.rabbit.exchange")
    )
  }

  def create(config: Config): RabbitRepository = {
    new RabbitRepository(createRabbitConfig(config))
  }
}
