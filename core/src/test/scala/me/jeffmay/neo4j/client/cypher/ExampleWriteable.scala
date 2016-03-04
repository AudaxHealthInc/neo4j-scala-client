package me.jeffmay.neo4j.client.cypher

case class ExampleWriteable(value: String)
object ExampleWriteable {
  implicit val cypherWritesExampleWriteable: CypherWrites[ExampleWriteable, CypherString] = {
    CypherWrites(v => CypherString(v.value))
  }
}
