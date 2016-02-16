package me.jeffmay.neo4j.client.cypher

import scala.language.{dynamics, implicitConversions}

trait CypherInterpolation {

  implicit def cypherStringContext(sc: StringContext): CypherStringContext = new CypherStringContext(sc)
}

class CypherStringContext(val sc: StringContext) extends AnyVal {

  /**
    * Build a cypher [[CypherStatement]] by interpolating the given [[CypherArg]]s.
    *
    * @param args the [[CypherResult]]s from which to extract the [[CypherArg]]s
    *
    * @return a [[CypherStatement]] with the concatenated template and the the template and parameters
    *         of the [[CypherStatement]].
    * @throws InvalidCypherException              if any of the args are [[CypherResultInvalid]]
    * @throws ConflictingParameterFieldsException if two [[CypherParamField]]s share the same namespace and property name
    *                                             and the given values are different.
    */
  def cypher(args: CypherResult[CypherArg]*): CypherStatement = {
    // Build the literal query string
    var count: Int = 0
    val tmplParts: Seq[String] = args.map {
      case CypherResultValid(valid) =>
        valid.template
      case CypherResultInvalid(invalid) =>
        count += 1
        s"|INVALID[$count]|"
    }
    val template = sc.s(tmplParts: _*)
    val invalidArgs = args.collect {
      case invalid: CypherResultInvalid => invalid
    }
    if (invalidArgs.nonEmpty) {
      throw new InvalidCypherException(
        "Encountered errors at the |INVALID[#]| location markers in query.",
        Some(template),
        invalidArgs
      )
    }

    // Separate the dynamic props from the static props
    var dynamicFields = Seq.empty[CypherParamField]
    var staticObjects = Seq.empty[CypherParamObject]
    args foreach {
      case CypherResultValid(p: CypherParamField)  =>
        dynamicFields :+= p
      case CypherResultValid(p: CypherParamObject) =>
        staticObjects :+= p
      case _ =>
    }
    val objectsByNamespace = staticObjects.groupBy(_.namespace)
    val fieldsByNamespace = dynamicFields.groupBy(_.namespace)

    // Collect all the static parameter objects or throw an exception
    // if any dynamic properties share the same namespace
    val immutableParamObjects = objectsByNamespace
      .map { case (namespace, objects) =>
        if (fieldsByNamespace contains namespace) {
          val conflictingParams = args.collect {
            case CypherResultValid(p: CypherParamField) if p.namespace == namespace => Cypher.props(p.id -> p.value)
            case CypherResultValid(p: CypherParamObject) if p.namespace == namespace => p.props
          }
          throw new MutatedParameterObjectException(namespace, conflictingParams, template)
        }
        else if (objects.toSet.size > 1) {
          throw new ConflictingParameterObjectsException(namespace, objects.map(_.props), template)
        }
        else {
          namespace -> objects.head.props
        }
      }

    // Collect the dynamic parameter fields into properties objects
    val mutableParamObjects = fieldsByNamespace
      .map { case (namespace, fields) =>
        val props: CypherProps = fields.groupBy(_.id).map { case (name, values) =>
          // Allow duplicate values if they are equal
          val conflictingValues = values.map(_.value)
          if (conflictingValues.toSet.size > 1) {
            throw new ConflictingParameterFieldsException(namespace, name, conflictingValues, template)
          }
          name -> values.head.value
        }
        namespace -> props
      }

    // We should not have any conflicts of namespace between static and dynamic properties
    val params = immutableParamObjects ++ mutableParamObjects
    assert(
      params.size == immutableParamObjects.size + mutableParamObjects.size,
      "Mutable and immutable param objects should never merge as " +
        "combining the two should always throw a MutatedParameterObjectException"
    )
    CypherStatement(template, params)
  }
}
