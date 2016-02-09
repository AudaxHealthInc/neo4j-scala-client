package me.jeffmay.neo4j.client.cypher

/**
  * An exception thrown when there is an error constructing a [[CypherArg]] or [[Statement]].
  */
sealed abstract class CypherException(message: String) extends Exception(message)

/**
  * Thrown when a [[CypherArg]] cannot be extracted from one or more [[CypherResult]]s.
  *
  * @note this will combine multiple exceptions from multiple [[CypherResultInvalid]]s using
  *       suppressed exceptions.
  *
  * @param message the message detailing why the cypher statement could not be constructed
  * @param causes the underlying issues detailing why the individual [[CypherArg]]s are invalid
  */
class InvalidCypherException(val message: String, val causes: Seq[CypherResultInvalid] = Seq.empty)
  extends CypherException({
    causes.zipWithIndex.map {
      case (error, n) => s"($n) ${error.result.message}"
    }.mkString("\n")
  }) {

  for (error <- causes) {
    this.addSuppressed(error.exception)
  }
}

/**
  * An exception thrown
  * @param namespace
  * @param property
  */
class DuplicatePropertyNameException(val namespace: String, val property: String, val template: String)
  extends CypherException(s"'$property' has already been defined for the props object named '$namespace' in:\n$template")
