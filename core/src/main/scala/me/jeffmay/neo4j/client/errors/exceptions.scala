package me.jeffmay.neo4j.client.errors

import me.jeffmay.neo4j.client.Neo4jError
import play.api.libs.json.{JsValue, Json}

import scala.util.control.NoStackTrace

object ResponseError {
  private[errors] val indent = 4
  private[errors] val shim = String.valueOf(Array.fill(indent)(' '))

  /**
    * Prepends the shim to every line of the given string (including the first).
    */
  private[errors] def shim(msg: String): String = msg.split('\n').mkString(shim, s"\n$shim", "\n")
}
sealed abstract class ResponseError(
  val requestMethod: String,
  val requestUrl: String,
  val requestHeaders: Seq[(String, String)],
  val requestBodyAsString: String,
  val responseStatus: Int,
  val responseBodyAsString: String,
  message: String
) extends Exception({
  import ResponseError._
  val indentedResponseHeaders = requestHeaders.map { case (k, v) => s"$shim$k: $v" }.mkString("\n")
  s"""FAILED to complete request.
    |$requestMethod $requestUrl
    |Request body:
    |${shim(requestBodyAsString)}
    |Request headers:
    |$indentedResponseHeaders
    |Received response with status code $responseStatus and response body:
    |${shim(responseBodyAsString)}
    |Reason: ${shim(message)}
  """.stripMargin
}) with NoStackTrace

class UnexpectedStatusException(
  requestMethod: String,
  requestUrl: String,
  requestHeaders: Seq[(String, String)],
  bodyAsString: String,
  responseStatus: Int,
  responseBodyAsString: String
) extends ResponseError(
  requestMethod,
  requestUrl,
  requestHeaders,
  bodyAsString,
  responseStatus,
  responseBodyAsString,
  "Unexpected response status."
)

case class StatusCodeError(
  override val requestMethod: String,
  override val requestUrl: String,
  override val requestHeaders: Seq[(String, String)],
  requestBody: Option[JsValue],
  override val responseStatus: Int,
  responseBody: JsValue,
  errors: Seq[Neo4jError]
) extends ResponseError(
  requestMethod,
  requestUrl,
  requestHeaders,
  requestBody.fold("")(Json.prettyPrint),
  responseStatus,
  Json.prettyPrint(responseBody),
  {
    import ResponseError._
    val errorMessages = errors.map { e =>
      Array(s"Code: ${e.status.code}", s"Description: ${e.status.description}", s"Message:\n${shim(e.message)}")
    }
    val errorMessagesIndexed = errorMessages.zipWithIndex.map {
      case (errorLines, index) =>
        errorLines.mkString(s"${index + 1})".padTo(4, ' '), s"\n$shim", "\n")
    }.mkString("\n")
    s"Encountered ${errors.size} error(s):\n$errorMessagesIndexed"
  }
)
