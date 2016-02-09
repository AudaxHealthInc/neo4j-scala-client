package me.jeffmay.neo4j.client.scalacheck

import me.jeffmay.neo4j.client.Neo4jStatusCode
import org.scalacheck.{Arbitrary, Gen}

object StatusCodeGenerators extends StatusCodeGenerators
trait StatusCodeGenerators {

  implicit val arbNeo4jStatusCode: Arbitrary[Neo4jStatusCode] = Arbitrary {
    Gen.oneOf(Neo4jStatusCode.directory.values.toSeq)
  }

  def genNeo4jStatusCode: Gen[Neo4jStatusCode] = arbNeo4jStatusCode.arbitrary
}
