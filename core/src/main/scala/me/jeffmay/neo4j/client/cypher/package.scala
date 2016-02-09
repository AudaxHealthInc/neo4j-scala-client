package me.jeffmay.neo4j.client

/**
  * This is meant to be imported as [[me.jeffmay.neo4j.client.cypher]] and then all
  * classes in this package are prefixed.
  */
package object cypher extends CypherInterpolation {

  type CypherProps = Map[String, CypherValue]
}

