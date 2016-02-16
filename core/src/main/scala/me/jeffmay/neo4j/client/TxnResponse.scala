package me.jeffmay.neo4j.client

import me.jeffmay.neo4j.client.cypher.CypherStatement
import org.joda.time.DateTime

/**
  * Represents the results of executing a batch of [[CypherStatement]]s.
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
    * The statements that initiated the transaction.
    */
  def statements: Seq[CypherStatement]

  /**
    * The results of executing the [[CypherStatement]]s returned in the same order as given.
    */
  def results: Seq[StatementResult]

  /**
    * All the errors encountered when executing the [[CypherStatement]]s within a transaction.
    */
  def errors: Seq[Neo4jError]

  /**
    * The transaction info about the open transaction info.
    */
  def openTxnInfo: Option[TxnInfo]
}

/**
  * Represents a nicer interface for a response to a single [[CypherStatement]].
  */
sealed trait SingleStatementTxnResponse extends TxnResponse {

  /**
    * The statement that initiated the transaction.
    */
  def statement: CypherStatement

  final override def statements: Seq[CypherStatement] = Seq(statement)

  /**
    * Returns either the Right of the [[StatementResult]] or a Left of the [[Neo4jError]]s
    */
  def result: Either[Seq[Neo4jError], StatementResult] = results match {
    case Seq() => Left(errors)
    case Seq(single) => Right(single)
    case many => throw new TooManyResultsException(statements, many)
  }
}

/**
  * Represents a transaction response that is still open.
  */
sealed trait OpenedTxnResponse extends TxnResponse {

  /**
    * The transaction info, needed to commit the transaction.
    */
  def transaction: TxnInfo

  final override def openTxnInfo: Option[TxnInfo] = Some(transaction)
}

/**
  * Represents an open transaction for a single statement.
  */
case class SingleOpenedTxnResponse(
  statement: CypherStatement,
  results: Seq[StatementResult],
  transaction: TxnInfo,
  errors: Seq[Neo4jError]
) extends OpenedTxnResponse
  with SingleStatementTxnResponse

/**
  * Represents an open transaction containing any number of statements
  */
case class GenericOpenedTxnResponse(
  statements: Seq[CypherStatement],
  results: Seq[StatementResult],
  transaction: TxnInfo,
  errors: Seq[Neo4jError]
) extends OpenedTxnResponse

/**
  * Represents a transaction response that has been committed and closed.
  */
sealed trait CommittedTxnResponse extends TxnResponse {
  final override def openTxnInfo: Option[TxnInfo] = None
}

/**
  * Represents a closed and committed transaction for a single statement.
  */
case class SingleCommittedTxnResponse(
  statement: CypherStatement,
  results: Seq[StatementResult],
  errors: Seq[Neo4jError]
) extends CommittedTxnResponse
  with SingleStatementTxnResponse

/**
  * Represents a closed and committed transaction containing any number of statements.
  */
case class GenericCommittedTxnResponse(
  statements: Seq[CypherStatement],
  results: Seq[StatementResult],
  errors: Seq[Neo4jError]
) extends CommittedTxnResponse

/**
  * Represents the transaction info
  *
  * @param txn the reference of the transaction
  * @param expires when the transaction expires
  */
case class TxnInfo(txn: TxnRef, expires: DateTime)
