package me.jeffmay.neo4j.client.cypher

import scala.annotation.implicitNotFound

/**
  * A serializer for converting a value into a [[CypherProps]].
  */
@implicitNotFound(
  "Cannot find an implicit CypherWritesProps[${T}] to create the CypherProps.\n" +
    "Note: An implicit is CypherWritesProps[Map[K, V]] is provided automatically wherever there is an " +
    "implicit CypherWrites[K, CypherString] and CypherWrites.Value[V].")
trait CypherWritesProps[-T] {
  def writes(value: T): CypherProps
}

object CypherWritesProps extends DefaultCypherWritesProps {

  def apply[T](write: T => CypherProps): CypherWritesProps[T] = new CypherWritesProps[T] {
    override def writes(value: T): CypherProps = write(value)
  }
}

trait DefaultCypherWritesProps {

  /**
    * Writes a Map[K, V], where K can be converted into a String, into [[CypherProps]].
    */
  implicit def writesMap[K, V](
    implicit writeK: CypherWrites[K, CypherString], writeV: CypherWrites.AsValue[V]): CypherWritesProps[Map[K, V]] = {
    new CypherWritesProps[Map[K, V]] {
      override def writes(values: Map[K, V]): CypherProps = {
        values.map {
          case (k, v) => (writeK.writes(k).value, writeV.writes(v))
        }
      }
    }
  }

  /**
    * [[Cypher.ImmutableParam]]s can be written as [[CypherProps]].
    */
  implicit val writesParamProps: CypherWritesProps[Cypher.ImmutableParam] = CypherWritesProps(_.props)

  /**
    * Any [[CypherProps]] can be implicitly written as a [[CypherProps]].
    */
  implicit val writesCypherProps: CypherWritesProps[CypherProps] = CypherWritesProps(identity)
}
