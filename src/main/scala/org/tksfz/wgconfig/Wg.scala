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
      .filter(_.startsWith("AllowedIPs = "))
      .flatMap { line =>
        val cidrStr = line.split('=')(1).trim
        val cidr = new Cidr4(cidrStr)
        if (cidr.getAddressCount(false) == 1) {
          Some(cidr.getLowIp(false))
        } else {
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
        |AllowedIPs = ${config.clientIpRange}\n""".stripMargin

    val f = File.newTemporaryFile("wgconfig-", ".conf", Some(File(System.getProperty("java.io.tmpdir"))))
    f.overwrite(addServerConfig)

    logger.info(addServerConfig)

    //s"wg addconf ${config.interface} ${f.pathAsString}".!

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
    val os = new ByteArrayOutputStream()
    "wg pubkey" #< is #> os run()
    val publicKey = os.toString.trim
    (privateKey, publicKey)
  }

}
