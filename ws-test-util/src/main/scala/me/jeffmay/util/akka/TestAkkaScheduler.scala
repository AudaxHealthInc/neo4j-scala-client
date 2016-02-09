package me.jeffmay.util.akka

import java.util.concurrent.ThreadFactory

import akka.actor.{LightArrayRevolverScheduler, Scheduler}
import akka.event.{NoLogging, LoggingAdapter}
import com.typesafe.config.{ConfigFactory, Config}

/**
  * Creates an Akka [[Scheduler]] with default arguments.
  */
object TestAkkaScheduler {

  def apply(
    threadFactory: ThreadFactory = TestGlobalAkka.threadFactory,
    logging: LoggingAdapter = NoLogging,
    config: Config = ConfigFactory.load()
  ): Scheduler = {
    new LightArrayRevolverScheduler(
      config,
      logging,
      threadFactory
    )
  }
}
