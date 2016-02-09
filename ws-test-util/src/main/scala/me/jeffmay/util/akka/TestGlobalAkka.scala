package me.jeffmay.util.akka

import java.util.concurrent.ThreadFactory

import akka.actor.{ActorSystem, Scheduler}
import akka.dispatch.MonitorableThreadFactory

object TestGlobalAkka {

  private[akka] lazy val threadFactory: ThreadFactory = MonitorableThreadFactory(
    "global-akka-scheduler",
    daemonic = true,
    contextClassLoader = Option(getClass.getClassLoader)
  )

  def scheduler: Scheduler = Implicits.scheduler

  def system: ActorSystem = Implicits.system

  object Implicits {

    implicit lazy val scheduler: Scheduler = TestAkkaScheduler()

    implicit lazy val system: ActorSystem = ActorSystem("TestGlobalAkka")
  }
}
