package me.jeffmay.neo4j.client.ws

import play.api.http.{HttpVerbs, Status}
import play.api.libs.json.JsValue
import play.api.libs.ws.{WSRequest, WSResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

object WSRequestWithException {

  implicit def wrap(request: WSRequest): WSRequestWithException = new WSRequestWithException(request)

  final val SuccessfulStatuses: Set[Int] = Set(Status.OK, Status.CREATED, Status.ACCEPTED)

  private def executeAndCheckStatus(
    request: WSRequest,
    validStatusSet: Int => Boolean,
    body: Option[JsValue] = None
  )(implicit ec: ExecutionContext): Future[WSResponse] = {
    val requestWithBody = body.fold(request)(request.withBody(_))
    requestWithBody.execute().flatMap {
      case resp if validStatusSet(resp.status) => Future.successful(resp)
      case resp => Future.failed(WSNeo4jClient.unexpectedStatusException(requestWithBody, body, resp))
    }
  }
}

class WSRequestWithException(val request: WSRequest) extends AnyVal {

  import WSRequestWithException._

  def getAndCheckStatus(expectedStatus: Int => Boolean = SuccessfulStatuses)
    (implicit ec: ExecutionContext): Future[WSResponse] = {
    executeAndCheckStatus(request.withMethod(HttpVerbs.GET), expectedStatus)
  }

  def deleteAndCheckStatus(expectedStatus: Int => Boolean = SuccessfulStatuses)
    (implicit ec: ExecutionContext): Future[WSResponse] = {
    executeAndCheckStatus(request.withMethod(HttpVerbs.DELETE), expectedStatus)
  }

  def putAndCheckStatus(body: Option[JsValue] = None, expectedStatus: Int => Boolean = SuccessfulStatuses)
    (implicit ec: ExecutionContext): Future[WSResponse] = {
    executeAndCheckStatus(request.withMethod(HttpVerbs.PUT), expectedStatus, body)
  }

  def postAndCheckStatus(body: Option[JsValue] = None, expectedStatus: Int => Boolean = SuccessfulStatuses)
    (implicit ec: ExecutionContext): Future[WSResponse] = {
    executeAndCheckStatus(request.withMethod(HttpVerbs.POST), expectedStatus, body)
  }
}
