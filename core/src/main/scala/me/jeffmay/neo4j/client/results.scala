package me.jeffmay.neo4j.client

import play.api.libs.json._

/**
  * A result from a single [[Statement]].
  *
  * @param columns a map of column names to index
  * @param data    the table of rows / columns of json values to be parsed
  * @param stats   stats about this statement's effects
  */
case class StatementResult(
  columns: Map[String, Int],
  data: Seq[IndexedSeq[JsValue]],
  stats: Option[StatementResultStats] = None
) {

  /**
    * Reads a row by index to parse a single value by column name.
    *
    * @param index the index of the row to read
    * @return a map of column name to value
    */
  def row(index: Int): StatementResultRow = StatementResultRow(data(index), columns)

  /**
    * A lazy iterator of all the rows.
    */
  def rowsIterator: Iterator[StatementResultRow] = data.iterator.map(r => StatementResultRow(r, columns))

  /**
    * A lazy iterable over all the rows of the data in a manner that allows passing a column name to get the value.
    */
  def rows: Iterable[StatementResultRow] = rowsIterator.toIterable
}

object StatementResult {
  implicit val jsonWriter: Writes[StatementResult] = Json.writes[StatementResult]
}

/**
  * A thin wrapper around a row of data that allows access via a column name instead of an index.
  *
  * @param data a reference to a row of data in the [[StatementResult]]
  * @param columns a reference to the [[StatementResult]]'s columns
  */
case class StatementResultRow(data: IndexedSeq[JsValue], columns: Map[String, Int]) {

  @inline def apply(key: String): JsValue = col(key)

  /**
    * Returns the value of a given column by key, and if the key is not a column name, throws an exception.
    */
  def col(key: String): JsValue = data(columns(key))

  /**
    * Returns the value of a given column by key, and if the key is not a column name, returns None.
    */
  def getCol(key: String): Option[JsValue] = columns.get(key) map data

  /**
    * Returns a map representation of the data, where the key is the column and the value comes from the data.
    */
  def toMap: Map[String, JsValue] = columns mapValues data
}

object StatementResultRow {
  implicit val jsonWriter: Writes[StatementResultRow] = Json.writes[StatementResultRow]
}

/**
  * Stats about the effects of executing the [[Statement]].
  *
  * @param containsUpdates whether the statement contained updates
  * @param nodesCreated the number of nodes created
  * @param nodesDeleted the number of nodes deleted
  * @param propertiesSet the number of properties set (on either nodes or relationships)
  * @param relationshipsCreated the number of relationships created
  * @param relationshipDeleted the number of relationships deleted
  * @param labelsAdded the number of labels added (on either nodes or relationships)
  * @param labelsRemoved the number of labels removed (on either nodes or relationships)
  * @param indexesAdded the number of indexes added
  * @param indexesRemoved the number of indexes removed
  * @param constraintsAdded the number of constraints added
  * @param constraintsRemoved the number of constraints removed
  */
case class StatementResultStats(
  containsUpdates: Boolean,
  nodesCreated: Int,
  nodesDeleted: Int,
  propertiesSet: Int,
  relationshipsCreated: Int,
  relationshipDeleted: Int,
  labelsAdded: Int,
  labelsRemoved: Int,
  indexesAdded: Int,
  indexesRemoved: Int,
  constraintsAdded: Int,
  constraintsRemoved: Int
)

object StatementResultStats {
  implicit val jsonWriter: Writes[StatementResultStats] = Json.writes[StatementResultStats]

  val empty: StatementResultStats = {
    StatementResultStats(
      containsUpdates = false,
      nodesCreated = 0,
      nodesDeleted = 0,
      propertiesSet = 0,
      relationshipsCreated = 0,
      relationshipDeleted = 0,
      labelsAdded = 0,
      labelsRemoved = 0,
      indexesAdded = 0,
      indexesRemoved = 0,
      constraintsAdded = 0,
      constraintsRemoved = 0
    )
  }
}
