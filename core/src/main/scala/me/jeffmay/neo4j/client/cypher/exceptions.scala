package me.jeffmay.neo4j.client.cypher

/**
  * An exception thrown when there is an error constructing a [[CypherArg]] or [[CypherStatement]].
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
class InvalidCypherException(
  val message: String,
  val template: Option[String],
  val causes: Seq[CypherResultInvalid] = Seq.empty
) extends CypherException({
  val cypherTemplate = template.fold("") { stmt =>
    // If the template is a multi-line statement, then put a newline before starting it
    s"Cypher statement template:${if (stmt contains "\n") "\n" else " "}'$stmt'"
  }
  message +
    (if (cypherTemplate.isEmpty) "\n" else s"\n$cypherTemplate\n") +
    causes.zipWithIndex.map {
      case (cause, n) => s"[${n + 1}] ${cause.result.message}"
    }.mkString("\n")
  }) {

  for (cause <- causes) {
    this.addSuppressed(cause.exception)
  }
}

/**
  * An exception thrown when a property is defined twice on the same namespace in the same template.
  *
  * @param namespace the namespace within which the [[CypherProps]] live
  * @param property the name of the property within the [[CypherProps]]
  * @param template the raw cypher template for debugging purposes
  */
class ConflictingParameterFieldsException(
  val namespace: String,
  val property: String,
  val conflictingValues: Seq[CypherValue],
  val template: String
) extends CypherException(
    s"The parameter namespace '$namespace' has been assigned conflicting values for property '$property' in:\n" +
    s"$template\n" +
    s"Conflicting values are: ${conflictingValues.mkString(", ")}"
  ) {
  private[this] val conflictingValueSet = conflictingValues.toSet
  assert(
    conflictingValueSet.size >= 2,
    "This exception should not be thrown when there is less than 2 distinct conflicting fields.\n" +
    s"Given: $conflictingValueSet"
  )
}

class ConflictingParameterObjectsException(
  val namespace: String,
  val conflictingParams: Seq[CypherProps],
  val template: String
) extends CypherException(
  s"The parameter namespace '$namespace' has been assigned conflicting objects in:\n" +
    s"$template\n" +
    s"Conflicting values are: ${conflictingParams.map("  " + _).mkString("[\n", ",\n", "\n]")}"
) {
  private[this] val conflictingValueSet = conflictingParams.toSet
  assert(
    conflictingValueSet.size >= 2,
    "This exception should not be thrown when there is less than 2 distinct conflicting objects.\n" +
    s"Given: $conflictingValueSet"
  )
}

class MutatedParameterObjectException(
  val namespace: String,
  val conflictingParams: Seq[CypherProps],
  val template: String
) extends CypherException({
  "ERROR"
})
