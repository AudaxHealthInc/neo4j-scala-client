lazy val commonRootSettings = Seq(
  name := "neo4j-scala-client",
  organization := "com.rallyhealth",
  organizationName := "Rally Health",

  scalaVersion := "2.11.7",

  licenses += ("Apache-2.0", url("http://opensource.org/licenses/apache-2.0"))
)

lazy val root = (project in file("."))
  .aggregate(core, testUtil, ws, wsUtil, wsTestUtil)
  .settings(commonRootSettings ++ Seq(
    publish := {},
    publishArtifact := false
  ))

// Library versions
val playVersion = "2.4.6"
val scalacheckVersion = "1.12.5"
val scalatestVersion = "3.0.0-M10"

// Test cleanup helpers
lazy val cleanup = settingKey[Boolean]("Set to false to disable cleaning up after each test")
addCommandAlias("cleanupOn", "set cleanup := true")
addCommandAlias("cleanupOff", "set cleanup := false")

lazy val commonSettings = commonRootSettings ++ Seq(

  // Add required resolvers
  resolvers ++= Seq(
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
  ),

  // Add stricter Scala compiler options
  scalacOptions in ThisBuild ++= Seq(
    "-Xfatal-warnings",
    "-feature",
    "-deprecation:false",
    "-unchecked",
    "-Xfuture"
  ),

  // Common dependencies
  libraryDependencies ++= Seq(
    compilerPlugin("org.psywerx.hairyfotr" %% "linter" % "0.1.12"),
    "com.typesafe.play" %% "play-json" % playVersion,  // TODO: Allow using other Json libraries
    "org.scalacheck" %% "scalacheck" % scalacheckVersion,
    "org.scalatest" %% "scalatest" % scalatestVersion % "test"
  ),

  // Avoid sbt warnings for these transitive dependencies getting evicted
  dependencyOverrides := Set(
    "com.google.guava" % "guava" % "18.0"
  ),

  // Don't publish tests.jar
  publishArtifact in Test := false,
  // Don't compile or publish ScalaDoc
  // (ScalaDoc compiler breaks on links and the source code comments are more valuable)
  sources in(Compile, doc) := Seq.empty,
  publishArtifact in (Compile, packageDoc) := false

)

lazy val core = (project in file("core")).settings(commonSettings ++ Seq(
  name := "neo4j-scala-client-core"
))

lazy val testUtil = Project("test-util", file("test-util")).settings(commonSettings ++ Seq(
  name := "neo4j-scala-client-test-util",

  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % scalatestVersion
  )
)).dependsOn(core)

lazy val ws = (project in file("ws")).settings(commonSettings ++ Seq(

  name := "neo4j-scala-client-ws",

  libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play-ws" % playVersion
  ),

  // Cleanup after tests by default
  cleanup := true,

  // Call setup and teardown hooks before and after any tests run
  testOptions ++= Seq(
    Tests.Setup {
      classLoader: ClassLoader =>
        val cleanDBAfterTests = cleanup.value
        sys.props += "NEO4J_TEST_CLEANUP_AFTER" -> cleanDBAfterTests.toString
        if (cleanDBAfterTests) {
          println(AnsiColor.GREEN +
            """All tests will cleanup their changes to their namespace in the database after they finish.
              |Note: The tests will cleanup their own namespace from previous test runs before they run.""".stripMargin
            + AnsiColor.RESET)
        }
        else {
          println(AnsiColor.BLUE +
            """Tests will NOT cleanup their changes to their namespace in the database after they finish.
              |Note: The tests will cleanup their own namespace from previous test runs before they run.""".stripMargin
            + AnsiColor.RESET)
        }
        classLoader.loadClass("me.jeffmay.neo4j.client.SetupBeforeTests").newInstance()
    },
    Tests.Cleanup {
      classLoader: ClassLoader =>
        classLoader.loadClass("me.jeffmay.neo4j.client.CleanupAfterTests").newInstance()
    }
  )
)).dependsOn(core, testUtil % "test", wsTestUtil % "test", wsUtil)

lazy val wsUtil = Project("ws-util", file("ws-util")).settings(commonSettings ++ Seq(
  name := "neo4j-scala-client-ws-util",

  libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play-ws" % playVersion
  )
))

lazy val wsTestUtil = Project("ws-test-util", file("ws-test-util")).settings(commonSettings ++ Seq(
  name := "neo4j-scala-client-ws-test-util",

  libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play-ws" % playVersion,
    "org.scalatest" %% "scalatest" % scalatestVersion
  )
)).dependsOn(core, testUtil, wsUtil)
