package me.jeffmay.neo4j.client

import me.jeffmay.neo4j.client.cypher._
import play.api.libs.json.{JsArray, Json, Reads, Writes}

package object rest {

  implicit lazy val readsNeo4jError: Reads[Neo4jError] = Json.reads[Neo4jError]

  implicit lazy val writesCypherValue: Writes[CypherValue] = Writes {
    case CypherArray(vs) => JsArray(vs.map(writesCypherValue.writes))
    case CypherString(v) => Json.toJson(v)
    case CypherInt(v) => Json.toJson(v)
    case CypherDouble(v) => Json.toJson(v)
    case CypherBoolean(v) => Json.toJson(v)
    case CypherLong(v) => Json.toJson(v)
    case CypherFloat(v) => Json.toJson(v)
    case CypherByte(v) => Json.toJson(new String(Array(v), "UTF-8"))
    case CypherChar(v) => Json.toJson(v.toString)
    case CypherShort(v) => Json.toJson(v)
  }
}
