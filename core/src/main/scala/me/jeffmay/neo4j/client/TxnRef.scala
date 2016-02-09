package me.jeffmay.neo4j.client

import play.api.libs.json.{JsString, Writes}

import scala.util.{Failure, Success, Try}

case class TxnRef(url: String, domain: String, id: Int)
object TxnRef {

  implicit val jsonWriter: Writes[TxnRef] = Writes(r => JsString(r.url))

  def parse(url: String): TxnRef = {
    def fail(message: String): Nothing = {
      throw new IllegalArgumentException(s"Invalid transaction url '$url': $message")
    }
    def validate(cond: Boolean, message: String): Unit = {
      if (!cond) {
        fail(message)
      }
    }
    validate(!url.isEmpty, "Cannot be empty.")
    val schemeStartIdx = url.indexOf("//")
    validate(schemeStartIdx > 0, "Missing http or https scheme")

    val scheme = url.substring(0, schemeStartIdx + 2)
    validate(Set("http://", "https://") contains scheme, s"Unexpected scheme '$scheme'")
    val startPathIdx = url.indexOf('/', scheme.length)

    val domain = if (startPathIdx > 0) url.substring(0, startPathIdx) else url
    val expectedPath = "/db/data/transaction/"
    val txnIdStartIdx = domain.length + expectedPath.length

    val actualPath =
      if (txnIdStartIdx > url.length) "[invalid path]"  // causes exception
      else url.substring(domain.length, txnIdStartIdx)
    validate(actualPath == expectedPath, s"Must contain transaction path '$expectedPath'.")

    val txnIdEndIdx = url.indexOf('/', txnIdStartIdx)
    val txnIdString =
      if (txnIdEndIdx < 0) url.substring(txnIdStartIdx)
      else url.substring(txnIdStartIdx, txnIdEndIdx)

    Try(txnIdString.toInt) match {
      case Success(txnId) =>
        TxnRef(url, domain, txnId)
      case Failure(ex) =>
        fail("Could not parse transaction id as Int")
    }
  }
}
