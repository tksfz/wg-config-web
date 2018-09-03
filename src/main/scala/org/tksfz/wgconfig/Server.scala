package org.tksfz.wgconfig

import scala.concurrent.ExecutionContext

import cats.effect.{Effect, IO}
import fs2.StreamApp
import org.http4s.HttpService
import org.http4s.server.blaze.BlazeBuilder

object Server extends StreamApp[IO] {
  import scala.concurrent.ExecutionContext.Implicits.global

  def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] = {
    val config =
      ServerConfig("wg0", "endpoint", "pubkey", null, null)
    ServerStream.stream[IO](config)
  }
}

object ServerStream {

  def helloWorldService[F[_]: Effect](config: ServerConfig): HttpService[F] = new Service[F](config).service

  def stream[F[_]: Effect](config: ServerConfig)(implicit ec: ExecutionContext): fs2.Stream[F, StreamApp.ExitCode] = {
    val host = Option(System.getenv("APP_HOST")).getOrElse("0.0.0.0")
    val port = Option(System.getenv("APP_PORT")).map(_.toInt).getOrElse(8080)

    BlazeBuilder[F]
      .bindHttp(port, host)
      .mountService(helloWorldService(config), "/")
      .serve
  }

}