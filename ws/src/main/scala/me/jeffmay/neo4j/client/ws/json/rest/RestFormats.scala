package me.jeffmay.neo4j.client.ws.json.rest

import me.jeffmay.neo4j.client.ws.json.SharedFormats
import play.api.libs.json.{Reads, Json, Writes}

private[json] trait RestFormats extends SharedFormats {

  implicit lazy val writesRawRequestStatement: Writes[RawRequestStatement] = Json.writes[RawRequestStatement]
  implicit lazy val writesRawStatementTransactionRequest: Writes[RawStatementTransactionRequest] = Json.writes[RawStatementTransactionRequest]

  implicit lazy val readsRawError: Reads[RawError] = Json.reads[RawError]
  implicit lazy val readsRawResultStats: Reads[RawResultStats] = Json.reads[RawResultStats]
  implicit lazy val readsRawStatementResult: Reads[RawStatementResult] = Json.reads[RawStatementResult]
  implicit lazy val readsRawStatementRow: Reads[RawStatementRow] = Json.reads[RawStatementRow]
  implicit lazy val readsRawTxnInfo: Reads[RawTxnInfo] = Json.reads[RawTxnInfo]
  implicit lazy val readsRawTxnResponse: Reads[RawTxnResponse] = Json.reads[RawTxnResponse]
}
