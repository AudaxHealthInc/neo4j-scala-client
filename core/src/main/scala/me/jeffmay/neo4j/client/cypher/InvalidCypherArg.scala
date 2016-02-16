package me.jeffmay.neo4j.client.cypher

import me.jeffmay.neo4j.client.Show

/**
  * The base trait for error messages related to building a [[CypherArg]].
  */
sealed trait InvalidCypherArg {
  def message: String
  def template: Option[String]
}

/**
  * The cypher argument requested is not available on the given props.
  */
case class MissingCypherProperty(props: CypherProps, name: String)(implicit showProps: Show[CypherProps])
  extends InvalidCypherArg {

  require(!(props contains name), s"${showProps show props} does contain the key '$name'")

  final override def template: Option[String] = None

  override lazy val message: String = {
    s"The property name '$name' cannot be found in the props: ${showProps show props}"
  }
}

/**
  * The base trait for error messages dealing with an invalid format when building the Cypher query string.
  */
sealed trait InvalidCypherArgFormat extends InvalidCypherArg {
  def literal: String
  override def template: Option[String] = Some(literal)
}

/**
  * Could not construct a [[CypherIdentifier]] because the provided label name did not match the required format.
  *
  * @param literal the invalid identifier name used
  */
case class CypherIdentifierInvalidFormat(literal: String) extends InvalidCypherArgFormat {
  override lazy val message: String = s"Invalid identifier format '$literal'. Must match /${CypherIdentifier.Valid.pattern.pattern}/"
}

/**
  * Could not construct a [[CypherLabel]] because the provided label name did not match the required format.
  *
  * @param literal the invalid label name used
  */
case class CypherLabelInvalidFormat(literal: String) extends InvalidCypherArgFormat {
  override lazy val message: String = s"Invalid label format '$literal'. Must match /${CypherLabel.Valid.pattern.pattern}/"
}
