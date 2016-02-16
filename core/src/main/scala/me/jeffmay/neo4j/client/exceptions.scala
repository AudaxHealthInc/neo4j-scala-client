package me.jeffmay.neo4j.client

import me.jeffmay.neo4j.client.cypher.CypherStatement

import scala.util.control.NoStackTrace
import Exceptions._

/**
  * Common static exception helpers.
  */
private[client] object Exceptions {

  final val indent = 4
  final val shim = String.valueOf(Array.fill(indent)(' '))

  /**
    * Prepends the shim to every line of the given string (including the first).
    */
  def shim(msg: String): String = msg.split('\n').mkString(shim, s"\n$shim", "\n")
}

/**
  * An exception used to fail futures if there is an error with the request or response from the REST API.
  */
sealed abstract class RestResponseException(
  val requestMethod: String,
  val requestUrl: String,
  val requestHeaders: Seq[(String, String)],
  val requestBodyAsString: String,
  val responseStatus: Int,
  val responseBodyAsString: String,
  message: String
) extends Exception({
  val indentedResponseHeaders = requestHeaders.map { case (k, v) => s"$shim$k: $v" }.mkString("\n")
  s"""FAILED to complete request.
    |$requestMethod $requestUrl
    |Request body:
    |${shim(requestBodyAsString)}
    |Request headers:
    |$indentedResponseHeaders
    |Received response with status code $responseStatus and response body:
    |${shim(responseBodyAsString)}
    |Reason:
    |${shim(message)}
  """.stripMargin
}) with NoStackTrace

/**
  * The server responded with an unexpected HTTP status code.
  */
class UnexpectedStatusException(
  requestMethod: String,
  requestUrl: String,
  requestHeaders: Seq[(String, String)],
  bodyAsString: String,
  responseStatus: Int,
  responseBodyAsString: String
) extends RestResponseException(
  requestMethod,
  requestUrl,
  requestHeaders,
  bodyAsString,
  responseStatus,
  responseBodyAsString,
  "Unexpected response status."
)

/**
  * The server responded with one or more [[Neo4jError]]s.
  */
case class StatusCodeException(
  override val requestMethod: String,
  override val requestUrl: String,
  override val requestHeaders: Seq[(String, String)],
  requestBody: String,
  override val responseStatus: Int,
  responseBody: String,
  errors: Seq[Neo4jError]
) extends RestResponseException(
  requestMethod,
  requestUrl,
  requestHeaders,
  requestBody,
  responseStatus,
  responseBody,
  {
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

/**
  * The server responded with more results than expected.
  */
class TooManyResultsException(val statements: Seq[CypherStatement], val results: Seq[StatementResult])
  extends RuntimeException({
    s"Expected ${statements.size} result(s), but received ${results.size}."
  })
