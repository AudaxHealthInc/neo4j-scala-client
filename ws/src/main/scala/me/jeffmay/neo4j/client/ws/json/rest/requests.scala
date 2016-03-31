package me.jeffmay.neo4j.client.ws.json.rest

import me.jeffmay.neo4j.client.cypher.{CypherProps, CypherStatement}
import me.jeffmay.neo4j.client.ws.json.rest.RestFormats._
import play.api.libs.json.{JsObject, OWrites}

import scala.language.implicitConversions

/**
  * The request sent to Neo4j's REST API for Cypher transactions.
  *
  * @param statements the Cypher statements to execute in bulk
  */
private[client] case class RawStatementTransactionRequest(
  statements: Seq[RawRequestStatement]
)

private[client] object RawStatementTransactionRequest {

  def fromCypherStatements(statements: Seq[CypherStatement]): RawStatementTransactionRequest = {
    new RawStatementTransactionRequest(statements.map(RawRequestStatement.fromCypherStatement))
  }
}

/**
  * The json representation of a [[CypherStatement]] for Neo4j's REST API.
  *
  * @param statement the query string
  * @param parameters the parameters to substitute into the query string
  * @param includeStats whether to include stats about this statement
  */
private[client] case class RawRequestStatement(
  statement: String,
  parameters: JsObject,
  includeStats: Boolean
)

private[client] object RawRequestStatement {

  def fromCypherStatement(statement: CypherStatement): RawRequestStatement = {
    new RawRequestStatement(
      statement.template,
      OWrites.map[CypherProps].writes(statement.parameters),
      statement.includeStats
    )
  }
}
