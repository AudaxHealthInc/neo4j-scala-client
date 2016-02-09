package me.jeffmay.neo4j.client.cypher

import org.scalatest.WordSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scalacheck._

class CypherStringContextSpec extends WordSpec
  with GeneratorDrivenPropertyChecks {

  "CypherStringContext" should {

    "allow param substitution in-place" in {
      val props = Cypher.params("props")
      val stmt = cypher"MATCH (n { id: ${props.id(1)}, name: ${props.name("x")} })"
      val expectedTemplate = "MATCH (n { id: {props}.id, name: {props}.name })"
      assertResult(expectedTemplate, "template did not render properly") {
        stmt.template
      }
      val expectedProps = Cypher.props("id" -> 1, "name" -> "x")
      assertResult(Map("props" -> expectedProps)) {
        stmt.parameters
      }
    }

    "allow param substitution for any known type" in {
      forAll() { (value: CypherValue) =>
        val props = Cypher.params("props")
        val stmt = cypher"MATCH (n { id: ${props.id(value)} })"
        val expectedTemplate = "MATCH (n { id: {props}.id })"
        assertResult(expectedTemplate, "template did not render properly") {
          stmt.template
        }
        val expectedProps = Cypher.props("id" -> value)
        assertResult(Map("props" -> expectedProps)) {
          stmt.parameters
        }
      }
    }

    "allow literal substitution for labels" in {
      val stmt = cypher"MATCH (n ${Cypher.label("EXPECTED")}) RETURN n"
      val expectedTemplate = "MATCH (n :EXPECTED) RETURN n"
      assertResult(expectedTemplate, "template did not render properly") {
        stmt.template
      }
      assertResult(Map.empty) {
        stmt.parameters
      }
    }

    "throw an exception with all invalid cypher arguments included as suppressed exceptions" in {
      val invalid = Cypher.label(":INVALID")
      val ex = intercept[InvalidCypherException](cypher"MATCH (n $invalid) RETURN n")
      assertResult(Seq(invalid)) {
        ex.causes
      }
      assertResult(ex.causes.head.exception) {
        ex.getSuppressed.head
      }
    }
  }
}
