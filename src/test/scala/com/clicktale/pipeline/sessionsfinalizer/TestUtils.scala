package com.clicktale.pipeline.sessionsfinalizer

import java.time._
import com.google.gson._
import scala.util.Random
import com.typesafe.config._
import com.clicktale.pipeline.sessionsfinalizer.contracts.FinalizerService._

object TestUtils {
  private val zoneUtc = ZoneOffset.UTC
  val config: Config = ConfigFactory.load("app.conf")
  private val rand = new Random(LocalTime.now().getSecond)
  private val epoch = ZonedDateTime.of(2015, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))

  val serializer: Gson = new GsonBuilder().create()

  def isDevMachine: Boolean = {
    System.getProperty("os.name").toLowerCase.startsWith("windows")
  }

  def getSid(minutesAgo: Int): Long = {
    val time = ZonedDateTime.now(ZoneId.of("UTC")).minusMinutes(minutesAgo)
    val duration = Duration.between(epoch, time)
    val milliseconds: Long = duration.getSeconds * 1000
    val sid: Long =  (milliseconds << 14) + rand.nextInt(1000)
    sid
  }

  def getSid: Long = getSid(0)

  def toString(x: Session): String = serializer.toJson(x)
}
