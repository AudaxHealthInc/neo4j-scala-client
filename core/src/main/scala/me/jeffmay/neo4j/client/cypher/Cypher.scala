package me.jeffmay.neo4j.client.cypher

import me.jeffmay.neo4j.client.Show

import scala.language.{dynamics, implicitConversions}

object Cypher {

  /**
    * Convert the given value to a map of string keys to [[CypherValue]].
    */
  def toProps[T](obj: T)(implicit writer: CypherWrites.Props[T]): CypherProps = writer writes obj

  /**
    * The canonical method to create a [[CypherLabel]] to insert into the cypher query string.
    *
    * @return A [[CypherResult]] which can either contain the label or an error.
    */
  def label(name: String): CypherResult[CypherLabel] = CypherLabel(name)

  /**
    * The canonical method to create a [[CypherIdentifier]] to insert into a cypher query string.
    *
    * @return a [[CypherResult]] which can either contain the identifier or an error
    */
  def ident(name: String): CypherResult[CypherIdentifier] = CypherIdentifier(name)

  // TODO: Use Li Haoyi's sourcecode macros to get the name of the variable this is assigned to
  //  def params(): Params = new Params("props", Map.empty)

  /**
    * @see [[DynamicMutableParam]]
    */
  def params(namespace: String): DynamicMutableParam = new DynamicMutableParam(namespace)

  /**
    * Used to insert cypher-injection-safe parameters into a [[CypherStatement]].
    *
    * @param namespace the namespace to which all parameters inserted by this class share
    */
  sealed abstract class Param(val namespace: String) {
    def isMutable: Boolean
    protected def __clsName: String
    override def toString: String = s"${__clsName}(namespace = $namespace)"
  }

  /**
    * Allows adding arbitrary [[CypherValue]]s to a single mutable [[CypherProps]] object within the
    * resulting [[CypherParams]]
    *
    * Used to enable the following string interpolation syntax:
    *
    * {{{
    *   val props = Cypher.params("props")
    *   val stmt = cypher"MATCH (n { id: ${props.id(1)} }) RETURN n"
    *   stmt.template // "MATCH (n { id: {props}.id }) RETURN n"
    *   stmt.parameters // Map("id" -> CypherInt(1))
    * }}}
    *
    * @param namespace the name of the parameter object as sent to the server
    *
    * @return a dynamic parameters builder for embedding into [[CypherStringContext]] interpolated [[CypherStatement]]s
    */
  class DynamicMutableParam private[Cypher](namespace: String)
    extends Param(namespace) with Dynamic {

    final override def isMutable: Boolean = true

    final override protected def __clsName: String = "ApplyDynamicParam"

    /**
      * Adds a property value to the resulting [[CypherProps]].
      *
      * @note assigning two different values to the same property within a [[CypherStatement]] will result
      *       in an error.
      * @return a valid [[CypherParamField]]
      */
    def applyDynamic[T](name: String)(value: T)(implicit writer: CypherWrites.Value[T]): CypherArg = {
      val cypherValue = writer.writes(value)
      new CypherParamField(namespace, name, cypherValue)
    }
  }

  /**
    * @see [[DynamicImmutableParam]]
    */
  def params(namespace: String, props: CypherProps): DynamicImmutableParam = new DynamicImmutableParam(namespace, props)

  /**
    * Immutable params cannot share the same namespace with other params in the same [[CypherStatement]]
    * as this would cause confusion if properties were merged and changed from underneath each other.
    *
    * @param namespace the namespace to which all parameters inserted by this class share
    * @param props the properties of the parameter object
    */
  sealed abstract class ImmutableParam(namespace: String, val props: CypherProps)(implicit showProps: Show[CypherProps])
    extends Param(namespace) with Proxy {
    final override def isMutable: Boolean = false
    override def self: Any = (namespace, props)
    override def toString: String = s"${__clsName}(namespace = $namespace, props = ${showProps show props})"
  }

  /**
    * Allows create a single immutable [[CypherProps]] object within the resulting [[CypherParams]]
    *
    * Enables the following string interpolation syntax:
    *
    * {{{
    *   val props = Cypher.params("props", Cypher.props("id" -> 1))
    *   val stmt = cypher"MATCH (n $props) RETURN n"
    *   stmt.template // "MATCH (n { props }) RETURN n"
    *   stmt.parameters // Map("id" -> CypherInt(1))
    * }}}
    *
    * @param namespace the name of the parameter object as sent to the server
    *
    * @return a dynamic parameters builder for embedding into [[CypherStringContext]] interpolated [[CypherStatement]]s
    */
  class DynamicImmutableParam private[Cypher](namespace: String, props: CypherProps)
    extends ImmutableParam(namespace, props) with Dynamic {

    final override protected def __clsName: String = "SelectDynamicParam"

    def selectDynamic(name: String): CypherResult[CypherArg] = {
      props.get(name) match {
        case Some(value) => CypherResultValid(new CypherParamField(namespace, name, value))
        case None => CypherResultInvalid(MissingCypherProperty(props, name))
      }
    }
  }

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

  /**
    * A magnet type that allows mixed type [[CypherValue]]s to be correctly converted for props.
    */
  sealed trait CypherValueWrapper {
    def value: CypherValue
  }
  private case class CypherValueWrapperImpl(value: CypherValue) extends CypherValueWrapper
  implicit def toCypherValueWrapper[T](field: T)(implicit w: CypherWrites.Value[T]): CypherValueWrapper = {
    CypherValueWrapperImpl(w.writes(field))
  }
}
