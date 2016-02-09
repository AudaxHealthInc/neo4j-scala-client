package me.jeffmay.neo4j.client.ws

import akka.actor.Scheduler
import me.jeffmay.neo4j.client._
import me.jeffmay.neo4j.client.cypher.Statement
import me.jeffmay.neo4j.client.errors.{StatusCodeError, UnexpectedStatusException}
import me.jeffmay.neo4j.client.rest._
import me.jeffmay.util.ws.{ProxyWSClient, TimeoutWSRequest}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSAuthScheme, WSClient, WSRequest, WSResponse}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class WSNeo4jClient(
  wsClient: WSClient,
  val config: Neo4jClientConfig,
  implicit private val scheduler: Scheduler,
  implicit private val executionContext: ExecutionContext
) extends Neo4jClient with Proxy {
  client =>

  import WSNeo4jClient._
  import WSRequestWithException._

  override def self: Any = (wsClient, config)

  def copy(
    ws: WSClient = this.wsClient,
    config: Neo4jClientConfig = this.config,
    scheduler: Scheduler = this.scheduler,
    executionContext: ExecutionContext = this.executionContext
  ) = {
    new WSNeo4jClient(ws, config, scheduler, executionContext)
  }

  lazy val ws: WSClient = new ProxyWSClient(wsClient) {
    override def url(url: String): WSRequest = {
      new TimeoutWSRequest(wsClient.url(url), config.timeout)
    }
  }

  override def withCredentials(username: String, password: String): Neo4jClient =
    this.copy(config = config.copy(credentials = Neo4jBasicAuth(username, password)))

  override def withBaseUrl(baseUrl: String): Neo4jClient =
    this.copy(config = config.copy(baseUrl = baseUrl))

  override def withStatsIncludedByDefault(includeStatsByDefault: Boolean): Neo4jClient =
    this.copy(config = config.copy(includeStatsByDefault = includeStatsByDefault))

  override def withTimeout(timeout: FiniteDuration): Neo4jClient =
    this.copy(config = config.copy(timeout = timeout))

  protected def http(path: String): WSRequest = {
    require(!path.isEmpty, "path cannot be empty")
    require(path.charAt(0) == '/', "path must be an absolute path starting with '/'")
    import config.credentials.{password, username}
    ws.url(config.baseUrl + path).withAuth(username, password, WSAuthScheme.BASIC)
  }

  override def passwordChangeRequired(): Future[Boolean] = {
    val request = http(s"/user/${config.credentials.username}")
    request.getAndCheckStatus(Set(200)).map { resp =>
      (resp.json \ "password_change_required").asOpt[Boolean] contains true
    }
  }

  override def changePassword(newPassword: String): Future[Unit] = {
    val request = http(s"/user/${config.credentials.username}/password")
    request.postAndCheckStatus(Some(Json.obj("password" -> newPassword)), Set(200))
      .map(_ => ())
  }

  override def openTxn(statements: Statement*): Future[OpenedTxnResponse] = {
    val rawBody = RawStatementTransactionRequest.fromCypherStatements(statements)
    val jsonBody = Json.toJson(rawBody)
    val request = http("/db/data/transaction")
    request.postAndCheckStatus(Some(jsonBody)).flatMap { resp =>
      // TODO: Handle version / json format errors with common recovery code
      val respBody = resp.json.as[RawTxnResponse]
      if (respBody.isSuccessful) {
        Future.successful(respBody.asOpenTxnResponse.get)
      }
      else {
        Future.failed(statusCodeException(request, Some(jsonBody), resp, respBody.neo4jErrors))
      }
    }
  }

  override def openAndCommitTxn(first: Statement, others: Statement*): Future[CommittedTxnResponse] = {
    val rawBody = RawStatementTransactionRequest.fromCypherStatements(first +: others)
    val jsonBody = Json.toJson(rawBody)
    val request = http("/db/data/transaction/commit")
    rawBody.statements.foreach { stmt =>
      println(s"Executing Cypher statement: ${Json.prettyPrint(Json.toJson(stmt))}")
    }
    request.postAndCheckStatus(Some(jsonBody)).flatMap { resp =>
      // TODO: Handle version / json format errors with common recovery code
      val respBody = resp.json.as[RawTxnResponse]
      if (respBody.isSuccessful) {
        Future.successful(respBody.asCommitTxnResponse.get)
      }
      else {
        Future.failed(statusCodeException(request, Some(jsonBody), resp, respBody.neo4jErrors))
      }
    }
  }

  override def commitTxn(ref: TxnRef, alongWith: Statement*): Future[CommittedTxnResponse] = {
    val rawBody = RawStatementTransactionRequest.fromCypherStatements(alongWith)
    val jsonBody = Json.toJson(rawBody)
    val request = http("/db/data/transaction")
    request.postAndCheckStatus(Some(jsonBody)).flatMap { resp =>
      // TODO: Handle version / json format errors with common recovery code
      val respBody = resp.json.as[RawTxnResponse]
      if (respBody.isSuccessful) {
        Future.successful(respBody.asCommitTxnResponse.get)
      }
      else {
        Future.failed(statusCodeException(request, Some(jsonBody), resp, respBody.neo4jErrors))
      }
    }
  }
}

object WSNeo4jClient {

  def apply(
    ws: WSClient,
    config: Neo4jClientConfig,
    scheduler: Scheduler,
    executionContext: ExecutionContext
  ) = new WSNeo4jClient(ws, config, scheduler, executionContext)

  def flattenHeaders(headers: Map[String, Seq[String]]): Seq[(String, String)] = {
    headers.toSeq.flatMap {
      case (header, values) => values.map(header -> _)
    }
  }

  def statusCodeException(request: WSRequest, requestBody: Option[JsValue], response: WSResponse, errors: Seq[Neo4jError]): StatusCodeError = {
    new StatusCodeError(
      request.method,
      request.url,
      flattenHeaders(request.headers),
      requestBody,
      response.status,
      response.json,
      errors
    )
  }

  def unexpectedStatusException(request: WSRequest, requestBody: Option[JsValue], response: WSResponse): UnexpectedStatusException = {
    new UnexpectedStatusException(
      request.method,
      request.url,
      flattenHeaders(request.headers),
      requestBody.fold("")(Json.prettyPrint),
      response.status,
      Try(Json.prettyPrint(response.json)) getOrElse response.body
    )
  }
}
