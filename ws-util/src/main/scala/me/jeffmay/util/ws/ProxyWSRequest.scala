package me.jeffmay.util.ws

import play.api.libs.iteratee.Enumerator
import play.api.libs.ws._

import scala.concurrent.Future

/**
  * Proxies all method calls to the underlying request.
  *
  * @note To be safe, all subclasses should be final to encourage creating a proxy class around them.
  *       This insures that none of the proxies are clobbered.
  *
  * @note Be sure to always call the methods on the underlying WSRequest and not on this to avoid
  *       infinite proxy loops.
  *
  * @param underlying the underlying WSRequest to wrap
  * @param proxy a function to create a new instance of this class
  */
class ProxyWSRequest(underlying: WSRequest, proxy: WSRequest => WSRequest) extends WSRequest with Proxy {
  override def self: Any = underlying
  def proxyTo(fn: WSRequest => WSRequest): WSRequest = {
    proxy(fn(underlying))
  }
  override def withHeaders(hdrs: (String, String)*): WSRequest = proxyTo(_.withHeaders(hdrs: _*))
  override def withAuth(username: String, password: String, scheme: WSAuthScheme): WSRequest =
    proxyTo(_.withAuth(username, password, scheme))
  override def withQueryString(parameters: (String, String)*): WSRequest =
    proxyTo(_.withQueryString(parameters: _*))
  override def execute(): Future[WSResponse] = underlying.execute()
  override def sign(calc: WSSignatureCalculator): WSRequest = proxyTo(_.sign(calc))
  override def stream(): Future[(WSResponseHeaders, Enumerator[Array[Byte]])] = underlying.stream()
  override def withVirtualHost(vh: String): WSRequest = proxyTo(_.withVirtualHost(vh))
  override def withMethod(method: String): WSRequest = proxyTo(_.withMethod(method))
  override def withRequestTimeout(timeout: Long): WSRequest = proxyTo(_.withRequestTimeout(timeout))
  override def withProxyServer(proxyServer: WSProxyServer): WSRequest  = proxyTo(_.withProxyServer(proxyServer))
  override def withFollowRedirects(follow: Boolean): WSRequest = proxyTo(_.withFollowRedirects(follow))
  override def withBody(body: WSBody): WSRequest = proxyTo(_.withBody(body))
  override val calc: Option[WSSignatureCalculator] = underlying.calc
  override val url: String = underlying.url
  override val queryString: Map[String, Seq[String]] = underlying.queryString
  override val method: String = underlying.method
  override val followRedirects: Option[Boolean] = underlying.followRedirects
  override val body: WSBody = underlying.body
  override val requestTimeout: Option[Int] = underlying.requestTimeout
  override val virtualHost: Option[String] = underlying.virtualHost
  override val proxyServer: Option[WSProxyServer] = underlying.proxyServer
  override val auth: Option[(String, String, WSAuthScheme)] = underlying.auth
  override val headers: Map[String, Seq[String]] = underlying.headers
  override def toString(): String = s"ProxyWSRequest(${underlying.toString})"
}
