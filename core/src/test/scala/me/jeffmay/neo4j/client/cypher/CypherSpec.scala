package me.jeffmay.neo4j.client.cypher

import org.scalacheck.Gen
import org.scalatest.WordSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class CypherSpec extends WordSpec
  with GeneratorDrivenPropertyChecks {

  val genLabelLike: Gen[String] = Gen.containerOf[Array, Char](
    Gen.oneOf(Gen.alphaChar, Gen.numChar)
  ).map(new String(_))

  "Cypher.label" should {

    "Not allow empty string" in {
      assert(Cypher.label("").isInvalid)
    }

    "Not allow back ticks" in {
      assert(Cypher.label("`a`").isInvalid)
    }

    "Not allow colon" in {
      assert(Cypher.label(":a").isInvalid)
    }

    "Allow all alpha-numeric characters and look like the given string prefixed with colon" in {
      forAll(genLabelLike) { (s: String) =>
        whenever(!s.isEmpty) {
          assertResult(s":$s") {
            Cypher.label(s).getOrThrow.template
          }
        }
      }
    }
  }

  "Cypher.ident" should {

    "Not allow empty string" in {
      assert(Cypher.ident("").isInvalid)
    }

    "Not allow back ticks" in {
      assert(Cypher.ident("`a`").isInvalid)
    }

    "Not allow colon" in {
      assert(Cypher.ident(":a").isInvalid)
    }

    "Allow all alpha-numeric characters and look like the given string" in {
      forAll(genLabelLike) { (s: String) =>
        whenever(!s.isEmpty && s.charAt(0).isLetter) {
          assertResult(s) {
            Cypher.ident(s).getOrThrow.template
          }
        }
      }
    }
  }

  "Cypher.param" should {

    "Not allow empty string" in {
      val ex = intercept[IllegalArgumentException] {
        Cypher.expand(Cypher.param("", Map.empty))
      }
      assert(ex.getMessage contains "empty")
    }
  }

  "Cypher.expand" should {

    "Construct the expected fragment for empty props" in {
      val fragmentParam = Cypher.param("ns", Cypher.props())
      val fragment = Cypher.expand(fragmentParam)
      assertResult("")(fragment.template)
      assertResult(fragmentParam.toParams) {
        fragment.statement.parameters
      }
    }

    "Construct the expected fragment for a single prop" in {
      val fragmentParam = Cypher.param("ns", Cypher.props("id" -> 1))
      val fragment = Cypher.expand(fragmentParam)
      assertResult("id: {ns}.id")(fragment.template)
      assertResult(fragmentParam.toParams) {
        fragment.statement.parameters
      }
    }

    "Construct the expected fragment for a multiple props" in {
      val fragmentParam = Cypher.param("ns", Cypher.props("id" -> 1, "name" -> "example"))
      val fragment = Cypher.expand(fragmentParam)
      assertResult("id: {ns}.id, name: {ns}.name")(fragment.template)
      assertResult(fragmentParam.toParams) {
        fragment.statement.parameters
      }
    }

    "Construct the expected fragment for a multiple props with indent" in {
      val fragmentParam = Cypher.param("ns", Cypher.props("id" -> 1, "name" -> "example"))
      val fragment = Cypher.expand(fragmentParam, chopAndIndent = Some(2))
      assertResult("id: {ns}.id,\n  name: {ns}.name")(fragment.template)
      assertResult(fragmentParam.toParams) {
        fragment.statement.parameters
      }
    }
  }

}
