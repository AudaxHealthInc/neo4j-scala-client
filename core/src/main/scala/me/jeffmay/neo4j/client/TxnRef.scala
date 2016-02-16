package me.jeffmay.neo4j.client

import scala.util.{Failure, Success, Try}

/**
  * A reference to an open transaction.
  *
  * @see <a href="http://neo4j.com/docs/stable/rest-api-transactional.html">Transaction Documentation</a>
  *
  * @note Open transactions are not shared among members of an HA cluster.
  *       Therefore, if you use this endpoint in an HA cluster, you must ensure that
  *       all requests for a given transaction are sent to the same Neo4j instance.
  *
  * @param url the URL to commit the transaction
  * @param host the host address of the Neo4j instance on which the transaction is open
  * @param id the integer id of the transaction
  */
case class TxnRef(url: String, host: String, id: Int)
object TxnRef {

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
