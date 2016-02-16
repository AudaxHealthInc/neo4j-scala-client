package me.jeffmay.neo4j.client.cypher.scalacheck

import me.jeffmay.neo4j.client.cypher._
import org.scalacheck.Shrink.shrink
import org.scalacheck.{Arbitrary, Gen, Shrink}

import scala.language.implicitConversions

/**
  * Extend or import from these generators to get
  */
object CypherValueGenerators extends CypherValueGenerators
trait CypherValueGenerators {

  implicit def arbCypherByte(implicit arbByte: Arbitrary[Byte]): Arbitrary[CypherByte] = Arbitrary {
    arbByte.arbitrary.map(CypherByte(_))
  }

  implicit def arbCypherChar(implicit arbChar: Arbitrary[Char]): Arbitrary[CypherChar] = Arbitrary {
    arbChar.arbitrary.map(CypherChar(_))
  }

  implicit def arbCypherString(implicit arbString: Arbitrary[String]): Arbitrary[CypherString] = Arbitrary {
    arbString.arbitrary.map(CypherString(_))
  }

  implicit val arbCypherBoolean: Arbitrary[CypherBoolean] = Arbitrary {
    Gen.oneOf(true, false).map(CypherBoolean(_))
  }

  implicit def arbCypherShort(implicit arbShort: Arbitrary[Short]): Arbitrary[CypherShort] = Arbitrary {
    arbShort.arbitrary.map(CypherShort(_))
  }

  implicit def arbCypherInt(implicit arbInt: Arbitrary[Int]): Arbitrary[CypherInt] = Arbitrary {
    arbInt.arbitrary.map(CypherInt(_))
  }

  implicit def arbCypherLong(implicit arbLong: Arbitrary[Long]): Arbitrary[CypherLong] = Arbitrary {
    arbLong.arbitrary.map(CypherLong(_))
  }

  implicit def arbCypherFloat(implicit arbFloat: Arbitrary[Float]): Arbitrary[CypherFloat] = Arbitrary {
    arbFloat.arbitrary.map(CypherFloat(_))
  }

  implicit def arbCypherDouble(implicit arbDouble: Arbitrary[Double]): Arbitrary[CypherDouble] = Arbitrary {
    arbDouble.arbitrary.map(CypherDouble(_))
  }

  implicit def arbCypherArrayOfType[T <: CypherPrimitive: NotMixed](
    implicit arbT: Arbitrary[T]): Arbitrary[CypherArray[T]] = Arbitrary {
    Gen.listOf(arbT.arbitrary).map(values => CypherArray(values))
  }

  implicit lazy val arbCypherArray: Arbitrary[CypherArray[CypherPrimitive]] = Arbitrary(genCypherArray)

  implicit lazy val arbCypherPrimitive: Arbitrary[CypherPrimitive] = Arbitrary(genCypherPrimitive)

  implicit lazy val arbCypherValue: Arbitrary[CypherValue] = Arbitrary(genCypherValue)

  lazy val allPrimitiveGens: Seq[Gen[CypherPrimitive]] = {
    Seq(
      arbCypherByte.arbitrary,
      arbCypherChar.arbitrary,
      arbCypherString.arbitrary,
      arbCypherBoolean.arbitrary,
      arbCypherShort.arbitrary,
      arbCypherInt.arbitrary,
      arbCypherLong.arbitrary,
      arbCypherFloat.arbitrary,
      arbCypherDouble.arbitrary
    )
  }

  /**
    * Generates [[CypherPrimitive]]s (ie. not [[CypherArray]] or [[CypherProps]]).
    */
  lazy val genCypherPrimitive: Gen[CypherPrimitive] = {
    Gen.oneOf(
      allPrimitiveGens.head,
      allPrimitiveGens.tail.head,
      allPrimitiveGens.tail.tail: _*
    )
  }

  def genCypherArrayOfType[T <: CypherPrimitive: NotMixed](implicit arbT: Arbitrary[T]): Gen[CypherArray[T]] = {
    genCypherArrayOfType[T](arbT.arbitrary)
  }

  def genCypherArrayOfType[T <: CypherPrimitive: NotMixed](tGen: Gen[T]): Gen[CypherArray[T]] = {
    Gen.listOf(tGen).map(CypherArray(_))
  }

  def genCypherArrayOfN(n: Int): Gen[CypherArray[CypherPrimitive]] = {
    // Safe to ignore mixing restriction because we generate the list from a generator of pure single-type values
    val primArrayGens = allPrimitiveGens.map(g => Gen.listOfN(n, g).map(CypherArray(_)(mixed = null)))
    Gen.oneOf(
      primArrayGens.head,
      primArrayGens.tail.head,
      primArrayGens.tail.tail: _*
    )
  }

  lazy val genCypherArray: Gen[CypherArray[CypherPrimitive]] = Gen.sized(genCypherArrayOfN)

  lazy val genCypherValue: Gen[CypherValue] = {
    Gen.oneOf(genCypherPrimitive, genCypherArray)
  }

  // Shrinks for better error output

  implicit def shrinkCypherArray[T <: CypherPrimitive: Shrink]: Shrink[CypherArray[T]] = Shrink {
    arr =>
      // Can't be mixed type when removing items
      val stream: Stream[CypherArray[T]] = shrink(arr.value) map { CypherArray(_)(mixed = null) }
      stream
  }

  implicit lazy val shrinkCypherString: Shrink[CypherString] = Shrink(v => shrink[String](v.value).map(CypherString(_)))

  implicit lazy val shrinkCypherShort: Shrink[CypherShort] = Shrink(v => shrink[Short](v.value).map(CypherShort(_)))

  implicit lazy val shrinkCypherInt: Shrink[CypherInt] = Shrink(v => shrink[Int](v.value).map(CypherInt(_)))

  implicit lazy val shrinkCypherLong: Shrink[CypherLong] = Shrink(v => shrink[Long](v.value).map(CypherLong(_)))

  implicit lazy val shrinkCypherFloat: Shrink[CypherFloat] = Shrink(v => shrink[Float](v.value).map(CypherFloat(_)))

  implicit lazy val shrinkCypherDouble: Shrink[CypherDouble] = Shrink(v => shrink[Double](v.value).map(CypherDouble(_)))

}

