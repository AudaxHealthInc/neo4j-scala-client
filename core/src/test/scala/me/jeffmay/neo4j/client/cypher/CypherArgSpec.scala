package me.jeffmay.neo4j.client.cypher

import org.scalacheck.Gen
import org.scalatest.WordSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class CypherArgSpec extends WordSpec
  with GeneratorDrivenPropertyChecks {

  val genLabelLike: Gen[String] = Gen.containerOf[Array, Char](
    Gen.oneOf(Gen.alphaChar, Gen.numChar)
  ).map(new String(_))

  "CypherLabel" should {

    "Not allow empty string" in {
      assert(Cypher.label("").isInvalid)
    }

    "Not allow back ticks" in {
      assert(Cypher.label("`a`").isInvalid)
    }

    "Not allow colon" in {
      assert(Cypher.label(":a").isInvalid)
    }

    "Allow all alpha-numeric characters" in {
      forAll(genLabelLike) { (s: String) =>
        whenever(!s.isEmpty) {
          assertResult(s":$s") {
            Cypher.label(s).getOrThrow.template
          }
        }
      }
    }
  }

}
