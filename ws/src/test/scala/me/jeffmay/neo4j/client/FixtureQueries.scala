package me.jeffmay.neo4j.client

import me.jeffmay.neo4j.client.cypher.{CypherProps, Cypher, CypherStatement}
import me.jeffmay.util.Namespace

object FixtureQueries extends FixtureQueries
trait FixtureQueries {

  /**
    * The property keys for the nodes in the queries
    */
  object PropKeys {

    val Id = "id"
    val Namespace = "ns"
  }

  /*
   * Each query has some pre-conditions / post-conditions for the expected success result stats to work.
   */

  case object CreateNode {
    def query(id: String)(implicit ns: Namespace): CypherStatement = {
      CypherStatement(
        s"CREATE (n { ns: {props}.ns, ${PropKeys.Id}: {props}.id })",
        Map("props" -> Cypher.props(
          "id" -> id,
          "ns" -> ns.value
        ))
      )
    }
    val successResultStats: StatementResultStats = {
      StatementResultStats.empty.copy(containsUpdates = true, nodesCreated = 1, propertiesSet = 2)
    }
  }

  case object CreateNodeWithPropsObject {
    def cleanup()(implicit ns: Namespace): CypherStatement = {
      CypherStatement(
        s"MATCH (n :${ns.value}) DELETE n"
      )
    }
    def query(props: CypherProps)(implicit ns: Namespace): CypherStatement = {
      CypherStatement(
        s"CREATE (n :${ns.value} { props })",
        Map("props" -> props)
      )
    }
    val successResultStats: StatementResultStats = {
      StatementResultStats.empty.copy(containsUpdates = true, nodesCreated = 1, propertiesSet = 2)
    }
  }

  case object FindNode {
    val NodeName = "n"
    def query(id: String)(implicit ns: Namespace): CypherStatement = {
      CypherStatement(
        s"MATCH ($NodeName { ns: {props}.ns, ${PropKeys.Id}: {props}.id }) RETURN $NodeName",
        Map("props" -> Cypher.props(
          "id" -> id,
          "ns" -> ns.value
        ))
      )
    }
    val successResultStats: StatementResultStats = {
      StatementResultStats.empty
    }
  }

  case object RenameNode {
    def query(id: String, newId: String)(implicit ns: Namespace): CypherStatement = {
      CypherStatement(
        s"MATCH (n { ns: {props}.ns, ${PropKeys.Id}: {props}.id }) SET n.${PropKeys.Id} = {new}.id",
        Map(
          "props" -> Cypher.props(
            "id" -> id,
            "ns" -> ns.value
          ),
          "new" -> Cypher.props(
            "id" -> newId
          )
        )
      )
    }
    val successResultStats: StatementResultStats = {
      StatementResultStats.empty.copy(containsUpdates = true, propertiesSet = 1)
    }
  }

  case object AddLabel {
    def query(id: String, label: String)(implicit ns: Namespace): CypherStatement = {
      CypherStatement(
        s"MATCH (n { ns: {props}.ns, ${PropKeys.Id}: {props}.id }) SET n ${Cypher.label(label).getOrThrow.template}",
        Map(
          "props" -> Cypher.props(
            "id" -> id,
            "ns" -> ns.value
          )
        )
      )
    }
    val successResultStats: StatementResultStats = {
      StatementResultStats.empty.copy(containsUpdates = true, labelsAdded = 1)
    }
  }
}
