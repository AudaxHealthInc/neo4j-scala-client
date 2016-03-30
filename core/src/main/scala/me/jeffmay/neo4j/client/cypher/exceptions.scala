package me.jeffmay.neo4j.client.cypher

import me.jeffmay.neo4j.client.Show

/**
  * An exception thrown when there is an error constructing a [[CypherArg]] or [[CypherStatement]].
  */
sealed abstract class CypherException(message: String) extends Exception(message)

/**
  * Thrown when a [[CypherArg]] cannot be extracted from one or more [[CypherResult]]s.
  *
  * @note this will combine multiple exceptions from multiple [[CypherResultInvalid]]s using
  *       suppressed exceptions.
  * @param message the message detailing why the cypher statement could not be constructed
  * @param causes the underlying issues detailing why the individual [[CypherArg]]s are invalid
  */
class CypherResultException(
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

sealed abstract class ConflictingParameterException(
  val namespace: String,
  val conflictingFields: Map[String, Seq[CypherValue]],
  val template: String,
  message: String
)(implicit showConflictingFields: Show[Map[String, Seq[CypherValue]]]) extends CypherException(message) {
  assert(
    conflictingFields.forall { case (k, values) => values.size >= 2 },
    {
      val printConflicts = showConflictingFields show conflictingFields
      "This exception should not be thrown when there is less than 2 distinct conflicting fields.\n" +
        s"Conflicts: $printConflicts"
    }
  )
}

/**
  * An exception thrown when a property is defined twice on the same namespace in the same template.
  *
  * @param namespace the namespace within which the [[CypherProps]] live
  * @param conflictingFields the properties and values that are in conflict (each entry must have >= 2 values)
  * @param template the raw cypher template for debugging purposes
  */
class ConflictingParameterFieldsException(
  namespace: String,
  conflictingFields: Map[String, Seq[CypherValue]],
  template: String
)(implicit
  showConflictingFields: Show[Map[String, Seq[CypherValue]]] = ConflictingParameterFieldsException.showConflicts
) extends ConflictingParameterException(
  namespace,
  conflictingFields,
  template, {
    val printConflicts = showConflictingFields show conflictingFields
    s"The parameter namespace '$namespace' has been assigned conflicting values for the following properties: $printConflicts\n" +
      "In the following template:\n" +
      template
  }
)

object ConflictingParameterFieldsException {
  val showConflicts: Show[Map[String, Seq[CypherValue]]] = {
    val showValues = Show[Seq[CypherValue]]
    Show.show { conflicts =>
      conflicts.map {
        case (k, values) => "\"" + k + "\": " + showValues.show(values)
      }.mkString("{\n", ",\n  ", "\n}")
    }
  }
}

/**
  * An exception thrown when a param is defined with the same namespace but two different sets of properties.
  *
  * @param namespace the namespace within which the [[CypherProps]] live
  * @param conflictingProps the properties and values that are in conflict (each entry must have >= 2 values)
  * @param template the raw cypher template for debugging purposes
  */
class ConflictingParameterObjectsException(
  namespace: String,
  val conflictingProps: Seq[CypherProps],
  template: String
)(implicit
  showConflictingFields: Show[Map[String, Seq[CypherValue]]] = ConflictingParameterFieldsException.showConflicts,
  showConflicts: Show[Seq[CypherProps]] = ConflictingParameterObjectsException.showConflicts
) extends ConflictingParameterException(
  namespace,
  {
    conflictingProps
      .flatMap(_.toSeq) // flatten properties into sequence of namespace -> value pairs
      .groupBy { case (k, v) => k } // group by namespace
      .mapValues(_.map(_._2).toSet) // group conflicting namespaces to sets of values
      .filter(_._2.size >= 2) // filter out identical value sets
      .mapValues(_.toSeq) // convert values back to seq keeping order for small sets
  },
  template, {
    val printConflicts = showConflicts show conflictingProps
    s"The parameter namespace '$namespace' has been assigned conflicting values for the following properties: $printConflicts\n" +
      "In the following template:\n" +
      template
  }
)

object ConflictingParameterObjectsException {
  val showConflicts: Show[Seq[CypherProps]] = {
    val showValues = Show[CypherProps]
    Show.show { conflicts =>
      conflicts.map {
        case props => showValues.show(props).split("\n").mkString("\n  ")
      }.mkString("[\n", ",\n  ", "\n]")
    }
  }
}

/**
  * Thrown when a [[CypherStatement]]'s template contains a reference to a parameter that is not present in the
  * statement's parameters map.
  *
  * @param statement
  * @param namespace
  * @param missingFieldReferences
  * @param showStatement
  */
class MissingParamReferenceException(
  val statement: CypherStatement,
  val namespace: String,
  val missingFieldReferences: Set[String]
)(implicit
  showStatement: Show[CypherStatement]
) extends CypherException({
    val fieldMessage =
      if (missingFieldReferences.isEmpty) ""
      else s" fields ${missingFieldReferences.mkString("'", "', '", "'")}"
    s"Missing parameter namespace '$namespace'$fieldMessage from cypher template:\n" +
    showStatement.show(statement)
  }) {
  def template: String = statement.template
}

/**
  * Thrown when creating a Cypher statement that contains a reference to a parameter using both the `{ param }` object
  * notation and per-field `{param}.field` notation.
  *
  * Mixing these styles is dangerous if the parameter field is added after the parameter object is added mutates the
  * effect of the query. This can manifest an issue in 2 ways:
  *
  * 1. A field can be added that was not present when the properties object was first inserted into the query.
  *    This can lead to the unexpected behavior of having unexpected properties on a created node.
  *
  * 2. A field value can be mutated since the "immutable" parameter object was referenced. This violates the
  *    contract of immutability and is probably not what you want.
  *
  * @note While this is strictly forbidden when using [[CypherInterpolation]], it is allowed when constructing
  *       a [[CypherStatement]] directly, as it assumes you know what you are doing when constructing by hand.
  *
  * @param namespace the namespace of the parameter that has conflicting object and field references
  * @param conflictingFieldReferences all field names that are referenced in the cypher template
  * @param template the cypher string template with conflicting reference types
  */
class MixedParamReferenceException(
  val namespace: String,
  val conflictingFieldReferences: Set[String],
  val template: String
)(implicit
  showStatement: Show[CypherStatement]
) extends CypherException({
    val printParamFields = conflictingFieldReferences.map(k => s"{$namespace}.$k").mkString("', '")
    s"Mixed use of param object and fields is not allowed. The namespace '$namespace' contains both a param object " +
    s"'{ $namespace }' as well as the following param fields '$printParamFields' in the following cypher template:\n" +
    template
  }) {
}
