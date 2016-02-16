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
case class CypherStatement(
  template: String,
  parameters: CypherParams = Map.empty,
  includeStats: Boolean = false
) {

  /**
    * Request that this statement include the stats
    */
  def withStats: CypherStatement = {
    if (includeStats) this
    else copy(includeStats = true)
  }

  /**
    * Provided as a mechanism for communicating the common error of appending a string to a string-looking statement.
    *
    * @note This does not protect you against appending a statement to a string.
    */
  @deprecated("Only append cypher\"\" interpolated strings with :+: to avoid cypher injection attacks", "0.4.0")
  def +(literal: String): CypherStatement = this.copy(template + literal)

  /**
    * Prepends the left-hand-side statement's template to this template and merges the properties.
    *
    * @note `:+:` is right applicative, so `a :+: b` is actually `b.:+:(a)` and thus it prepends `a` to `b`
    *
    * @see [[concat]]
    */
  @inline final def :+:(that: CypherStatement): CypherStatement = that concat this

  /**
    * Appends the given statement's template to this template and merges the properties.
    *
    * @note WARNING: Any settings like "includeStats" drop back to the default.
    *       You have to explicitly add the settings you want after concatenating the statements.
    * @throws ConflictingParameterFieldsException if the statements contain conflicting property values for a single
    *                                             property name within a namespace.
    */
  def concat(that: CypherStatement): CypherStatement = {
    val mergedTemplate = this.template + that.template
    val conflictingNamespaces = this.parameters.keySet intersect that.parameters.keySet
    var mergedOverrides = Seq.empty[(String, CypherProps)]
    for (ns <- conflictingNamespaces) {
      val thisParams = this.parameters(ns)
      val thatParams = that.parameters(ns)
      val conflictingParamKeys = thisParams.keySet intersect thatParams.keySet
      for (key <- conflictingParamKeys) {
        val duplicates = Set(thisParams(key), thatParams(key))
        if (duplicates.size > 1) {
          throw new ConflictingParameterFieldsException(ns, key, duplicates.toSeq, mergedTemplate)
        }
      }
      mergedOverrides :+= ns -> (thisParams ++ thatParams)
    }
    // Merge the parameters by iterating over the non-conflicting namespaces and appending the merged props
    val mergedParams = (
      (this.parameters.view ++ that.parameters.view).filterNot {
        case (ns, _) => conflictingNamespaces(ns)
      } ++
        mergedOverrides.view
      ).toMap
    CypherStatement(mergedTemplate, mergedParams)
  }
}
