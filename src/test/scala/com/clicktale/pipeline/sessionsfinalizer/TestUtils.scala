package com.clicktale.pipeline.sessionsfinalizer

import java.time._
import scala.util.Random

object TestUtils {
  private val zoneUtc = ZoneOffset.UTC
  private val rand = new Random(LocalTime.now().getSecond)
  private val epoch = ZonedDateTime.of(2015, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))


  def isDevMachine: Boolean = {
    System.getProperty("os.name").toLowerCase.startsWith("windows")
  }

  def getSid: Long = {
    val duration = Duration.between(epoch, ZonedDateTime.now(ZoneId.of("UTC")))
    val milliseconds: Long = duration.getSeconds * 1000
    val sid: Long =  (milliseconds << 14) + rand.nextInt(1000)
    sid
  }
}
