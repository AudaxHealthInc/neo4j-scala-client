package me.jeffmay.util.concurrent

import java.util.concurrent.TimeoutException

import akka.actor.Scheduler

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.implicitConversions
import scala.util.Failure

// TODO is this necessary? Figure out why WS was not timing out on its own

object FutureWithTimeout {

  def wrap[T](future: Future[T]): FutureTimeoutOps[T] = new FutureTimeoutOps[T](future)

  def apply[T](timeout: FiniteDuration)(op: => T)(implicit ec: ExecutionContext, scheduler: Scheduler): Future[T] = {
    wrap(Future(op)).withTimeout(timeout)
  }
}

trait FutureWithTimeoutImplicits {
  @inline final implicit def futureWithTimeout[T](future: Future[T]): FutureTimeoutOps[T] = FutureWithTimeout.wrap(future)
}

class FutureTimeoutOps[T](val future: Future[T]) extends AnyVal {

  def withTimeout(timeout: FiniteDuration)(implicit executionContext: ExecutionContext, scheduler: Scheduler): Future[T] = {
    val promise = Promise[T]()
    scheduler.scheduleOnce(timeout) {
      promise.tryComplete(Failure(new TimeoutException(s"Timed out after $timeout")))
    }
    promise.completeWith(future).future
  }
}
