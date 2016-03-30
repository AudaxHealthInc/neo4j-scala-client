package me.jeffmay.neo4j.client.ws

import me.jeffmay.neo4j.client._
import me.jeffmay.neo4j.client.cypher.{Cypher, CypherStatement}
import me.jeffmay.util.UniquePerClassNamespace
import me.jeffmay.util.akka.TestGlobalAkka
import org.scalatest.mock.MockitoSugar
import org.scalatest.{Outcome, Safety, fixture}
import org.slf4j.Logger

import scala.concurrent.{ExecutionContext, Future}

abstract class CommonWSNeo4jClientSpec extends fixture.AsyncFreeSpec
  with Safety
  with MockitoSugar
  with FixtureQueries
  with WSDefaultShows
  with AssertResultStats
  with UniquePerClassNamespace {

  override implicit def executionContext: ExecutionContext = ExecutionContext.global

  class FixtureParam extends UniqueNamespace {
    val logger: Logger = mock[Logger]
    val client: Neo4jClient = new WSNeo4jClient(
      TestWSClient.TestWS,
      Neo4jClientConfig.fromConfig(TestGlobal.config),
      logger,
      TestGlobalAkka.scheduler,
      executionContext
    )
  }

  override def withAsyncFixture(test: OneArgAsyncTest): Future[Outcome] = {
    val fixture = new FixtureParam
    val futureOutcome = test(fixture)
    Neo4jTestNamespace.cleanupAfter(futureOutcome, fixture.namespace, fixture.client)
  }

  val ClientName = "CommonWSNeo4jClient"

  s"$ClientName should not require a password change on boot" in { fixture =>
    import fixture._
    for {
      passwordChangeRequired <- client.passwordChangeRequired()
    } yield {
      assert(!passwordChangeRequired, "password change should not be required after initializing")
    }
  }

  s"$ClientName should start with an empty unique namespace" in { fixture =>
    import fixture._
    val getAllNodes = CypherStatement(
      "MATCH (n { ns: {props}.ns }) RETURN n",
      Map("props" -> Cypher.props("ns" -> namespace.value))
    )
    for {
      rsp <- client.openAndCommitTxn(getAllNodes)
    } yield {
      assert(rsp.errors.isEmpty, "should not encounter errors")
      assertResult(Seq(), "namespace should be empty") {
        rsp.results.head.data
      }
    }
  }
}
