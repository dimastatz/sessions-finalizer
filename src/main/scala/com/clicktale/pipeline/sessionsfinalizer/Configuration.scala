package com.clicktale.pipeline.sessionsfinalizer

import scala.util._
import java.nio.file._
import com.typesafe.config._
import com.clicktale.pipeline.sessionsfinalizer.Configuration._

case class Configuration
(
  environment: String,
  address: NetworkAddress
)

object Configuration {
  final val configName = "app.conf"
  final val logName = "./logback.xml"
  case class NetworkAddress(port: Int, host: String)

  def load(): Configuration = {
    loadLogback()
    val config = loadConfig()

    Configuration(
      config.getString("conf.env"),
      NetworkAddress(config.getInt("conf.port"), config.getString("conf.interface")))
  }

  private def loadConfig(): Config = {
    Try(ConfigFactory
      .load(s"./$configName"))
      .getOrElse(ConfigFactory.load(configName))
  }

  private def loadLogback(): Unit = {
    if (Files.exists(Paths.get(logName)))
      System.setProperty("logback.configurationFile", logName)
  }
}
