package me.jeffmay.util.ws

import akka.actor.Scheduler
import me.jeffmay.util.concurrent.FutureWithTimeout
import play.api.libs.ws.{WSRequest, WSResponse}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

final class TimeoutWSRequest(underlying: WSRequest, after: FiniteDuration)
  (implicit scheduler: Scheduler, ec: ExecutionContext)
  extends ProxyWSRequest(underlying, new TimeoutWSRequest(_, after)) {

  override def self: Any = (underlying, after)

  override def execute(): Future[WSResponse] = {
    val wsTimeoutSet = underlying.withRequestTimeout(after.toMillis)
    FutureWithTimeout.wrap(wsTimeoutSet.execute()).withTimeout(after)
  }

  override def toString(): String = s"TimeoutWSRequest(${underlying.toString}, $after)"
}
