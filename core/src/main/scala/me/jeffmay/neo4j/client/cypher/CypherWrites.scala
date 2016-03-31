package me.jeffmay.neo4j.client.cypher

import scala.annotation.implicitNotFound

/**
  * A serializer for converting any given type into either a specific subclass of [[CypherValue]],
  * or any parent of [[CypherValue]] (given access to all subclasses through normal variance rules).
  */
@implicitNotFound(
  "Cannot find an implicit CypherWrites to convert ${T} into ${V}.\n" +
    "Please define one using CypherWrites.a[${T}].as()")
trait CypherWrites[-T, +V <: CypherValue] {
  def writes(value: T): V
}

object CypherWrites extends DefaultCypherWrites {

  // Array aliases
  @deprecated("Use AsArray", "0.6.0")
  type Array[-T, V <: CypherPrimitive] = CypherWrites[Traversable[T], CypherArray[V]]
  @deprecated("Use AsPrimitiveArray", "0.6.0")
  type PrimitiveArray[-T] = CypherWrites.AsArray[T, CypherPrimitive]
  type AsArray[-T, V <: CypherPrimitive] = CypherWrites[Traversable[T], CypherArray[V]]
  type AsPrimitiveArray[-T] = CypherWrites[Traversable[T], CypherArray[CypherPrimitive]]

  // Core aliases
  @deprecated("Use AsValue", "0.6.0")
  type Value[-T] = CypherWrites[T, CypherValue]
  @deprecated("Use AsPrimitive", "0.6.0")
  type Primitive[-T] = CypherWrites[T, CypherPrimitive]
  type AsValue[-T] = CypherWrites[T, CypherValue]
  type AsPrimitive[-T] = CypherWrites[T, CypherPrimitive]

  // Handy aliases for common types
  type AsBoolean[-T] = CypherWrites[T, CypherBoolean]
  type AsString[-T] = CypherWrites[T, CypherString]
  type AsInt[-T] = CypherWrites[T, CypherInt]
  type AsLong[-T] = CypherWrites[T, CypherLong]
  type AsDouble[-T] = CypherWrites[T, CypherDouble]

  // Complex maps
  @deprecated("Use AsProps", "0.6.0")
  type Props[-T] = CypherWritesProps[T]
  type AsProps[-T] = CypherWritesProps[T]

  @deprecated("Use a[T].as(writeFn) instead. This will migrate to mean something else in a future version.", "0.6.0")
  def apply[T, V <: CypherValue](writeFn: T => V): CypherWrites[T, V] = a[T].as(writeFn)

  @deprecated("Use a[T].asArray(writeFn) instead", "0.6.0")
  def array[T, V <: CypherPrimitive: NotMixed](writeEach: T => V): CypherWrites.AsArray[T, V] = a[T].asArray(writeEach)

  def a[T]: CypherWritesBuilder[T] = new CypherWritesBuilder[T]
  def an[T]: CypherWritesBuilder[T] = new CypherWritesBuilder[T]

  class CypherWritesBuilder[T] private[CypherWrites] {

    def as[V <: CypherValue](writeFn: T => V): CypherWrites[T, V] = {
      new CypherWrites[T, V] {
        override def writes(value: T): V = writeFn(value)
      }
    }

    def asArray[V <: CypherPrimitive: NotMixed](writeEach: T => V): CypherWrites.AsArray[T, V] = {
      new CypherWrites.AsArray[T, V] {
        override def writes(value: Traversable[T]): CypherArray[V] = {
          CypherArray[V](value.map(writeEach).toSeq)
        }
      }
    }

    def asString(writeFn: T => String): CypherWrites[T, CypherString] = {
      as(writeFn.andThen(new CypherString(_)))
    }

    def using[R, V <: CypherValue](writeFn: T => R)(implicit writer: CypherWrites[R, V]): CypherWrites[T, V] = {
      as(writeFn andThen writer.writes)
    }

    def asProps(writeFn: T => CypherProps): CypherWritesProps[T] = {
      new CypherWritesProps[T] {
        override def writes(value: T): CypherProps = writeFn(value)
      }
    }
  }
}

trait DefaultCypherWrites {

  /**
    * Any [[CypherValue]] can be implicitly written as a [[CypherValue]].
    */
  implicit val writesValue: CypherWrites[CypherValue, CypherValue] = CypherWrites.a[CypherValue].as(identity)

  /**
    * Any [[CypherPrimitive]] can be implicitly written as a [[CypherPrimitive]].
    */
  implicit val writesPrimitive: CypherWrites[CypherPrimitive, CypherPrimitive] = CypherWrites.a[CypherPrimitive].as(identity)

  /**
    * Writes a traversable of values that can be written as [[CypherPrimitive]]s into a [[CypherArray]].
    */
  implicit def writesTraversable[T, V <: CypherPrimitive](
    implicit writer: CypherWrites[T, V], notMixed: NotMixed[V]): CypherWrites[Traversable[T], CypherArray[V]] = {
    new CypherWrites[Traversable[T], CypherArray[V]] {
      override def writes(values: Traversable[T]): CypherArray[V] = {
        CypherArray(values.map(writer.writes).toSeq)
      }
    }
  }

  implicit val writesByte: CypherWrites[Byte, CypherByte] = CypherWrites.a[Byte].as(CypherByte(_))

  implicit val writesChar: CypherWrites[Char, CypherChar] = CypherWrites.a[Char].as(CypherChar(_))

  implicit val writesString: CypherWrites[String, CypherString] = CypherWrites.a[String].as(CypherString(_))

  implicit val writesBoolean: CypherWrites[Boolean, CypherBoolean] = CypherWrites.a[Boolean].as(CypherBoolean(_))

  implicit val writesShort: CypherWrites[Short, CypherShort] = CypherWrites.a[Short].as(CypherShort(_))

  implicit val writesInt: CypherWrites[Int, CypherInt] = CypherWrites.an[Int].as(CypherInt(_))

  implicit val writesLong: CypherWrites[Long, CypherLong] = CypherWrites.a[Long].as(CypherLong(_))

  implicit val writesFloat: CypherWrites[Float, CypherFloat] = CypherWrites.a[Float].as(CypherFloat(_))

  implicit val writesDouble: CypherWrites[Double, CypherDouble] = CypherWrites.a[Double].as(CypherDouble(_))


}
