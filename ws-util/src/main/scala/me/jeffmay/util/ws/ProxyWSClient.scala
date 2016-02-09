package me.jeffmay.util.ws

import play.api.libs.ws.{WSRequest, WSClient}

/**
  * Proxies all method calls to the underlying client.
  *
  * Typically this is companied by methods returning [[ProxyWSRequest]].
  */
class ProxyWSClient(underlying: WSClient) extends WSClient with Proxy {
  override def self: Any = underlying
  override def underlying[T]: T = underlying.asInstanceOf[T]
  override def url(url: String): WSRequest = underlying.url(url)
  override def close(): Unit = underlying.close()
}
