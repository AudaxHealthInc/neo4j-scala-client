package me.jeffmay.neo4j.client

import org.scalatest.Suite
import play.api.libs.json.Json

trait AssertResultStats {
  self: Suite =>

  def assertResultStats(expected: StatementResultStats)(actual: StatementResultStats): Unit = {
    assertResultStats(expected, "expected result stats did not equal actual result stats")(actual)
  }

  def assertResultStats(expected: StatementResultStats, clue: String)(actual: StatementResultStats): Unit = {
    if (expected != actual) {
      fail(s"$clue\nexpected: ${Json.prettyPrint(Json.toJson(expected))}\nactual: ${Json.prettyPrint(Json.toJson(actual))}")
    }
  }
}
