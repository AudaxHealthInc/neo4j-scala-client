package me.jeffmay.neo4j.client.ws.json.debug

import me.jeffmay.neo4j.client.Show
import play.api.libs.json.{Json, Writes}

private[json] trait ShowAsJson {

  implicit def showAsJson[T](implicit writer: Writes[T]): Show[T] = Show.show(it => Json.prettyPrint(writer writes it))
}
