package me.jeffmay.neo4j.client

import org.scalatest.Suite

trait AssertResultStats {
  self: Suite =>

  def assertResultStats(expected: StatementResultStats)(actual: StatementResultStats)(implicit show: Show[StatementResultStats]): Unit = {
    assertResultStats(expected, "expected result stats did not equal actual result stats")(actual)
  }

  def assertResultStats(expected: StatementResultStats, clue: String)(actual: StatementResultStats)(implicit show: Show[StatementResultStats]): Unit = {
    if (expected != actual) {
      fail(s"$clue\nexpected: ${show show expected}\nactual: ${show show actual}")
    }
  }
}
