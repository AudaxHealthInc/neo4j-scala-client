package me.jeffmay.neo4j.client

import play.api.libs.json._

case class Neo4jError(status: Neo4jStatusCode, message: String)

object Neo4jError {
  implicit val jsonWriter: Writes[Neo4jError] = {
    OWrites { error =>
      // Prettify the message by splitting up the message into equally indented lines
      // and avoiding escaped quotes
      val messageLines = error.message.split("\\n").zipWithIndex.map {
        case (line, num) => Json.obj(f"Line #$num%-2d" -> line.replaceAllLiterally("\"", "'"))
      }
      Json.obj(
        "code" -> error.status.code,
        "description" -> error.status.description,
        "message" -> messageLines
      )
    }
  }
}
