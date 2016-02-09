package me.jeffmay.neo4j.client

import org.joda.time.DateTime
import play.api.libs.json._

/**
  * Represents the results of executing a batch of [[Statement]]s.
  */
sealed trait TxnResponse {

  /**
    * Whether the transaction is still open.
    */
  @inline final def isTxnOpen: Boolean = openTxnInfo.isDefined

  /**
    * Whether the transaction has been closed.
    */
  @inline final def isTxnClosed: Boolean = !isTxnOpen

  /**
    * Whether the transaction executed any statements.
    *
    * Typically, this is used for open transactions.
    */
  @inline def isEmptyTransaction: Boolean = results.isEmpty

  /**
    * Whether the results are all successful and didn't encounter any rollbacks.
    */
  def allSuccessful: Boolean = errors.forall(!_.status.causedRollback)

  /**
    * Whether the results encountered a transient error that can safely be retried without side-effects.
    */
  def safeToRetry: Boolean = results.forall(_.data.isEmpty) && errors.forall(_.status.safeToRetry)

  /**
    * The results of executing the [[Statement]]s returned in the same order as given.
    */
  def results: Seq[StatementResult]

  /**
    * All the errors encountered when executing the [[Statement]]s within a transaction.
    */
  def errors: Seq[Neo4jError]

  /**
    * The transaction info about the open transaction info.
    */
  def openTxnInfo: Option[TxnInfo]
}
object TxnResponse {

  implicit val jsonWriter: Writes[TxnResponse] = Writes {
    case open: OpenedTxnResponse => OpenedTxnResponse.jsonWriter.writes(open)
    case closed: CommittedTxnResponse => CommittedTxnResponse.jsonWriter.writes(closed)
  }

  def prettyPrint(response: TxnResponse): String = {
    val respAsJson = Json.prettyPrint(Json.toJson(response))
    s"${response.getClass.getSimpleName}:\n$respAsJson"
  }
}

/**
  * Represents an open transaction.
  */
case class OpenedTxnResponse(
  results: Seq[StatementResult],
  transaction: TxnInfo,
  errors: Seq[Neo4jError]
) extends TxnResponse {

  override def openTxnInfo: Option[TxnInfo] = Some(transaction)
}

object OpenedTxnResponse {
  implicit val jsonWriter: Writes[OpenedTxnResponse] = Json.writes[OpenedTxnResponse]
}

/**
  * Represents a closed and committed transaction.
  */
case class CommittedTxnResponse(
  results: Seq[StatementResult],
  errors: Seq[Neo4jError]
) extends TxnResponse {

  override def openTxnInfo: Option[TxnInfo] = None
}

object CommittedTxnResponse {
  implicit val jsonWriter: Writes[CommittedTxnResponse] = Json.writes[CommittedTxnResponse]
}

/**
  * Represents the transaction info
  *
  * @param txn the reference of the transaction
  * @param expires when the transaction expires
  */
case class TxnInfo(txn: TxnRef, expires: DateTime)

object TxnInfo {
  implicit val jsonWriter: Writes[TxnInfo] = Json.writes[TxnInfo]
}
