package me.jeffmay.neo4j.client.cypher

object Label {

  val LabelNameRE = "^[a-zA-Z_]+$".r

  def apply(name: String): String = validateOrThrow(name)

  def validateOrThrow(name: String): String = {
    require(LabelNameRE.findFirstMatchIn(name).isDefined,
      s"Invalid Label format '$name'. Must match /${LabelNameRE.pattern.pattern}/"
    )
    name
  }
}
