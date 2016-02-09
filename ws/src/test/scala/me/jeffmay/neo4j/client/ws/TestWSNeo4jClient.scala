package me.jeffmay.neo4j.client.ws

import akka.actor.Scheduler
import me.jeffmay.neo4j.client.{Neo4jClientConfig, TestGlobal}
import me.jeffmay.util.akka.TestGlobalAkka
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext

/**
  * A [[WSNeo4jClient]] with default configuration provided from global values when appropriate.
  */
class TestWSNeo4jClient(
  ws: WSClient = TestWSClient.TestWS,
  config: Neo4jClientConfig = Neo4jClientConfig.fromConfig(TestGlobal.config),
  scheduler: Scheduler = TestGlobalAkka.scheduler
)(implicit executionContext: ExecutionContext)
  extends WSNeo4jClient(ws, config, scheduler, executionContext)
