package me.jeffmay.neo4j.client.cypher

import scala.annotation.implicitNotFound

/**
  * An algebraic data type for representing the values you can insert into a Cypher query.
  *
  * @see <a href="http://neo4j.com/docs/stable/cypher-values.html">Values Documentation</a>
  *
  * Some observations that are enforced with type-safety:
  *
  * 1. [[CypherArray]]s can only contain values of a single type. See
  *    <a href="http://neo4j.com/docs/stable/rest-api-property-values.html#_arrays">here</a>
  *
  * 2. [[CypherArray]]s cannot be empty. This is hinted at compile-time by requiring evidence
  *    that the type argument is a non-mixed type, which only the sub-classes of [[CypherPrimitive]]
  *    define. You can bypass this restriction by passing `null` or defining the implicit, but
  *    only do this if you know what you are doing. The server will return an error in some cases.
  *
  * 3. [[CypherProps]] cannot nested [[CypherProps]], this is why it takes only [[CypherPrimitive]]s
  */
sealed trait CypherValue {
  type Value
  def value: Value
}

class CypherArray[T <: CypherPrimitive] private (override val value: Seq[T]) extends CypherValue with Proxy {
  override def self: Any = value
  override type Value = Seq[T]
}
object CypherArray {
  def apply[T <: CypherPrimitive](values: Seq[T])(implicit mixed: NotMixed[T]): CypherArray[T] = {
    new CypherArray[T](values)
  }
  def unapply[T <: CypherPrimitive](arr: CypherArray[T]): Option[Seq[T]] = {
    Some(arr.value)
  }
}

@implicitNotFound(
  "Neo4j does not support mixed arrays. Please use a specific CypherPrimitive type.\n" +
  "This error is often caused by the compiler taking the least upper bound of the given arguments.\n" +
  "If you are certain that the CypherArray will not contain mixed values, you can pass 'null'")
trait NotMixed[T <: CypherPrimitive]

sealed trait CypherPrimitive extends CypherValue

case class CypherByte(value: Byte) extends CypherPrimitive {
  override type Value = Byte
}
object CypherByte {
  implicit def single: NotMixed[CypherByte] = null
}

case class CypherChar(value: Char) extends CypherPrimitive {
  override type Value = Char
}
object CypherChar {
  implicit def single: NotMixed[CypherChar] = null
}

case class CypherString(value: String) extends CypherPrimitive {
  override type Value = String
}
object CypherString {
  implicit def single: NotMixed[CypherString] = null
}

case class CypherBoolean(value: Boolean) extends CypherPrimitive {
  override type Value = Boolean
}
object CypherBoolean {
  implicit def single: NotMixed[CypherBoolean] = null
}

case class CypherShort(value: Short) extends CypherPrimitive {
  override type Value = Short
}
object CypherShort {
  implicit def single: NotMixed[CypherShort] = null
}

case class CypherInt(value: Int) extends CypherPrimitive {
  override type Value = Int
}
object CypherInt {
  implicit def single: NotMixed[CypherInt] = null
}

case class CypherLong(value: Long) extends CypherPrimitive {
  override type Value = Long
}
object CypherLong {
  implicit def single: NotMixed[CypherLong] = null
}

case class CypherFloat(value: Float) extends CypherPrimitive {
  override type Value = Float
}
object CypherFloat {
  implicit def single: NotMixed[CypherFloat] = null
}

case class CypherDouble(value: Double) extends CypherPrimitive {
  override type Value = Double
}
object CypherDouble {
  implicit def single: NotMixed[CypherDouble] = null
}
