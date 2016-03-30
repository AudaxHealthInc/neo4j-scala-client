package me.jeffmay.neo4j.client.ws

import me.jeffmay.neo4j.client.cypher.CypherStatement
import me.jeffmay.neo4j.client.{StatusCodeException, StatusCodes}

class InvalidCypherQueriesSpec extends CommonWSNeo4jClientSpec {

  s"$ClientName should fail to execute a MATCH query with a parameter object" in { fixture =>
    import fixture._
    client.openAndCommitTxn(CypherStatement(
      s"MATCH (n { props })"
    )).map { rsp =>
      fail("Props object should not be allowed in MATCH statement")
    }.recover {
      case ex: StatusCodeException =>
        assertResult(StatusCodes.Neo.ClientError.Statement.InvalidSyntax)(ex.errors.head.status)
      case ex =>
        fail("Unexpected exception", ex)
    }
  }
}
