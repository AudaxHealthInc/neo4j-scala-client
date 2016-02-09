package me.jeffmay.neo4j.client

import com.typesafe.config.{Config, ConfigException, ConfigFactory, ConfigResolveOptions}

import scala.io.AnsiColor

object TestGlobal {

  lazy val config: Config = {
    try loadConfig() catch {
      case ex: ConfigException =>
        println(AnsiColor.RED +
          s"""
             |Could not load required configuration for tests. Encountered exception:
             |${ex.getMessage}
             |
             |Run docker/start.sh to get a printout of the required configs.""".stripMargin
          + AnsiColor.RESET)
        sys.exit(1)
    }
  }

  def loadConfig(resolveOptions: ConfigResolveOptions = ConfigResolveOptions.defaults()): Config = {
    ConfigFactory.load(getClass.getClassLoader, resolveOptions)
  }
}
