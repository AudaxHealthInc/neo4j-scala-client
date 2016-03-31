package me.jeffmay.neo4j.client

/**
  * This is meant to be imported as [[me.jeffmay.neo4j.client.cypher]] and then all
  * classes in this package are prefixed.
  */
package object cypher extends CypherInterpolation with DefaultCypherShows {

  /**
    * Used to pass cypher-injection-safe parameters to the Neo4j REST API.
    */
  type CypherParams = Map[String, CypherProps]
  type CypherProps = Map[String, CypherValue]

  @deprecated("Use CypherStatement instead", "0.4.0")
  type Statement = CypherStatement
  @deprecated("Use CypherStatement instead", "0.4.0")
  val Statement = CypherStatement

  @deprecated("Use CypherResultException instead", "0.7.0")
  type InvalidCypherException = CypherResultException

  @deprecated("Use MixedParamReferenceException instead", "0.7.0")
  type MutatedParameterObjectException = MixedParamReferenceException
}

