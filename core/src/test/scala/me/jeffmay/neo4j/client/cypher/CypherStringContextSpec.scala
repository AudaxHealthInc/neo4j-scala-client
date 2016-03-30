package me.jeffmay.neo4j.client.cypher

import me.jeffmay.neo4j.client.cypher.scalacheck.CypherGenerators._
import org.scalatest.WordSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class CypherStringContextSpec extends WordSpec
  with GeneratorDrivenPropertyChecks {

  "CypherStringContext" should {

    "allow param substitution in-place" in {
      val props = Cypher.param("props")
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
        val props = Cypher.param("props")
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
      val props = Cypher.param("props")
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
      val props = Cypher.param("props", expectedProps)
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
      val props = Cypher.obj(Cypher.param("props", expectedProps))
      val stmt = cypher"MATCH (n $props)"
      assertResult(s"MATCH (n { ${props.namespace} })", "template did not render properly") {
        stmt.template
      }
      assertResult(Map("props" -> expectedProps)) {
        stmt.parameters
      }
    }

    "allow inserting an object for any known cypher props" in {
      forAll() { (props: CypherProps) =>
        val obj = Cypher.obj(Cypher.param("props", props))
        val stmt = cypher"MATCH (n $obj)"
        assertResult(s"MATCH (n { ${obj.namespace} })", "template did not render properly") {
          stmt.template
        }
        assertResult(Map("props" -> props)) {
          stmt.parameters
        }
      }
    }

    "throw an exception when given two different objects for the same namespace" in {
      val conflictingNamespace = "props"
      val obj1 = Cypher.obj(Cypher.param(conflictingNamespace, Cypher.props("id" -> 1, "name" -> "x")))
      val obj2 = Cypher.obj(Cypher.param(conflictingNamespace, Cypher.props("id" -> 2, "name" -> "y")))
      val ex = intercept[ConflictingParameterObjectsException] {
        cypher"MATCH (n $obj1) --> (m $obj2)"
      }
      assertResult(
        s"MATCH (n { $conflictingNamespace }) --> (m { $conflictingNamespace })",
        "template did not render properly") {
        ex.template
      }
      assertResult(Seq(obj1.props, obj2.props)) {
        ex.conflictingProps
      }
    }

    "throw an exception mixing mutable params with an object that has the same namespace in the same statement" in {
      val conflictingNamespace = "props"
      val obj = Cypher.obj(Cypher.param(conflictingNamespace, Cypher.props("id" -> 1, "name" -> "x")))
      val conflicting = Cypher.param(conflictingNamespace)
      val conflictingKey = "anything"
      val conflictingValue = "doesn't matter"
      val ex = intercept[MixedParamReferenceException] {
        cypher"MATCH (n $obj) --> (m { id: ${conflicting.applyDynamic(conflictingKey)(conflictingValue)} })"
      }
      assertResult(
        s"MATCH (n { $conflictingNamespace }) --> (m { id: {$conflictingNamespace}.$conflictingKey })",
        "template did not render properly") {
        ex.template
      }
      assertResult(Set(conflictingKey)) {
        ex.conflictingFieldReferences
      }
    }

    "allow literal substitution for identifiers" in {
      val id = "ident"
      val ident = Cypher.ident(id)
      val stmt = cypher"MATCH ($ident) RETURN $ident"
      assertResult(s"MATCH ($id) RETURN $id", "template did not render properly") {
        stmt.template
      }
      assertResult(Map.empty) {
        stmt.parameters
      }
    }

    "allow literal substitution for params" in {
      val id = "params"
      val ident = Cypher.param(id)
      val stmt = cypher"MATCH ($ident) RETURN $ident"
      assertResult(s"MATCH ($id) RETURN $id", "template did not render properly") {
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
      val ex = intercept[CypherResultException](cypher"MATCH (n $invalid) RETURN n")
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
