package me.jeffmay.neo4j.client.cypher

import play.api.libs.json._

/**
  * Represents a cypher statement.
  *
  * @note You should always provide the parameters instead of inserting values directly into the query
  *       to prevent cypher injection attacks.
  * @see http://neo4j.com/docs/stable/rest-api-transactional.html
  * @param statement    the cypher statement to execute
  * @param parameters   a map of the keys and values to inject into the query in a way that is safe from
  *                     cypher injection attacks
  * @param includeStats whether to include the [[me.jeffmay.neo4j.client.StatementResultStats]] for this statement
  */
case class Statement(
  statement: String,
  // TODO: Neo4j doesn't support nested objects, we should use something more type-safe
  parameters: Map[String, JsObject] = Map.empty,
  includeStats: Boolean = false
) {
  def withStats: Statement = {
    if (includeStats) this
    else copy(includeStats = true)
  }
}
