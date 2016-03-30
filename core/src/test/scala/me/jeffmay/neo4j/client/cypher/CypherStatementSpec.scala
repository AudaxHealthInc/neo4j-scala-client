package me.jeffmay.neo4j.client.cypher

import me.jeffmay.neo4j.client.cypher.scalacheck.CypherGenerators._
import org.scalatest.WordSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks._

class CypherStatementSpec extends WordSpec {

  "CypherStatement" when {

    "interpolated" should {

      "allowed merging parameters that share the same namespace and key and they have the same value" in {
        val props = Cypher.param("props")
        val value = props.ref(1)
        assertResult(CypherStatement(
          "MATCH (n { ref: {props}.ref }) --> (m { ref: {props}.ref })",
          Map("props" -> Cypher.props("ref" -> 1))
        )) {
          cypher"MATCH (n { ref: $value }) --> (m { ref: $value })"
        }
      }

      "throw an exception when statement parameters share the same namespace and key but with different values" in {
        val props = Cypher.param("props")
        val ex = intercept[ConflictingParameterFieldsException] {
          cypher"MATCH (n { ref: ${props.ref(1)} }) --> (m { ref: ${props.ref(2)} })"
        }
        assertResult(Seq(CypherInt(1), CypherInt(2)))(ex.conflictingFields.values.head)
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
        val props = Cypher.param("props")
        val value = props.ref(1)
        assertResult(CypherStatement(
          "MATCH (n { ref: {props}.ref }) --> (m { ref: {props}.ref })",
          Map("props" -> Cypher.props("ref" -> 1))
        )) {
          cypher"MATCH (n { ref: $value })" :+: cypher" --> (m { ref: $value })"
        }
      }

      "throw an exception when statement parameters share the same namespace and key but with different values" in {
        val props = Cypher.param("props")
        val ex = intercept[ConflictingParameterFieldsException] {
          cypher"MATCH (n { ref: ${props.ref(1)} })" :+: cypher" --> (m { ref: ${props.ref(2)} })"
        }
        assertResult(Seq(CypherInt(1), CypherInt(2)))(ex.conflictingFields.values.head)
      }

      "merge all properties given" in {
        forAll(genCypherParams) { (params: CypherParams) =>
          val expectedParams = params.filter(_._2.nonEmpty)
          val result = params.foldLeft(cypher"") {
            case (acc, (ns, propValues)) =>
              val props = Cypher.param(ns)
              propValues.foldLeft(acc) {
                case (c, (k, v)) => c :+: cypher"${props.applyDynamic(k)(v)}"
              }
          }
          assertResult(expectedParams)(result.parameters)
        }
      }
    }

    "parseParams" should {

      "parse a template with an embedded field param" in {
        assertResult(Seq("props" -> Some("name"))) {
          CypherStatement.parseParams("MATCH (n { id: 1, name: {props}.name })")
        }
      }

      "parse a template with an embedded object param" in {
        assertResult(Seq("props" -> None)) {
          CypherStatement.parseParams("CREATE (m { props })")
        }
      }

      "parse a template with both embedded field and object params" in {
        assertResult(Seq("props" -> Some("name"), "props" -> None)) {
          CypherStatement.parseParams("MATCH (n { id: 1, name: {props}.name }); CREATE (m { props })")
        }
      }

      "parse a template empty property selectors" in {
        assertResult(Seq()) {
          CypherStatement.parseParams("MATCH (n {})")
        }
      }

      "parse a template with only the start of a param" in {
        assertResult(Seq()) {
          CypherStatement.parseParams("MATCH (n { id: 1, name: {props")
        }
      }

      "parse a template with only the end of a param" in {
        assertResult(Seq()) {
          CypherStatement.parseParams("}.name })")
        }
      }
    }

    "validate()" should {

      "NOT throw an exception when parameter field is present" in {
        val template = "MATCH (n { id: 1, name: {props}.name })"
        CypherStatement(template, Map("props" -> Cypher.props("name" -> "field"))).validate()
      }

      "throw an exception for missing parameter by field" in {
        val template = "MATCH (n { id: {props}.id, name: {props}.name })"
        val exception = intercept[MissingParamReferenceException] {
          CypherStatement(template, Map("props" -> Cypher.props("id" -> 1))).validate()
        }
        assertResult("props")(exception.namespace)
        assertResult(Set("name"))(exception.missingFieldReferences)
        assertResult(template)(exception.statement.template)
      }

      "NOT throw an exception for parameter object is present" in {
        val template = "CREATE (m { mProps })"
        CypherStatement(template, Map("mProps" -> Cypher.props())).validate()
      }

      "throw an exception for missing parameter object" in {
        val template = "CREATE (m { mProps }); CREATE (n { nProps })"
        val exception = intercept[MissingParamReferenceException] {
          CypherStatement(template, Map("nProps" -> Cypher.props("id" -> 1))).validate()
        }
        assertResult("mProps")(exception.namespace)
        assertResult(template)(exception.statement.template)
      }
    }
  }
}
