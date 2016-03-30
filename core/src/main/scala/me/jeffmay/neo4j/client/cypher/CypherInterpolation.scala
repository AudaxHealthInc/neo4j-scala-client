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
    * @throws CypherResultException               if any of the args are [[CypherResultInvalid]]
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
      throw new CypherResultException(
        "Encountered errors at the |INVALID[#]| location markers in query.",
        Some(template),
        invalidArgs
      )
    }

    // Collect all the field references
    var foundParamFields = Map.empty[String, Set[String]]
    var foundParamObjs = Map.empty[String, CypherProps]
    val params = args.foldLeft(Map.empty.withDefaultValue(Map.empty: CypherProps): CypherParams) {
      case (accParams, CypherResultValid(param: CypherParamArg)) =>
        val ns = param.namespace
        param match {
          case obj: CypherParamObject =>
            foundParamObjs.get(obj.namespace) match {
              case Some(conflictingProps) if conflictingProps != obj.props =>
                throw new ConflictingParameterObjectsException(ns, Seq(foundParamObjs(obj.namespace), obj.props), template)
              case Some(duplicates) => // nothing to do, duplicate props already found
              case _ =>
                foundParamObjs += obj.namespace -> obj.props
            }
          case field: CypherParamField =>
            foundParamFields.get(field.namespace) match {
              case Some(props) =>
                foundParamFields += field.namespace -> (props + field.id)
              case None =>
                foundParamFields += field.namespace -> Set(field.id)
            }
        }
        val nextProps = param.toProps
        accParams.get(ns) match {
          case Some(prevProps) =>
            // Merge non-conflicting properties / duplicates into same namespace
            accParams + (ns -> CypherStatement.mergeNamespace(template, ns, prevProps, nextProps))
          case None =>
            // Add non-conflicting parameter namespace
            accParams + (ns -> nextProps)
        }
      case (accParams, _) => accParams
    }

    // Throw the first exception of any object references that conflict with field name references in the same namespace
    for (conflictingNs <- foundParamObjs.keySet intersect foundParamFields.keySet) {
      throw new MixedParamReferenceException(conflictingNs, foundParamFields(conflictingNs), template)
    }

    // Return a statement that has been validated for missing or conflicting parameter values
    val stmt = CypherStatement(template, params)
    stmt.validate()
    stmt
  }
}
