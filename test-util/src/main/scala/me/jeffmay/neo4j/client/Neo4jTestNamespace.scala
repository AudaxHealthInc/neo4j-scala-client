package me.jeffmay.neo4j.client

import me.jeffmay.neo4j.client.cypher.{Cypher, CypherStatement}
import me.jeffmay.util.Namespace

import scala.concurrent.{ExecutionContext, Future}

object Neo4jTestNamespace {

  def cleanup(namespace: Namespace, client: Neo4jClient)(implicit ec: ExecutionContext): Future[Any] = {
    client.openAndCommitTxn(
      CypherStatement("MATCH (n { ns: {props}.ns }) OPTIONAL MATCH (n)-[r]-() DELETE n, r",
        Map("props" -> Cypher.props("ns" -> namespace.value))
      )
    )
  }

  def cleanupBefore[T](action: Future[T], namespace: Namespace, client: Neo4jClient)
    (implicit ec: ExecutionContext): Future[T] = {
    cleanup(namespace, client).flatMap(_ => action)
  }

  def cleanupAfter[T](action: Future[T], namespace: Namespace, client: Neo4jClient)
    (implicit ec: ExecutionContext): Future[T] = {
    if (sys.props.get("NEO4J_TEST_CLEANUP_AFTER") contains false.toString) {
      action
    }
    else {
      action.andThen {
        case _ => cleanup(namespace, client)
      }
    }
  }

}
