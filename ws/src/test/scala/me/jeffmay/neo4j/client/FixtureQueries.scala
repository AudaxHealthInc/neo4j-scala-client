package me.jeffmay.neo4j.client

import me.jeffmay.neo4j.client.cypher.Statement
import me.jeffmay.util.Namespace
import play.api.libs.json.Json

object FixtureQueries extends FixtureQueries
trait FixtureQueries {

  /*
   * Each query has some pre-conditions / post-conditions for the expected success result stats to work.
   */

  object CreateNode {
    def query(id: String, includeStats: Boolean = true)(implicit ns: Namespace): Statement = {
      Statement(
        "CREATE (n { ns: {props}.ns, id: {props}.id })",
        Map("props" -> Json.obj(
          "id" -> id,
          "ns" -> ns.value
        )),
        includeStats = includeStats
      )
    }
    val successResultStats: StatementResultStats = {
      StatementResultStats.empty.copy(containsUpdates = true, nodesCreated = 1, propertiesSet = 2)
    }
  }

  object RenameNode {
    def query(id: String, newId: String, includeStats: Boolean = true)(implicit ns: Namespace): Statement = {
      Statement(
        "MATCH (n { ns: {props}.ns, id: {props}.id }) SET n.id = {new}.id",
        Map(
          "props" -> Json.obj(
            "id" -> id,
            "ns" -> ns.value
          ),
          "new" -> Json.obj(
            "id" -> newId
          )
        ),
        includeStats = includeStats
      )
    }
    val successResultStats: StatementResultStats = {
      StatementResultStats.empty.copy(containsUpdates = true, propertiesSet = 1)
    }
  }

  object AddLabel {
    def query(id: String, label: String, includeStats: Boolean = true)(implicit ns: Namespace): Statement = {
      Statement(
        s"MATCH (n { ns: {props}.ns, id: {props}.id }) SET n :${cypher.Label(label)}",
        Map(
          "props" -> Json.obj(
            "id" -> id,
            "ns" -> ns.value
          )
        ),
        includeStats = includeStats
      )
    }
    val successResultStats: StatementResultStats = {
      StatementResultStats.empty.copy(containsUpdates = true, labelsAdded = 1)
    }
  }
}
