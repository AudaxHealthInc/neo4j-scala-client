package me.jeffmay.neo4j.client.ws

import me.jeffmay.util.ws.ProxyWSRequest
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.ws.{WSRequest, WSResponse}

import scala.concurrent.Future

final class DebugWSRequest(underlying: WSRequest, debugHandle: String, debugFn: String => Unit = println)
  extends ProxyWSRequest(underlying, new DebugWSRequest(_, debugHandle, debugFn)) {

  override def execute(): Future[WSResponse] = {
    val passwordInfo = this.auth.fold(Seq[String]()) { case (u, p, _) => Seq(s"username=$u", s"password=$p") }
    val timeoutInfo = this.requestTimeout.map { t => s"timeout=$t" }
    val debugInfo = passwordInfo ++ timeoutInfo
    debugFn(s"${DateTime.now(DateTimeZone.UTC)} [$debugHandle] " +
      s"$method $url ${debugInfo.mkString("[", ", ", "]")}\n"
    )
    underlying.execute()
  }

  override def toString(): String = {
    s"DebugWSRequest(${underlying.toString})"
  }
}
