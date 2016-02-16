package me.jeffmay.neo4j.client

import me.jeffmay.neo4j.client.cypher.CypherStatement

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait Neo4jClient {

  def passwordChangeRequired(): Future[Boolean]

  def changePassword(newPassword: String): Future[Unit]

  def openAndCommitTxn(statement: CypherStatement): Future[SingleCommittedTxnResponse]
  def openAndCommitTxn(statements: Seq[CypherStatement]): Future[CommittedTxnResponse]

  def openTxn(): Future[OpenedTxnResponse]
  def openTxn(statement: CypherStatement): Future[SingleOpenedTxnResponse]
  def openTxn(statements: Seq[CypherStatement]): Future[OpenedTxnResponse]

  def commitTxn(ref: TxnRef, alongWith: Seq[CypherStatement] = Seq.empty): Future[CommittedTxnResponse]

  def withStatsIncludedByDefault(includeStatsByDefault: Boolean): Neo4jClient

  def withBaseUrl(baseUrl: String): Neo4jClient

  def withCredentials(username: String, password: String): Neo4jClient

  def withTimeout(timeout: FiniteDuration): Neo4jClient
}
