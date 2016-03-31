package me.jeffmay.neo4j.client.ws

import me.jeffmay.neo4j.client.cypher._
import me.jeffmay.neo4j.client.{Neo4jError, Show}

class WSFixtureQueriesSpec extends CommonWSNeo4jClientSpec {

  s"$CreateNode should add a single node to the graph" in { fixture =>
    import fixture._
    for {
      rsp <- client.openAndCommitTxn(CreateNode.query("createSingleNode"))
    } yield {
      assert(rsp.errors.isEmpty, "should not encounter errors on create")
    }
  }

  s"$CreateNode should return the correct stats for a single create statement" in { fixture =>
    import fixture._
    for {
      rsp <- client.openAndCommitTxn(CreateNode.query("singleWithStats").withStats)
    } yield {
      assertResult(1, "should have the correct stats included") {
        rsp.results.head.stats.get.nodesCreated
      }
    }
  }

  s"$CreateNodeWithPropsObject should create node with empty props object" in { fixture =>
    import fixture._
    for {
      resp <- client.openAndCommitTxn(CreateNodeWithPropsObject.query(Cypher.props()).withStats)
      _ <- client.openAndCommitTxn(CreateNodeWithPropsObject.cleanup())
    } yield {
      resp.result match {
        case Right(result) =>
          assertResult(Some(1)) {
            result.stats.map(_.nodesCreated)
          }
        case Left(errors) =>
          fail(s"Unexpected errors: ${Show[Seq[Neo4jError]].show(errors)}")
      }
    }
  }

  s"$CreateNodeWithPropsObject should create node with multiple properties" in { fixture =>
    import fixture._
    for {
      resp <- client.openAndCommitTxn(CreateNodeWithPropsObject.query(Cypher.props("id" -> 1, "name" -> "multipleProps")).withStats)
      _ <- client.openAndCommitTxn(CreateNodeWithPropsObject.cleanup())
    } yield {
      resp.result match {
        case Right(result) =>
          assertResult(Some(1)) {
            result.stats.map(_.nodesCreated)
          }
        case Left(errors) =>
          fail(s"Unexpected errors: ${Show[Seq[Neo4jError]].show(errors)}")
      }
    }
  }

  s"$FindNode should extract a node's properties" in { fixture =>
    import fixture._
    val nodeId = "extractNodeProps"
    for {
      insert <- client.openAndCommitTxn(CreateNode.query(nodeId))
      fetch <- client.openAndCommitTxn(FindNode.query(nodeId))
    } yield {
      assert(insert.errors.isEmpty, "should not encounter errors on create")
      fetch.result match {
        case Right(rs) =>
          rs.rows.headOption match {
            case Some(row) =>
              assertResult(nodeId) {
                (row.col(FindNode.NodeName) \ PropKeys.Id).as[String]
              }
              assertResult(namespace.value) {
                (row.col(FindNode.NodeName) \ PropKeys.Namespace).as[String]
              }
            case None => fail("Found 0 rows")
          }
        case Left(errors) => fail(s"Encountered errors: [${errors.map(Show[Neo4jError].show).mkString(", ")}]")
      }
    }
  }
}
