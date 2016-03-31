package me.jeffmay.neo4j.client.ws

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest._

class WSNeo4jClientSpec extends CommonWSNeo4jClientSpec {

  s"$ClientName should log queries at debug level" in { fixture =>
    import fixture._
    for {
      rsp <- client.openAndCommitTxn(CreateNode.query("logAtDebug"))
    } yield {
      verify(logger, times(1)).debug(any())
      Succeeded
    }
  }

  s"$ClientName return the correct stats for sequence of statements" in { fixture =>
    import fixture._
    val queries = Seq(
      CreateNode.query("a").withStats,
      RenameNode.query("a", "b").withStats,
      AddLabel.query("b", "B").withStats
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
