package com.clicktale.pipeline.sessionsfinalizer

import scala.io._
import java.nio.file._
import com.typesafe.scalalogging.LazyLogging

object Boot extends LazyLogging {
  def main(args: Array[String]): Unit = {
    logger.debug("starting sessions-finalizer")

    loadLogback()
    logger.error("log is loaded")

    // wait for io
    StdIn.readLine()
  }

  private def loadLogback() = {
    if(Files.exists(Paths.get("./logback.xml")))
      System.setProperty("logback.configurationFile", s"./logback.xml")
  }

}
