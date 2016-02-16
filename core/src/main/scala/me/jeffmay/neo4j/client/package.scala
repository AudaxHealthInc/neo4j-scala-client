package me.jeffmay.neo4j

import me.jeffmay.neo4j.client.cypher.ConflictingParameterFieldsException

package object client {

  @deprecated("Use ConflictingParameterFieldsException instead", "0.4.0")
  type DuplicatePropertyNameException = ConflictingParameterFieldsException
}
