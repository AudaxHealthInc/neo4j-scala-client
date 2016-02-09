package me.jeffmay.neo4j.client.cypher

/**
  * Represents a cypher statement.
  *
  * @note You should always provide the parameters instead of inserting values directly into the query
  *       to prevent cypher injection attacks.
  *
  * @see <a href="http://neo4j.com/docs/stable/rest-api-transactional.html">Transaction Documentation</a>
  *
  * @param template     the literal Cypher string with parameter placeholders
  * @param parameters   a map of namespaces -> props objects containing the keys to substitute in the
  *                     template associated with the values. These parameters are safe from cypher
  *                     injection attacks when executed on the server.
  * @param includeStats whether to include the [[me.jeffmay.neo4j.client.StatementResultStats]] for this statement
  */
case class Statement(
  template: String,
  parameters: Map[String, CypherProps] = Map.empty,
  includeStats: Boolean = false
) {
  def withStats: Statement = {
    if (includeStats) this
    else copy(includeStats = true)
  }
}
