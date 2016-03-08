package me.jeffmay.neo4j.client

import me.jeffmay.neo4j.client.ws.{TestWSClient, TestWSNeo4jClient}
import me.jeffmay.util.akka.TestAkkaScheduler
import me.jeffmay.util.{RunHooksBlockingInOrder, RunStartupHooks}
import org.slf4j.LoggerFactory
import play.api.http.Status

import scala.concurrent.duration._
import scala.io.AnsiColor

class SetupBeforeTests extends RunStartupHooks(30.seconds, { implicit ec =>
  println(AnsiColor.YELLOW +
    """Loading src/test/resources/application.conf.
      |If there are issues loading the config:
      |exit sbt, run docker/start.sh, export the required environment variables, and retry.""".stripMargin +
    AnsiColor.RESET)
  val config = TestGlobal.loadConfig()
  val clientConfig = Neo4jClientConfig.fromConfig(config)
  val client = new TestWSNeo4jClient(
    TestWSClient.TestWS,
    clientConfig,
    LoggerFactory.getLogger(classOf[TestWSNeo4jClient]),
    TestAkkaScheduler(config = config)
  )
  Map(
    "Change default password" -> { () =>
      client.withCredentials("neo4j", "neo4j").changePassword(clientConfig.credentials.password).recover {
        case error: UnexpectedStatusException if error.responseStatus == Status.UNAUTHORIZED =>
        // Ok, nothing wrong here, the password was already set appropriately
      }
    }
  )
}) with RunHooksBlockingInOrder
