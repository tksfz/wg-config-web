package org.tksfz.wgconfig

import cats.effect.Effect
import io.circe.Json
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Disposition`

class Service[F[_]: Effect](config: ServerConfig) extends Http4sDsl[F] {
  val wg = new Wg(config)

  val service: HttpService[F] = {
    HttpService[F] {
      case GET -> Root / "hello" / name =>
        Ok(Json.obj("message" -> Json.fromString(s"Hello, ${name}")))
      case POST -> Root / "clients" / "new" =>
        wg.findAvailableClientIp().map { clientIp =>
          val clientConfig = wg.newClientConfig(clientIp.getAddress)
          Ok(clientConfig).putHeaders(`Content-Disposition`("attachment", Map("filename" -> "wireguard.conf")))
        }.getOrElse {
          NotFound()
        }
    }
  }
}