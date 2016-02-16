package me.jeffmay.neo4j.client.ws.json.debug

import me.jeffmay.neo4j.client._
import me.jeffmay.neo4j.client.cypher.CypherStatement
import me.jeffmay.neo4j.client.ws.json.SharedFormats
import me.jeffmay.neo4j.client.ws.json.rest._
import play.api.libs.json._

private[json] trait DebugFormats extends SharedFormats {

  /*
   * Core model formats
   */

  /**
    * Formats [[Neo4jError]]s as Json object for REST API, but writes it for debugging purposes.
    */
  implicit lazy val writesNeo4jError: Writes[Neo4jError] = {
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

  implicit lazy val writesCypherStatement: Writes[CypherStatement] = Json.writes[CypherStatement]
  implicit lazy val writesStatementResult: Writes[StatementResult] = Json.writes[StatementResult]
  implicit lazy val writesStatementResultRow: Writes[StatementResultRow] = Json.writes[StatementResultRow]
  implicit lazy val writesStatementResultStats: Writes[StatementResultStats] = Json.writes[StatementResultStats]

  /*
   * Transaction response formats
   */

  implicit lazy val writesTxnInfo: Writes[TxnInfo] = Json.writes[TxnInfo]
  implicit lazy val writesTxnRef: Writes[TxnRef] = Writes(r => JsString(r.url))

  implicit val writesTxnResponse: Writes[TxnResponse] = Writes {
    case open: OpenedTxnResponse => writesOpenedTxnResponse.writes(open)
    case closed: CommittedTxnResponse => writesCommittedTxnResponse.writes(closed)
  }

  implicit lazy val writesOpenedTxnResponse: Writes[OpenedTxnResponse] = Writes {
    case single: SingleOpenedTxnResponse => writesSingleOpenedTxnResponse.writes(single)
    case generic: GenericOpenedTxnResponse => writesGenericOpenedTxnResponse.writes(generic)
  }
  implicit lazy val writesSingleOpenedTxnResponse: Writes[SingleOpenedTxnResponse] = Json.writes[SingleOpenedTxnResponse]
  implicit lazy val writesGenericOpenedTxnResponse: Writes[GenericOpenedTxnResponse] = Json.writes[GenericOpenedTxnResponse]

  implicit lazy val writesCommittedTxnResponse: Writes[CommittedTxnResponse] = Writes {
    case single: SingleCommittedTxnResponse => writesSingleCommittedTxnResponse.writes(single)
    case generic: GenericCommittedTxnResponse => writesGenericCommittedTxnResponse.writes(generic)
  }
  implicit lazy val writesSingleCommittedTxnResponse: Writes[SingleCommittedTxnResponse] = Json.writes[SingleCommittedTxnResponse]
  implicit lazy val writesGenericCommittedTxnResponse: Writes[GenericCommittedTxnResponse] = Json.writes[GenericCommittedTxnResponse]

  /*
   * Raw REST API formats
   */

  implicit lazy val writesRawError: Writes[RawError] = Json.writes[RawError]
  implicit lazy val writesRawResultStats: Writes[RawResultStats] = Json.writes[RawResultStats]
  implicit lazy val writesRawStatementResult: Writes[RawStatementResult] = Json.writes[RawStatementResult]
  implicit lazy val writesRawStatementRow: Writes[RawStatementRow] = Json.writes[RawStatementRow]
  implicit lazy val writesRawTxnInfo: Writes[RawTxnInfo] = Json.writes[RawTxnInfo]
  implicit lazy val writesRawTxnResponse: Writes[RawTxnResponse] = Json.writes[RawTxnResponse]
}
