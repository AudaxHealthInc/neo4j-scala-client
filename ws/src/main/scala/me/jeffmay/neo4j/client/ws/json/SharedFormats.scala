package me.jeffmay.neo4j.client.ws.json

import me.jeffmay.neo4j.client.cypher._
import me.jeffmay.neo4j.client.{Neo4jError, Neo4jStatusCode}
import play.api.libs.json._

private[json] trait SharedFormats {

  /**
    * Formats [[Neo4jStatusCode]]s as a String in Json using the full key path.
    */
  implicit lazy val formatStatusCode = new Format[Neo4jStatusCode] {
    override def reads(json: JsValue): JsResult[Neo4jStatusCode] = {
      json.validate[String].flatMap { code =>
        Neo4jStatusCode.findByCode(code) match {
          case Some(status) => JsSuccess(status)
          case None => JsError("error.expected.Neo4jStatusCode")
        }
      }
    }
    override def writes(status: Neo4jStatusCode): JsValue = JsString(status.code)
  }

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
