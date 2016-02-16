package me.jeffmay.neo4j.client

/**
  * Represents an error message sent back from the Neo4j REST API.
  *
  * @param status the parsed generic status code
  * @param message a detailed and specific error message explaining what was wrong with the request
  */
case class Neo4jError(status: Neo4jStatusCode, message: String)
