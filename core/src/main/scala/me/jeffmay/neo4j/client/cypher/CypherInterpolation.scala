package me.jeffmay.neo4j.client.cypher

import scala.language.{dynamics, implicitConversions}

trait CypherInterpolation {

  implicit def cypherStringContext(sc: StringContext): CypherStringContext = new CypherStringContext(sc)
}

class CypherStringContext(val sc: StringContext) extends AnyVal {

  /**
    * Build a cypher [[Statement]] by interpolating the given [[CypherArg]]s.
    *
    * @param args the [[CypherResult]]s from which to extract the [[CypherArg]]s
    *
    * @return a [[Statement]] with the concatenated template and the the template and parameters
    *         of the [[Statement]].
    *
    * @throws InvalidCypherException if any of the args are [[CypherResultInvalid]]
    * @throws DuplicatePropertyNameException if two [[CypherParam]]s share the same namespace and property name
    */
  def cypher(args: CypherResult[CypherArg]*): Statement = {
    // Build the literal query string
    var count: Int = 0
    val tmplParts: Seq[String] = args.map {
      case CypherResultValid(valid) =>
        valid.template
      case CypherResultInvalid(invalid) =>
        count += 1
        s"|($count) INVALID|"
    }
    val template = sc.s(tmplParts: _*)
    val invalidArgs = args.collect {
      case invalid: CypherResultInvalid => invalid
    }
    if (invalidArgs.nonEmpty) {
      throw new InvalidCypherException(
        "Encountered errors at the |INVALID| location markers in query:\n" +
          s"$template\n\n",
        invalidArgs
      )
    }
    // Build the properties object
    val params = args
      .collect { case CypherResultValid(p: CypherParam) => p }
      .groupBy(_.namespace)
      .map { case (namespace, fields) =>
        val props: CypherProps = fields.groupBy(_.id).map { case (name, values) =>
          if (values.tail.nonEmpty) {
            throw new DuplicatePropertyNameException(namespace, name, template)
          }
          name -> values.head.value
        }
        namespace -> props
      }
    Statement(template, params)
  }
}
