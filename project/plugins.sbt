// See https://wiki.audaxhealth.com/display/ENG/Build+Structure#BuildStructure-Localconfiguration
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

// Add the resolvers
resolvers += Resolver.url(
  "Rally Plugin Releases", 
  url("https://artifacts.werally.in/artifactory/ivy-plugins-release")
)(Resolver.ivyStylePatterns)

// Add Rally plugins
addSbtPlugin("com.rallyhealth" %% "rally-versioning" % "latest.integration")
addSbtPlugin("com.rallyhealth" %% "rally-sbt-plugin" % "0.4.0")

// The current versions of these are failing because of this bug:
// https://github.com/scoverage/sbt-coveralls/issues/73
addSbtPlugin("org.scoverage" % "sbt-coveralls" % "1.0.0") // 1.0.3
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.0.1") // 1.3.5
