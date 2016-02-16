package me.jeffmay.neo4j.client.cypher.scalacheck

import me.jeffmay.neo4j.client.cypher._
import org.scalacheck.{Shrink, Gen}

object CypherStatementGenerators extends CypherStatementGenerators
trait CypherStatementGenerators extends CypherValueGenerators {

  /**
    * Generates a valid field name where the first character is alphabetical and the remaining chars
    * are alphanumeric.
    */
  def genFieldName: Gen[String] = Gen.identifier

  def genFields: Gen[(String, CypherValue)] = Gen.zip(genFieldName, genCypherValue)

  /**
    * Generates a nested array at the specified depth and width.
    */
  lazy val genCypherProps: Gen[CypherProps] = {
    for {
      fields <- Gen.listOf(genFields)
    } yield Map(fields: _*)
  }

  /**
    * Generates the parameters of a [[CypherStatement]].
    */
  lazy val genCypherParams: Gen[CypherParams] = {
    Gen.mapOf(Gen.zip(
      Gen.identifier,
      Gen.mapOf(Gen.zip(
        Gen.identifier,
        genCypherValue
      ))
    ))
  }

  /**
    * Avoids shrinking property names to empty strings, since these would be invalid.
    *
    * @note the explicit cases of handling empty property names is tested within this library,
    *       so you shouldn't need to generate invalid inputs.
    */
  implicit val shrinkCypherProps: Shrink[CypherProps] = Shrink { params =>
    implicit val shrinkIdentifier = Shrink[String](id => Shrink.shrink[String](id).filterNot(_.isEmpty))
    Shrink.shrinkContainer2[Map, String, CypherValue].shrink(params)
  }

  /**
    * Avoids shrinking parameter object names to empty strings, since these would be invalid.
    *
    * @note the explicit cases of handling empty parameter object names is tested within this library,
    *       so you shouldn't need to generate invalid inputs.
    */
  implicit val shrinkCypherParams: Shrink[CypherParams] = Shrink { params =>
    implicit val shrinkIdentifier = Shrink[String](id => Shrink.shrink[String](id).filterNot(_.isEmpty))
    Shrink.shrinkContainer2[Map, String, CypherProps].shrink(params)
  }
}
