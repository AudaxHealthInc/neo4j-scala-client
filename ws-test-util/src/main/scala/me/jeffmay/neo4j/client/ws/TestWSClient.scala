package me.jeffmay.neo4j.client.ws

import me.jeffmay.util.RunHooks
import play.api.libs.ws.ning.NingWSClient
import play.api.libs.ws.{WSClient, WSRequest}

object TestWSClient {

  private lazy val client: NingWSClient = synchronized {
    val started = NingWSClient()
    RunHooks.addShutdownHook("TestNeo4jWSClient.close()") {
      this.close()
    }
    started
  }

  object TestWS extends WSClient {
    override def underlying[T]: T = client.asInstanceOf[T]
    override def url(url: String): WSRequest = new DebugWSRequest(client.url(url), "TestNeo4jWSClient.TestWS")
    override def close(): Unit = ()
  }

  def close(): Unit = {
    println("Closing connection pool for TestNeo4jWSClient.TestWS")
    client.close()
  }

}
