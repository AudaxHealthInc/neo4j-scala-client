package me.jeffmay.neo4j.client.ws

import me.jeffmay.neo4j.client.Show
import me.jeffmay.neo4j.client.ws.json.rest.RestFormats
import me.jeffmay.neo4j.client.ws.json.{DebugFormats, ShowAsJson}

/**
  * Import from this object to get the standard [[Show]] instances for all the rest models as Json.
  */
object WSDefaultShows extends WSDefaultShows
private[ws] trait WSDefaultShows extends DebugFormats with RestFormats with ShowAsJson
