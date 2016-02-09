package me.jeffmay.neo4j.client

import me.jeffmay.neo4j.client.cypher.Statement

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

trait Neo4jClient {

  def passwordChangeRequired(): Future[Boolean]

  def changePassword(newPassword: String): Future[Unit]

  // TODO: Use a single statement return type
  def openAndCommitTxn(first: Statement, others: Statement*): Future[CommittedTxnResponse]

  // TODO: Use a single statement return type
  def openTxn(statements: Statement*): Future[OpenedTxnResponse]

  // TODO: Use a single statement return type
  def commitTxn(ref: TxnRef, alongWith: Statement*): Future[CommittedTxnResponse]

  def withStatsIncludedByDefault(includeStatsByDefault: Boolean): Neo4jClient

  def withBaseUrl(baseUrl: String): Neo4jClient

  def withCredentials(username: String, password: String): Neo4jClient

  def withTimeout(timeout: FiniteDuration): Neo4jClient
}
