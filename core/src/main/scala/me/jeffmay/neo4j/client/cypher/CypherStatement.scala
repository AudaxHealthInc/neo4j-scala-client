package me.jeffmay.neo4j.client.cypher

import me.jeffmay.neo4j.client.Show

import scala.util.matching.Regex

/**
  * Represents a cypher statement.
  *
  * @note You should always provide the parameters instead of inserting values directly into the query
  *       to prevent cypher injection attacks.
  * @see <a href="http://neo4j.com/docs/stable/rest-api-transactional.html">Transaction Documentation</a>
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
    * Validates that the parameters referenced in the template have associated entries in the [[parameters]],
    * and that object references and parameter field references are not mixed within the same query.
    *
    * @note This does NOT validate that the query is a valid Cypher query string.
    */
  def validate(): Unit = {
    // Get the referenced namespaces from the template and their referenced fields
    val namespaceReferences = CypherStatement.parseParams(template)
    var nsObjRefs = Set.empty[String]
    var nsFieldRefs = Map.empty[String, Set[String]].withDefaultValue(Set.empty)
    // Build a map of all the param fields referenced and a set of all the param object namespace references
    for ((ns, fieldReferenced) <- namespaceReferences) {
      fieldReferenced match {
        case Some(k) =>
          nsFieldRefs += ns -> (nsFieldRefs(ns) + k)
        case None =>
          nsObjRefs += ns
      }
    }
    // Throw an exception if any references are not present in the parameters
    val referencedNamespaces = nsObjRefs ++ nsFieldRefs.keySet
    for (referencedNs <- referencedNamespaces) {
      if (!(parameters contains referencedNs)) {
        throw new MissingParamReferenceException(this, referencedNs, Set.empty)
      }
      val existingProps = parameters(referencedNs)
      val expectedfields = nsFieldRefs(referencedNs)
      val missingProps = expectedfields -- existingProps.keySet
      if (missingProps.nonEmpty) {
        throw new MissingParamReferenceException(this, referencedNs, missingProps)
      }
    }
  }

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
    // For every namespace that is in conflict
    for (ns <- conflictingNamespaces) {
      // Merge the properties in the conflicting namespace
      mergedOverrides :+= ns -> CypherStatement.mergeNamespace(template, ns, this.parameters(ns), that.parameters(ns))
    }
    // Merge the parameters by iterating over the non-conflicting namespaces and appending the merged props
    val mergedParams = {
      (this.parameters.view ++ that.parameters.view).filterNot {
        case (ns, _) => conflictingNamespaces(ns)
      } ++ mergedOverrides
    }.toMap
    // Build the CypherStatement by concatenating the templates and merging the properties
    CypherStatement(mergedTemplate, mergedParams)
  }
}

object CypherStatement {

  implicit def showStatement(implicit showParams: Show[CypherParams]): Show[CypherStatement] = Show.show[CypherStatement] { stmt =>
    "CypherStatement(" +
    s"  template = ${(if (stmt.template.length > DefaultCypherShows.defaultMaxLineWidth) "\n" else "") + stmt.template},\n" +
    s"  parameters = ${showParams.show(stmt.parameters)},\n" +
    s"  includeStats = ${stmt.includeStats},\n" +
    ")"
  }

  private[cypher] val ParamRegex: Regex = """(\{\s*%s\s*\}\s*(\.%s)?)"""
    .format(CypherIdentifier.ValidChars, CypherIdentifier.ValidChars).r

  private[cypher] def parseParams(template: String): Seq[(String, Option[String])] = {
    ParamRegex.findAllIn(template).foldLeft(Seq.empty[(String, Option[String])]) { (params, nextMatch) =>
      // Convert all "{ props }" and "{props}.id" into "props" and "props.id" respectively
      val clean = nextMatch.replaceAll("[{ }]", "")
      val parts = clean.split("\\.").filterNot(_.isEmpty)
      if (parts.length == 1) params :+ clean -> None
      else if (parts.length == 2) params :+ parts(0) -> Some(parts(1))
      else throw new IllegalArgumentException(
        s"Invalid template param token, '$nextMatch' extracted from Cypher:\n" +
        s"$template\n" +
        s"Split on '.' created ${parts.length} non-empty parts."
      )
    }
  }

  /**
    * Merges all non-conflicting or duplicate properties otherwise throws exception.
    *
    * @param template the template of this query for debugging purposes.
    * @param namespace the namespace to merge for debugging purposes.
    * @param first the first set of properties
    * @param second the second set of properties
    * @return the merged set of first and second properties
    */
  @throws[ConflictingParameterFieldsException]("If the given properties contains any conflicting values for a shared key")
  def mergeNamespace(template: String, namespace: String, first: CypherProps, second: CypherProps): CypherProps = {
    val overlappingKeys = first.keySet intersect second.keySet
    val allConflicts = overlappingKeys.toSeq.map { k =>
      (k, Set(first(k), second(k)).toSeq) // Small Set's retain their order
    }.filter {
      case (k, conflicts) => conflicts.size >= 2
    }.toMap
    if (allConflicts.nonEmpty) {
      throw new ConflictingParameterFieldsException(namespace, allConflicts, template)
    }
    first ++ second
  }
}
