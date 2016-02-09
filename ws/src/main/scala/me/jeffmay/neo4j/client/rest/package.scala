package me.jeffmay.neo4j.client

import play.api.libs.json.{Json, Reads}

package object rest {

  implicit lazy val readsNeo4jError: Reads[Neo4jError] = Json.reads[Neo4jError]
}
