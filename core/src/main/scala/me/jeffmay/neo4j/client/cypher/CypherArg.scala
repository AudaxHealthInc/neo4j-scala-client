package me.jeffmay.neo4j.client.cypher

/**
  * A valid argument to pass into the [[CypherStringContext]] used to insert some literal string or parameter(s)
  * into a Cypher [[CypherStatement]].
  */
sealed trait CypherArg {

  /**
    * The string of Cypher text sent to the server as the "statement" argument in the
    * <a href="http://neo4j.com/docs/stable/rest-api-transactional.html">Transaction Endpoint</a>
    *
    * @note This may still contain placeholders (surrounded by {}) that are substituted on the
    *       Neo4j server to avoid Cypher injection attacks.
    */
  def template: String
}

/**
  * A literal string to insert into the Cypher string.
  *
  * @param template the literal value to insert into the [[CypherStatement.template]].
  */
sealed abstract class CypherTemplatePart(override val template: String) extends CypherArg with Proxy {
  override def self: Any = template
}

/**
  * An identifier to refer to nodes, relationships, or paths in a pattern.
  *
  * @see <a href="http://neo4j.com/docs/stable/cypher-identifiers.html">Cypher Identifiers</a>
  * @param name the name of the identifier
  */
final class CypherIdentifier private (name: String) extends CypherTemplatePart(name)
object CypherIdentifier {

  val Valid = "^[a-zA-Z][a-zA-Z0-9_]*$".r

  def isValid(literal: String): Boolean = {
    Valid.findFirstMatchIn(literal).isDefined
  }

  def apply(literal: String): CypherResult[CypherIdentifier] = {
    if (isValid(literal)) {
      CypherResultValid(new CypherIdentifier(literal))
    } else {
      CypherResultInvalid(CypherIdentifierInvalidFormat(literal))
    }
  }
}

/**
  * A label to add to either a node or relationship.
  *
  * @see <a href="http://neo4j.com/docs/stable/graphdb-neo4j.html#graphdb-neo4j-labels">Label Documentation</a>
  * @param name the name of the label (without the preceding colon ':')
  */
final class CypherLabel private (name: String) extends CypherTemplatePart(s":$name")
object CypherLabel {

  val Valid = "^[a-zA-Z0-9_]+$".r

  private[this] var validated: Map[String, CypherLabel] = Map.empty

  def isValid(label: String): Boolean = {
    Valid.findFirstMatchIn(label).isDefined
  }

  def apply(label: String): CypherResult[CypherLabel] = {
    validated.get(label).map(CypherResultValid(_)) getOrElse {
      if (isValid(label)) {
        val valid = new CypherLabel(label)
        validated += label -> valid
        CypherResultValid(valid)
      } else {
        CypherResultInvalid(CypherLabelInvalidFormat(label))
      }
    }
  }
}

/**
  * Holds a single parameter field within one of the [[CypherStatement.parameters]] namespaces.
  *
  * @note This is not constructed directly. Rather, you use the [[Cypher.params]] methods to build this.
  * @param namespace the key of the [[CypherProps]] within which field names are unique
  * @param id the field name within the namespace
  * @param value the value of the parameter object's field
  */
case class CypherParamField private[cypher] (namespace: String, id: String, value: CypherValue) extends CypherArg {
  override val template: String = s"{$namespace}.$id"
}

/**
  * Holds a single parameter object as one of the [[CypherStatement.parameters]] namespaces.
  *
  * @note This is not constructed directly. Rather, you use the [[Cypher.params]] methods to build this.
  * @param namespace the key of the [[CypherProps]] within which field names are unique
  * @param props the map of fields to unfold as properties in place
  */
case class CypherParamObject private[cypher] (namespace: String, props: CypherProps) extends CypherArg {
  override val template: String = s"{ $namespace }"
}
