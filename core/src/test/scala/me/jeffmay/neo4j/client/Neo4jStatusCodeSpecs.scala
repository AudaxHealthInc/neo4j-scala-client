package me.jeffmay.neo4j.client

import org.scalatest.{Matchers, WordSpec}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scalacheck._

class Neo4jStatusCodeSpecs extends WordSpec
  with Matchers
  with GeneratorDrivenPropertyChecks {

  def validateIncludesOnlyOnce(code: String, groupName: String): Unit = {
    val groupNameWithDot = groupName + '.'
    code should include (groupNameWithDot)
    withClue(s"'$groupNameWithDot' appears more than once in $code") {
      code.indexOf(groupNameWithDot) should be(code.lastIndexOf(groupNameWithDot))
    }
  }

  "StatusCodes" should {

    "not be empty" in {
      assert(StatusCodes.Neo.statusCodes.nonEmpty)
    }

    "have the right size" in {
      val numberOfStatusCodes = StatusCodes.Neo.statusCodes.size
      assert(numberOfStatusCodes == 73)
    }

    "find all statuses by code" in {
      forAll() { (status: Neo4jStatusCode) =>
        val found = Neo4jStatusCode.findByCodeOrThrow(status.code)
        found should equal (status)
      }
    }

    "include the category in their name only once" in {
      forAll() { (status: Neo4jStatusCode) =>
        validateIncludesOnlyOnce(status.code, status.parent.name)
      }
    }

    "include the classification in their name only once" in {
      forAll() { (status: Neo4jStatusCode) =>
        validateIncludesOnlyOnce(status.code, status.parent.parent.name)
      }
    }

    "include 'Neo' in their name only once" in {
      forAll() { (status: Neo4jStatusCode) =>
        validateIncludesOnlyOnce(status.code, StatusCodes.Neo.name)
      }
    }
  }

}
