package me.jeffmay.neo4j.client.cypher

import scala.annotation.implicitNotFound

/**
  * A serializer for converting any given type into either a specific subclass of [[CypherValue]],
  * or any parent of [[CypherValue]] (given access to all subclasses through normal variance rules).
  */
@implicitNotFound(
  "Cannot find an implicit CypherWrites to convert ${T} into ${V}\n" +
    "Note: An implicit CypherWritesPrimitive[${T}] will only be applied if the expected type " +
    "is no more specific than CypherPrimitive.")
trait CypherWrites[-T, +V <: CypherValue] {
  def writes(value: T): V
}

object CypherWrites extends DefaultCypherWrites {

  def apply[T, V <: CypherValue](writeFn: T => V): CypherWrites[T, V] = {
    new CypherWrites[T, V] {
      override def writes(value: T): V = writeFn(value)
    }
  }

  def array[T, V <: CypherPrimitive: NotMixed](writeEach: T => V): CypherWrites[Traversable[T], CypherArray[V]] = {
    new CypherWrites[Traversable[T], CypherArray[V]] {
      override def writes(value: Traversable[T]): CypherArray[V] = {
        CypherArray[V](value.map(writeEach).toSeq)
      }
    }
  }
}

trait DefaultCypherWrites {

  /**
    * [[CypherWritesPrimitive]] is implicitly applicable as a [[CypherWrites]] of [[CypherPrimitive]].
    */
  implicit def writesPrimitive[T](implicit writer: CypherWritesPrimitive[T]): CypherWrites[T, CypherPrimitive] = {
    new CypherWrites[T, CypherPrimitive] {
      override def writes(value: T): CypherPrimitive = writer.writes(value)
    }
  }

  /**
    * Any [[CypherValue]] can be implicitly written as a [[CypherValue]].
    */
  implicit val writesValue: CypherWrites[CypherValue, CypherValue] = CypherWrites(identity)

  /**
    * Any [[CypherPrimitive]] can be implicitly written as a [[CypherPrimitive]].
    */
  implicit val writesPrimitive: CypherWrites[CypherPrimitive, CypherPrimitive] = CypherWrites(identity)

  /**
    * Writes a traversable of values that can be written as [[CypherPrimitive]]s into a [[CypherArray]].
    */
  implicit def writesTraversable[T, V <: CypherPrimitive: NotMixed](
    implicit writer: CypherWrites[T, V]): CypherWrites[Traversable[T], CypherArray[V]] = {
    new CypherWrites[Traversable[T], CypherArray[V]] {
      override def writes(values: Traversable[T]): CypherArray[V] = {
        CypherArray(values.map(writer.writes).toSeq)
      }
    }
  }

  implicit val writesByte: CypherWrites[Byte, CypherByte] = CypherWrites(CypherByte(_))

  implicit val writesChar: CypherWrites[Char, CypherChar] = CypherWrites(CypherChar(_))

  implicit val writesString: CypherWrites[String, CypherString] = CypherWrites(CypherString(_))

  implicit val writesBoolean: CypherWrites[Boolean, CypherBoolean] = CypherWrites(CypherBoolean(_))

  implicit val writesShort: CypherWrites[Short, CypherShort] = CypherWrites(CypherShort(_))

  implicit val writesInt: CypherWrites[Int, CypherInt] = CypherWrites(CypherInt(_))

  implicit val writesLong: CypherWrites[Long, CypherLong] = CypherWrites(CypherLong(_))

  implicit val writesFloat: CypherWrites[Float, CypherFloat] = CypherWrites(CypherFloat(_))

  implicit val writesDouble: CypherWrites[Double, CypherDouble] = CypherWrites(CypherDouble(_))


}
