package me.jeffmay.neo4j.client.rest

import play.api.libs.json.{JsValue, Json}

class ConversionError(val expectedType: String, val body: JsValue, val reason: String)
  extends Exception(
    s"Connect convert to $expectedType from: ${Json.prettyPrint(Json.toJson(body))}\n" +
      s"Reason: $reason"
  )
