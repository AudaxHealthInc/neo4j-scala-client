package me.jeffmay.neo4j.client.ws.json.rest

import me.jeffmay.neo4j.client._
import me.jeffmay.neo4j.client.cypher.CypherStatement
import me.jeffmay.neo4j.client.ws.json.DebugFormats
import org.joda.time.DateTime
import play.api.libs.json._

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
  * The raw json body from the API as a case class model filled with options.
  *
  * Represents the different result types returned from the
  * <a href="http://neo4j.com/docs/stable/rest-api-transactional.html">Transaction API</a>
  */
private[client] case class RawTxnResponse(
  results: Seq[RawStatementResult] = Seq.empty,
  commit: Option[String] = None,
  transaction: Option[RawTxnInfo] = None,
  errors: Seq[RawError] = Seq.empty
) {

  lazy val (notifications, impassibleErrors) = errors.partition(_.code.isNotification)

  def isSuccessful: Boolean = errors.isEmpty

  def transactionComplete: Boolean = transaction.isEmpty && impassibleErrors.isEmpty

  def neo4jErrors: Seq[Neo4jError] = errors.map(_.asNeo4jError)

  private def failToConvert(expected: String, reason: String, cause: Throwable = null): Try[Nothing] = {
    import DebugFormats._
    Failure(new IllegalArgumentException(
      s"Connect convert to $expected from: ${Json.prettyPrint(Json.toJson(this))}\n" +
        s"Reason: $reason",
      cause
    ))
  }

  private def asCommittedTxn[T](f: (Seq[StatementResult], Seq[Neo4jError]) => T): Try[T] = {
    if (transactionComplete)
      Success(f(results.map(_.asStatementResult), errors.map(_.asNeo4jError)))
    else
      failToConvert(
        "CommittedTxnResponse",
        "Transaction URL was empty and / or result contained errors that would trigger rollback"
      )
  }

  def asCommittedTxnResponse(statement: CypherStatement): Try[SingleCommittedTxnResponse] = {
    asCommittedTxn(SingleCommittedTxnResponse(statement, _, _))
  }

  def asCommittedTxnResponse(statements: Seq[CypherStatement]): Try[CommittedTxnResponse] = {
    asCommittedTxn(GenericCommittedTxnResponse(statements, _, _))
  }

  private def asOpenedTxn[T](f: (Seq[StatementResult], TxnInfo, Seq[Neo4jError]) => T): Try[T] = {
    for {
      commitUrl <- commit.map(Success(_)).getOrElse(failToConvert("OpenTxnResponse", "Missing commitUrl"))
      txn <- transaction.map(Success(_)).getOrElse(failToConvert("OpenTxnResponse", "Missing transaction"))
      txnRef <- Try(TxnRef.parse(commitUrl)).recoverWith {
        case ex => failToConvert("OpenTxnResponse", "Failed to parse TxnRef", ex)
      }
    } yield {
      f(results.map(_.asStatementResult), TxnInfo(txnRef, txn.expires), errors.map(_.asNeo4jError))
    }
  }

  def asOpenedTxnResponse(statement: CypherStatement): Try[SingleOpenedTxnResponse] = {
    asOpenedTxn(SingleOpenedTxnResponse(statement, _, _, _))
  }

  def asOpenedTxnResponse(statements: Seq[CypherStatement]): Try[OpenedTxnResponse] = {
    asOpenedTxn(GenericOpenedTxnResponse(statements, _, _, _))
  }
}

/**
  * The error code and message sent by the REST API.
 *
  * @param code the status code parsed from the response
  * @param message the debug message to help find the problem with the request
  */
private[client] case class RawError(code: Neo4jStatusCode, message: String) {
  def asNeo4jError: Neo4jError = new Neo4jError(code, message)
}

private[client] object RawError {
  // Implicitly available as
  implicit def asStatementResultStats(raw: RawError): Neo4jError = raw.asNeo4jError
}

/**
  * The transaction info about the open transaction.
  */
private[client] case class RawTxnInfo(expires: DateTime)

/**
  * Stats about the result of running a single [[CypherStatement]].
  */
private[client] case class RawResultStats(
  contains_updates: Boolean,
  nodes_created: Int,
  nodes_deleted: Int,
  properties_set: Int,
  relationships_created: Int,
  relationship_deleted: Int,
  labels_added: Int,
  labels_removed: Int,
  indexes_added: Int,
  indexes_removed: Int,
  constraints_added: Int,
  constraints_removed: Int
) {
  def asStatementResultStats: StatementResultStats = {
    StatementResultStats(
      containsUpdates = contains_updates,
      nodesCreated = nodes_created,
      nodesDeleted = nodes_deleted,
      propertiesSet = properties_set,
      relationshipsCreated = relationships_created,
      relationshipDeleted = relationship_deleted,
      labelsAdded = labels_added,
      labelsRemoved = labels_removed,
      indexesAdded = indexes_added,
      indexesRemoved = indexes_removed,
      constraintsAdded = constraints_added,
      constraintsRemoved = constraints_removed
    )
  }
}

private[client] object RawResultStats {
  // Implicitly available as
  implicit def asStatementResultStats(raw: RawResultStats): StatementResultStats = raw.asStatementResultStats
}

/**
  * The result of running a single [[CypherStatement]]
  */
private[client] case class RawStatementResult(
  columns: Seq[String],
  data: Seq[RawStatementRow],
  stats: Option[RawResultStats]
) {

  def asStatementResult: StatementResult = {
    new StatementResult(columns.zipWithIndex.toMap, data.map(_.row.toIndexedSeq), stats.map(_.asStatementResultStats))
  }
}
private[client] object RawStatementResult {

  // Implicitly available as
  implicit def asStatementResult(raw: RawStatementResult): StatementResult = raw.asStatementResult
}

/**
  * A single row of a [[RawStatementResult]]
  */
private[client] case class RawStatementRow(row: Seq[JsValue])
