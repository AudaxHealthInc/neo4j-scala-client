package me.jeffmay.neo4j.client.cypher

import scala.annotation.implicitNotFound

/**
  * A serializer for converting a value into a [[CypherPrimitive]].
  */
@implicitNotFound(
  "Cannot find an implicit CypherWritesPrimitive to convert ${T} into a CypherPrimitive.\n" +
    "Note: A CypherWrites[${T}, _ <: CypherPrimitive] will also apply implicitly.")
trait CypherWritesPrimitive[-T] {
  def writes(value: T): CypherPrimitive
}

object CypherWritesPrimitive extends DefaultCypherPrimitiveWrites {

  def apply[T](writeFn: T => CypherPrimitive): CypherWritesPrimitive[T] = new CypherWritesPrimitive[T] {
    override def writes(value: T): CypherPrimitive = writeFn(value)
  }
}

trait DefaultCypherPrimitiveWrites {

  /**
    * [[CypherWrites]] of a [[CypherPrimitive]] will be implicitly converted.
    *
    * @see [[DefaultCypherWrites]] for examples.
    */
  implicit def writes[T](implicit writer: CypherWrites[T, CypherPrimitive]): CypherWritesPrimitive[T] = {
    new CypherWritesPrimitive[T] {
      override def writes(value: T): CypherPrimitive = writer.writes(value)
    }
  }
}
