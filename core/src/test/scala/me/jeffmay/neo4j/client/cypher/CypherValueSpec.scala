package me.jeffmay.neo4j.client.cypher

import me.jeffmay.neo4j.client.cypher.scalacheck.CypherGenerators._
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.{Succeeded, WordSpec}

class CypherValueSpec extends WordSpec
  with GeneratorDrivenPropertyChecks {

  "CypherArray" should {

    "not allow building mixed arrays" in {
      CypherArray(Seq(CypherInt(1), CypherInt(2))) // compiles
      assertDoesNotCompile("CypherArray(Seq(CypherInt(1), CypherBoolean(false)))")
    }

    "not generate mixed arrays" in {
      forAll() { arr: CypherArray[CypherPrimitive] =>
        arr.value.headOption match {
          case None =>
          case Some(first) =>
            val firstClass = first.value.getClass
            for ((next, i) <- arr.value.tail.zipWithIndex) {
              assertResult(firstClass, s"Value at index $i did not have expected value type of ${firstClass.getSimpleName}") {
                next.value.getClass
              }
            }
        }
        Succeeded
      }
    }
  }
}
