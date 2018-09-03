name := "wg-config-http"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  "com.github.pathikrit" %% "better-files" % "3.4.0",
  "com.github.veqryn" % "cidr-ip-trie" % "1.0.1"
)

val http4sVersion  = "0.18.9"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.http4s"     %% "http4s-blaze-server" % http4sVersion,
  "org.http4s"     %% "http4s-circe"        % http4sVersion,
  "org.http4s"     %% "http4s-dsl"          % http4sVersion,
)
