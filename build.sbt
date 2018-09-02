name := "wg-config-http"

scalaVersion := "2.12.6"

val http4sVersion  = "0.18.9"

libraryDependencies ++= Seq(
  "org.http4s"     %% "http4s-blaze-server" % http4sVersion,
  "org.http4s"     %% "http4s-circe"        % http4sVersion,
  "org.http4s"     %% "http4s-dsl"          % http4sVersion,
)
