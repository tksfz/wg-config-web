package org.tksfz.wgconfig

import scala.concurrent.ExecutionContext
import cats.effect.{Effect, IO}
import com.github.veqryn.net.Cidr4
import fs2.StreamApp
import org.http4s.HttpService
import org.http4s.server.blaze.BlazeBuilder

object Server extends StreamApp[IO] {
  import scala.concurrent.ExecutionContext.Implicits.global

  def stream(args: List[String], requestShutdown: IO[Unit]): fs2.Stream[IO, StreamApp.ExitCode] = {
    val theCommand = command
    theCommand.parse(args).map { config =>
      ServerStream.stream[IO](config)
    }.getOrElse {
      fs2.Stream.eval(IO {
        println(theCommand.showHelp)
        StreamApp.ExitCode.Success
      })
    }
  }

  def command = {
    import com.monovore.decline._
    import cats.implicits._
    val intf = Opts.option[String]("interface", help = "Wireguard interface (e.g. wg0)", short = "i")
    val endpoint = Opts.option[String]("endpoint", help = "Server public endpoint with port (e.g. myhost.com:12345)", short = "e")
    val pubkey = Opts.option[String]("pubkey", help = "Server public key")
    val clientIpRange = Opts.option[String]("client-ips", help = "Client IP range as CIDR")
    val serverLanRange = Opts.option[String]("server-lan", help = "Server LAN CIDR")
    val combined = (intf, endpoint, pubkey, clientIpRange, serverLanRange).mapN { (intf, endpoint, pubkey, clientIpRange, serverLanRange) =>
      ServerConfig(intf, endpoint, pubkey, new Cidr4(clientIpRange), new Cidr4(serverLanRange))
    }
    Command("wg-config-web", "Web interface to generate Wireguard client configs")(combined)
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