package me.jeffmay.neo4j.client.cypher

import me.jeffmay.neo4j.client.cypher.Cypher.ImmutableParam

import scala.language.implicitConversions
import scala.util.Try

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

  /**
    * All [[CypherArg]]s are valid [[CypherArg]]s.
    */
  implicit def valid[T <: CypherArg](arg: T): CypherResultValid[T] = CypherResultValid(arg)

  /**
    * All [[ImmutableParam]]s are valid [[CypherArg]]s.
    */
  implicit def immutableParams(arg: ImmutableParam): CypherResultValid[CypherParamObject] = {
    CypherResultValid(new CypherParamObject(arg.namespace, arg.props))
  }
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

// TODO: Use compile-time exception information?
/**
  * A [[CypherResult]] that did not meet the pre-conditions.
  *
  * @param result The invalid result
  */
case class CypherResultInvalid(result: InvalidCypherArg) extends CypherResult[Nothing] {
  // Construct the exception on instantiation to insure that the stack-trace captures where the failure occurs.
  val exception: Throwable = new InvalidCypherException(result.message, result.template)
  final override def isValid: Boolean = false
  final override def getOrThrow: Nothing = throw exception
}
