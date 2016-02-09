package me.jeffmay.neo4j.client.cypher

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
  * A container for the success or failure to build a Cypher argument.
  *
  * This avoids throwing an exception on the construction of a [[CypherArg]], but captures the exception
  * at the point it occurs so that when you attempt to use the argument in the interpolated Cypher query,
  * you see an exception with a stack trace that points to the where the failure happened.
  */
sealed trait CypherResult[+T <: CypherArg] {

  def isValid: Boolean
  @inline final def isInvalid: Boolean = !isValid

  def getOrThrow: T
  @inline final def tryGet: Try[T] = Try(getOrThrow)
}
object CypherResult {
  implicit def valid[T <: CypherArg](arg: T): CypherResultValid[T] = CypherResultValid(arg)
}

/**
  * Contains a valid [[CypherArg]] that can be used
 *
  * @param result the valid result.
  */
case class CypherResultValid[+T <: CypherArg](result: T) extends CypherResult[T] {
  final override def isValid: Boolean = true
  final override def getOrThrow: T = result
}

/**
  * A [[CypherResult]] that did not meet the pre-conditions.
  *
  * @param result The invalid result
  */
case class CypherResultInvalid(result: InvalidCypherArg) extends CypherResult[Nothing] {
  // Construct the exception on instantiation to insure that the stack-trace captures where the failure occurs.
  val exception: Throwable = new InvalidCypherException(result.message)
  final override def isValid: Boolean = false
  final override def getOrThrow: Nothing = throw exception
}

/**
  * The base trait for error messages related to building a [[CypherArg]].
  */
sealed trait InvalidCypherArg {
  def message: String
}

/**
  * Could not construct a [[CypherLabel]] because the provided label name did not match the required format.
  *
  * @param label the invalid label name used
  */
case class CypherLabelInvalidFormat(label: String) extends InvalidCypherArg {
  override def message: String = s"Invalid Label format '$label'. Must match /${CypherLabel.Valid.pattern.pattern}/"
}

/**
  * A valid argument to pass into the [[CypherStringContext]] used to insert some literal string or parameter(s)
  * into a Cypher [[Statement]].
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
  * @param template the literal value to insert into the [[Statement.template]].
  */
sealed abstract class CypherLiteral(override val template: String) extends CypherArg with Proxy {
  override def self: Any = template
}

/**
  * A label to add to either a node or relationship.
  *
  * @param name the name of the label (without the preceding colon ':')
  */
final class CypherLabel private (name: String) extends CypherLiteral(s":$name")
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
        // TODO: Use compile-time exception information?
        CypherResultInvalid(CypherLabelInvalidFormat(label))
      }
    }
  }
}

/**
  * Holds a single parameter in the [[Statement.parameters]].
  *
  * @note This is not constructed directly. Rather, you use the [[Cypher.params]] methods to build this.
  * @param namespace the key of the [[CypherProps]] within which field names are unique
  * @param id the field name within the namespace
  * @param value the value of the parameter object's field
  */
case class CypherParam private[cypher] (namespace: String, id: String, value: CypherValue) extends CypherArg {
  override val template: String = s"{$namespace}.$id"
}
