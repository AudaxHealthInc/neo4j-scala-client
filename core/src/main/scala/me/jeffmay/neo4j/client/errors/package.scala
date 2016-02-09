package me.jeffmay.neo4j.client

import me.jeffmay.neo4j.client

package object errors {
  @deprecated("Use me.jeffmay.neo4j.client.RestResponseException", "0.3.0")
  type ResponseError = client.RestResponseException
  @deprecated("Use me.jeffmay.neo4j.client.StatusCodeException", "0.3.0")
  type StatusCodeError = client.StatusCodeException
  @deprecated("Use me.jeffmay.neo4j.client.UnexpectedStatusException", "0.3.0")
  type UnexpectedStatusException = client.UnexpectedStatusException
}
