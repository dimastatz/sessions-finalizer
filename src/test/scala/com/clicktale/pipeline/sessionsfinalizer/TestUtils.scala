package com.clicktale.pipeline.sessionsfinalizer

import java.time._

import com.typesafe.config.{Config, ConfigFactory}

import scala.util.Random

object TestUtils {
  private val zoneUtc = ZoneOffset.UTC
  private val rand = new Random(LocalTime.now().getSecond)
  private val epoch = ZonedDateTime.of(2015, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
  // configuration file
  val config: Config = ConfigFactory.load("app.conf")

  def isDevMachine: Boolean = {
    System.getProperty("os.name").toLowerCase.startsWith("windows")
  }

  def getSid: Long = {
    val time = ZonedDateTime.now(ZoneId.of("UTC")).minusMinutes(10)
    val duration = Duration.between(epoch, time)
    val milliseconds: Long = duration.getSeconds * 1000
    val sid: Long =  (milliseconds << 14) + rand.nextInt(1000)
    sid
  }
}
