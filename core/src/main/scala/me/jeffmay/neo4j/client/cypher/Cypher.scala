package me.jeffmay.neo4j.client.cypher

import scala.language.{dynamics, implicitConversions}

object Cypher {

  /**
    * The canonical method to create a [[CypherLabel]] to insert into a CypherString.
    *
    * @see <a href="http://neo4j.com/docs/stable/graphdb-neo4j.html#graphdb-neo4j-labels">Label Documentation</a>
    *
    * @return A [[CypherResult]] which can either contain the label or an error.
    */
  def label(labelName: String): CypherResult[CypherLabel] = CypherLabel(labelName)

  /**
    * The canonical method to create a [[CypherProps]] map.
    *
    * @param fields the key-value pairs of the properties and their associated [[CypherValue]].
    *
    * @return a [[CypherProps]] map where the last value for a given key overrides all earlier values.
    */
  def props(fields: (String, CypherValueWrapper)*): CypherProps = {
    Map(fields.map { case (k, v) => (k, v.value) }: _*)
  }

  // TODO: Use Li Haoyi's sourcecode macros to get the name of the variable this is assigned to
  //  def params(): Params = new Params("props", Map.empty)

  /**
    * Builds a handy [[Params]] object that enable string interpolation syntax.
    *
    * {{{
    *   val id: String = "1"
    *   val props = Cypher.params("props")
    *   val stmt = cypher"MATCH (n { id: ${props.id(id)} }) RETURN n"
    *   stmt.template // "MATCH (n { id: {props}.id }) RETURN n"
    *   stmt.parameters // Map("id" -> "1")
    * }}}
    *
    * @param namespace the name of the parameters object as sent to the server
    * @return a dynamic parameters builder for embedding into [[CypherStringContext]] interpolated [[Statement]]s
    */
  def params(namespace: String): Params = new Params(namespace)

  class Params private[Cypher] (val namespace: String) extends Dynamic {
    def applyDynamic[T](name: String)(value: T)(implicit writer: CypherWrites[T, CypherValue]): CypherArg = {
      new CypherParam(namespace, name, writer.writes(value))
    }
  }

  /**
    * A magnet type that allows mixed type [[CypherValue]]s to be correctly converted for props.
    */
  sealed trait CypherValueWrapper {
    def value: CypherValue
  }
  private case class CypherValueWrapperImpl(value: CypherValue) extends CypherValueWrapper
  implicit def toCypherValueWrapper[T](field: T)(implicit w: CypherWrites[T, CypherValue]): CypherValueWrapper = {
    CypherValueWrapperImpl(w.writes(field))
  }
}
