package com.clicktale.pipeline.sessionsfinalizer

import com.typesafe.scalalogging.LazyLogging

object Boot extends LazyLogging {
  def main(args: Array[String]): Unit = {
    logger.debug("starting sessions-finalizer")
  }
}
