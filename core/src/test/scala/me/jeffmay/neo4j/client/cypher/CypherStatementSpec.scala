package me.jeffmay.neo4j.client.cypher

import org.scalatest.WordSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks._
import scalacheck._

class CypherStatementSpec extends WordSpec {

  "CypherStatement" when {

    "interpolated" should {

      "allowed merging parameters that share the same namespace and key and they have the same value" in {
        val props = Cypher.params("props")
        val value = props.ref(1)
        assertResult(CypherStatement(
          "MATCH (n { ref: {props}.ref }) --> (m { ref: {props}.ref })",
          Map("props" -> Cypher.props("ref" -> 1))
        )) {
          cypher"MATCH (n { ref: $value }) --> (m { ref: $value })"
        }
      }

      "throw an exception when statement parameters share the same namespace and key but with different values" in {
        val props = Cypher.params("props")
        val ex = intercept[ConflictingParameterFieldsException] {
          cypher"MATCH (n { ref: ${props.ref(1)} }) --> (m { ref: ${props.ref(2)} })"
        }
        assertResult(Seq(CypherInt(1), CypherInt(2)))(ex.conflictingValues)
      }
    }

    "concatenated" should {

      "append raw template strings" in {
        assertResult(cypher"ab") {
          cypher"a" :+: cypher"b"
        }
      }

      "append a string literal to the template" in {
        assertResult(cypher"ab") {
          cypher"a" + "b"
        }
      }

      "allowed merging parameters that share the same namespace and key and they have the same value" in {
        val props = Cypher.params("props")
        val value = props.ref(1)
        assertResult(CypherStatement(
          "MATCH (n { ref: {props}.ref }) --> (m { ref: {props}.ref })",
          Map("props" -> Cypher.props("ref" -> 1))
        )) {
          cypher"MATCH (n { ref: $value })" :+: cypher" --> (m { ref: $value })"
        }
      }

      "throw an exception when statement parameters share the same namespace and key but with different values" in {
        val props = Cypher.params("props")
        val ex = intercept[ConflictingParameterFieldsException] {
          cypher"MATCH (n { ref: ${props.ref(1)} })" :+: cypher" --> (m { ref: ${props.ref(2)} })"
        }
        assertResult(Seq(CypherInt(1), CypherInt(2)))(ex.conflictingValues)
      }

      "merge all properties given" in {
        forAll(genCypherParams) { (params: CypherParams) =>
          val expectedParams = params.filter(_._2.nonEmpty)
          val result = params.foldLeft(cypher"") {
            case (acc, (ns, propValues)) =>
              val props = Cypher.params(ns)
              propValues.foldLeft(acc) {
                case (c, (k, v)) => c :+: cypher"${props.applyDynamic(k)(v)}"
              }
          }
          assertResult(expectedParams)(result.parameters)
        }
      }
    }
  }
}
