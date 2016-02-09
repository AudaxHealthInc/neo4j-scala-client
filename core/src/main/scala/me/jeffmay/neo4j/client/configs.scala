package me.jeffmay.neo4j.client

import com.typesafe.config.{Config, ConfigException}

import scala.concurrent.duration._
import scala.util.Try

/**
  * Basic auth configuration to send on each request to Neo4j's REST API.
  *
  * @param username the username (typically "neo4j")
  * @param password the password to authenticate with (starts as "neo4j" but must be changed)
  */
case class Neo4jBasicAuth(username: String, password: String)
object Neo4jBasicAuth {

  /**
    * The initial username and password of Neo4j that you are asked to change; just for reference.
    */
  final val Initial: Neo4jBasicAuth = Neo4jBasicAuth("neo4j", "neo4j")

  /**
    * You should really change these, but for testing purposes these are useful defaults.
    */
  final val UnsafeDefaultPassword: Neo4jBasicAuth = Neo4jBasicAuth("neo4j", "password")

  def fromConfig(config: Config, path: String = Neo4jClientConfig.DefaultRoot): Neo4jBasicAuth = {
    val c = config.getConfig(path)
    Neo4jBasicAuth(
      username = c.getString("username"),
      password = c.getString("password")
    )
  }
}

case class Neo4jClientConfig(
  baseUrl: String,
  timeout: FiniteDuration = 2.seconds,
  autoInitialize: Boolean = true,
  credentials: Neo4jBasicAuth = Neo4jBasicAuth.Initial,
  includeStatsByDefault: Boolean = false
)

object Neo4jClientConfig {

  /**
    * The default root of the config file from which to read configs for this library.
    *
    * This default assumes a fairly flat config file. You may want to consider nesting
    * this config under some deeper nested block. Providing your custom root to any
    * of the config parsers will insure you don't get runtime errors.
    */
  final val DefaultRoot: String = "neo4j"

  /**
    * Use localhost default settings for the database.
    */
  final val Local: Neo4jClientConfig = Neo4jClientConfig("http://localhost:7474")

  /**
    * Load the neo4j client configuration from the provided Typesafe [[Config]].
    *
    * @param config the config file or sub-object
    * @param path the root path in the config file from which all configs in this library are read.
    * @return the fully configured [[Neo4jClientConfig]] filled with defaults with appropriate
    * @throws ConfigException if any required config is missing
    */
  def fromConfig(
    config: Config,
    path: String = DefaultRoot,
    defaultCredentials: Option[Neo4jBasicAuth] = Some(Neo4jBasicAuth.UnsafeDefaultPassword)
  ): Neo4jClientConfig = {
    val c = config.getConfig(path)
    val timeout = Try(c.getDuration("timeout")).map(d => FiniteDuration(d.toMillis, MILLISECONDS))
    Neo4jClientConfig(
      baseUrl = Try(c.getString("url")) getOrElse Local.baseUrl,
      credentials = {
        Try(Neo4jBasicAuth.fromConfig(config, path)).recover {
          case ex => defaultCredentials getOrElse { throw ex }
        }.get
      },
      timeout = timeout getOrElse Local.timeout
    )
  }
}
