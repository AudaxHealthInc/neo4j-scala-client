package me.jeffmay.neo4j.client.ws

import me.jeffmay.neo4j.client._
import me.jeffmay.neo4j.client.cypher.{Cypher, CypherStatement}
import me.jeffmay.util.UniquePerClassNamespace
import me.jeffmay.util.akka.TestGlobalAkka
import org.scalatest._

import scala.concurrent.{ExecutionContext, Future}

class WSNeo4jClientBasicSpecs extends fixture.AsyncWordSpec
  with Safety
  with AssertResultStats
  with UniquePerClassNamespace {

  import ws.json.debug._

  override implicit def executionContext: ExecutionContext = ExecutionContext.global

  class FixtureParam extends UniqueNamespace with FixtureQueries {
    val client: Neo4jClient = new WSNeo4jClient(
      TestWSClient.TestWS,
      Neo4jClientConfig.fromConfig(TestGlobal.config),
      TestGlobalAkka.scheduler,
      executionContext
    )
  }

  override def withAsyncFixture(test: OneArgAsyncTest): Future[Outcome] = {
    val fixture = new FixtureParam
    val futureOutcome = test(fixture)
    Neo4jTestNamespace.cleanupAfter(futureOutcome, fixture.namespace, fixture.client)
  }

  "WSNeo4jClient" when {

    "providing authorization" should {

      "not require a password change on boot" in { fixture =>
        import fixture._
        for {
          passwordChangeRequired <- client.passwordChangeRequired()
        } yield {
          assertResult(false, "password change should not be required after initializing") {
            passwordChangeRequired
          }
        }
      }
    }

    "calling openAndCloseTxn" should {

      "start with an empty unique namespace" in { fixture =>
        import fixture._
        val getAllNodes = CypherStatement(
          "MATCH (n { ns: {props}.ns }) RETURN n",
          Map("props" -> Cypher.props("ns" -> namespace.value))
        )
        for {
          rsp <- client.openAndCommitTxn(getAllNodes)
        } yield {
          assertResult(true, "should not encounter errors") {
            rsp.errors.isEmpty
          }
          assertResult(Seq(), "namespace should be empty") {
            rsp.results.head.data
          }
        }
      }

      "add a single node to the graph" in { fixture =>
        import fixture._
        for {
          rsp <- client.openAndCommitTxn(CreateNode.query("singleWithoutStats", includeStats = false))
        } yield {
          assertResult(true, "should not encounter errors") {
            rsp.errors.isEmpty
          }
        }
      }

      "return the correct stats for a single create statement" in { fixture =>
        import fixture._
        for {
          rsp <- client.openAndCommitTxn(CreateNode.query("singleWithStats"))
        } yield {
          assertResult(1, "should have the correct stats included") {
            rsp.results.head.stats.get.nodesCreated
          }
        }
      }

      "return the correct stats for sequence of statements" in { fixture =>
        import fixture._
        val queries = Seq(
          CreateNode.query("a"),
          RenameNode.query("a", "b"),
          AddLabel.query("b", "B")
        )
        val expectedResultStats = Seq(
          CreateNode.successResultStats,
          RenameNode.successResultStats,
          AddLabel.successResultStats
        )
        for {
          rsp <- client.openAndCommitTxn(queries)
        } yield {
          assert(rsp.results.size === expectedResultStats.size)
          val tests = (expectedResultStats zip rsp.results).zipWithIndex
          for (((expected, actual), n) <- tests) {
            assertResultStats(
              expected,
              s"stats for query #${n + 1} '${queries(n).template}' did not have the expected stats") {
              actual.stats.get
            }
          }
          Succeeded
        }
      }
    }

  }
}
