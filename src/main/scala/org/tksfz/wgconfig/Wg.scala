package org.tksfz.wgconfig

import java.io._

import better.files.File
import com.github.veqryn.net.Cidr4
import org.slf4j.LoggerFactory

import scala.sys.process._

case class ServerConfig(interface: String, serverEndpoint: String, serverPublicKey: String,
                        clientIpRange: Cidr4, serverLanRange: Cidr4) {
  lazy val allClientIps = clientIpRange.getAllIps(false)
}

class Wg(config: ServerConfig) {
  private val logger = LoggerFactory.getLogger(classOf[Wg])

  def findAvailableClientIp() = {
    val showConf = s"wg showconf ${config.interface}".lineStream_!
    val used = showConf
      .flatMap { line =>
        line.split('=').map(_.trim).toSeq match {
          case Seq("AllowedIPs", cidrStr) =>
            val cidr = new Cidr4(cidrStr)
            if (cidr.getAddressCount(true) == 1) {
              Some(cidr.getLowIp(true))
            } else {
              None
            }
          case _ =>
            None
        }
      }
      .toSet
    config.allClientIps.find(!used.contains(_))
  }

  def newClientConfig(clientIp: String) = {

    val (clientPrivateKey, clientPublicKey) = generatePrivateAndPublicKeys()

    // Server config
    val addServerConfig =
      s"""[Peer]
        |PublicKey = $clientPublicKey
        |AllowedIPs = ${clientIp}/32\n""".stripMargin

    val f = File.newTemporaryFile("wgconfig-", ".conf", Some(File(System.getProperty("java.io.tmpdir"))))
    f.overwrite(addServerConfig)

    logger.info(addServerConfig)

    s"wg addconf ${config.interface} ${f.pathAsString}".!

    val clientConfig =
      s"""[Interface]
        |PrivateKey = $clientPrivateKey
        |ListenPort = 5555
        |Address = $clientIp/${config.serverLanRange.getMaskBits}
        |
        |[Peer]
        |PublicKey = ${config.serverPublicKey}
        |Endpoint = ${config.serverEndpoint}
        |AllowedIPs = ${config.serverLanRange.getCidrSignature}\n""".stripMargin

    clientConfig
  }

  def generatePrivateAndPublicKeys() = {
    import scala.sys.process._
    val privateKey = "wg genkey".lineStream_!.head
    val is = new ByteArrayInputStream(privateKey.getBytes)
    val publicKey = ("wg pubkey" #< is).lineStream_!.head
    (privateKey, publicKey)
  }

}
