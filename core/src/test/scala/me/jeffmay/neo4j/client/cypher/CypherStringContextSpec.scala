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
      assertResult(
        s"MATCH (n { id: {${props.namespace}}.id, name: {${props.namespace}}.name })",
        "template did not render properly") {
        stmt.template
      }
      val expectedProps = Cypher.props("id" -> 1, "name" -> "x")
      assertResult(Map("props" -> expectedProps)) {
        stmt.parameters
      }
    }

    "allow param substitution for any CypherValue" in {
      forAll() { (value: CypherValue) =>
        val props = Cypher.params("props")
        val stmt = cypher"MATCH (n { id: ${props.id(value)} })"
        assertResult(s"MATCH (n { id: {${props.namespace}}.id })", "template did not render properly") {
          stmt.template
        }
        val expectedProps = Cypher.props("id" -> value)
        assertResult(Map("props" -> expectedProps)) {
          stmt.parameters
        }
      }
    }

    "allow param substitution for a writeable type" in {
      import ExampleWriteable._
      val value = ExampleWriteable("value")
      val props = Cypher.params("props")
      val stmt = cypher"MATCH (n { id: ${props.id(value)} })"
      assertResult(s"MATCH (n { id: {${props.namespace}}.id })", "template did not render properly") {
        stmt.template
      }
      val expectedProps = Cypher.props("id" -> value)
      assertResult(Map("props" -> expectedProps)) {
        stmt.parameters
      }
    }

    "allow param substitution for a Traversable of writeable type values" in {
      val values = Seq(1, 2, 3).map(n => ExampleWriteable(n.toString))
      val expectedProps = Cypher.props("values" -> values)
      val props = Cypher.params("props", expectedProps)
      val stmt = cypher"MATCH (n) WHERE n IN ${props.values}"
      assertResult(s"MATCH (n) WHERE n IN {${props.namespace}}.values", "template did not render properly") {
        stmt.template
      }
      assertResult(Map("props" -> expectedProps)) {
        stmt.parameters
      }
    }

    "allow property object substitution" in {
      val expectedProps = Cypher.props("id" -> 1, "name" -> "x")
      val props = Cypher.params("props", expectedProps)
      val stmt = cypher"MATCH (n $props)"
      assertResult(s"MATCH (n { ${props.namespace} })", "template did not render properly") {
        stmt.template
      }
      assertResult(Map("props" -> expectedProps)) {
        stmt.parameters
      }
    }

    "allow property substitution for any known cypher props" in {
      forAll() { (obj: CypherProps) =>
        val props = Cypher.params("props", obj)
        val stmt = cypher"MATCH (n $props)"
        assertResult(s"MATCH (n { ${props.namespace} })", "template did not render properly") {
          stmt.template
        }
        assertResult(Map("props" -> obj)) {
          stmt.parameters
        }
      }
    }

    "throw an exception when given two different objects for the same namespace" in {
      val conflictingNamespace = "props"
      val obj1 = Cypher.params(conflictingNamespace, Cypher.props("id" -> 1, "name" -> "x"))
      val obj2 = Cypher.params(conflictingNamespace, Cypher.props("id" -> 2, "name" -> "y"))
      val ex = intercept[ConflictingParameterObjectsException] {
        cypher"MATCH (n $obj1) --> (m $obj2)"
      }
      assertResult(
        s"MATCH (n { $conflictingNamespace }) --> (m { $conflictingNamespace })",
        "template did not render properly") {
        ex.template
      }
      assertResult(Seq(obj1.props, obj2.props)) {
        ex.conflictingParams
      }
    }

    "throw an exception mixing any mutable and immutable params with the same namespace in the same statement" in {
      val conflictingNamespace = "props"
      val obj = Cypher.params(conflictingNamespace, Cypher.props("id" -> 1, "name" -> "x"))
      val conflicting = Cypher.params(conflictingNamespace)
      val conflictingKey = "anything"
      val conflictingValue = "doesn't matter"
      val ex = intercept[MutatedParameterObjectException] {
        cypher"MATCH (n $obj) --> (m { id: ${conflicting.applyDynamic(conflictingKey)(conflictingValue)} })"
      }
      assertResult(
        s"MATCH (n { $conflictingNamespace }) --> (m { id: {$conflictingNamespace}.$conflictingKey })",
        "template did not render properly") {
        ex.template
      }
      assertResult(Seq(obj.props, Cypher.props(conflictingKey -> conflictingValue))) {
        ex.conflictingParams
      }
    }

    "allow literal substitution for identifiers" in {
      val n = "n"
      val nIdent = Cypher.ident("n")
      val stmt = cypher"MATCH ($nIdent) RETURN $nIdent"
      assertResult(s"MATCH ($n) RETURN $n", "template did not render properly") {
        stmt.template
      }
      assertResult(Map.empty) {
        stmt.parameters
      }
    }

    "allow literal substitution for labels" in {
      val labelName = "EXPECTED"
      val stmt = cypher"MATCH (n ${Cypher.label(labelName)}) RETURN n"
      assertResult(s"MATCH (n :$labelName) RETURN n", "template did not render properly") {
        stmt.template
      }
      assertResult(Map.empty) {
        stmt.parameters
      }
    }

    "throw an exception with all invalid cypher arguments included as suppressed exceptions" in {
      val invalidLabelName = ":INVALID"
      val invalid = Cypher.label(invalidLabelName)
      val ex = intercept[InvalidCypherException](cypher"MATCH (n $invalid) RETURN n")
      assertResult(Some("MATCH (n |INVALID[1]|) RETURN n")) {
        ex.template
      }
      assertResult(Seq(invalid)) {
        ex.causes
      }
      assertResult(ex.causes.head.exception) {
        ex.getSuppressed.head
      }
    }
  }
}
