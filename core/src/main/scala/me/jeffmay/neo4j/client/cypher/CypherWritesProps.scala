package me.jeffmay.neo4j.client.cypher

import scala.annotation.implicitNotFound

/**
  * A serializer for converting a value into a [[CypherProps]].
  */
@implicitNotFound(
  "Cannot find an implicit CypherWritesProps to convert ${T} into CypherProps.")
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
    * Writes a Map of String to T as [[CypherProps]].
    */
  implicit def writesMap[T](
    implicit writer: CypherWritesPrimitive[T]): CypherWritesProps[Map[String, T]] = {
    new CypherWritesProps[Map[String, T]] {
      override def writes(values: Map[String, T]): CypherProps = {
        values.mapValues(writer.writes)
      }
    }
  }

  /**
    * Any [[CypherProps]] can be implicitly written as a [[CypherProps]].
    */
  implicit val writesCypherProps: CypherWritesProps[CypherProps] = CypherWritesProps(identity)
}
