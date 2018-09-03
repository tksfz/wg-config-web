package org.tksfz.wgconfig

import java.io._

import better.files.File
import com.github.veqryn.net.{Cidr4, Ip4}

import scala.sys.process._

case class IpRange(ipAddress: String, bits: Int) {
  def toCidr = s"$ipAddress/$bits"
}

object IpRange {
  def fromString(s: String) = {
    val Cidr = """([\d\.]+)/(\d+)""".r
    val Cidr(ipAddress, netmaskStr) = s
    IpRange(ipAddress, netmaskStr.toInt)
  }
}

case class ServerConfig(interface: String, serverEndpoint: String, serverPublicKey: String,
                        clientIpRange: IpRange, serverLanRange: IpRange)

class Wg(config: ServerConfig) {

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
    // TODO: lazy val
    val cidr4 = new Cidr4(config.clientIpRange.toCidr)
    cidr4.getAllIps(false).find(!used.contains(_))
  }

  def newClientConfig(clientIp: String) = {

    val (clientPublicKey, clientPrivateKey) = generatePrivateAndPublicKeys()

    // Server config
    val addServerConfig =
      s"""[Peer]
        |PublicKey = $clientPublicKey
        |AllowedIPs = ${config.clientIpRange}
        |""".stripMargin

    val f = File.newTemporaryFile("wgconfig-", ".conf", Some(File(System.getProperty("java.io.tmpdir"))))
    f.overwrite(addServerConfig)

    s"wg addconf ${config.interface} ${f.pathAsString}".!

    val clientConfig =
      s"""[Interface]
        |PrivateKey = $clientPrivateKey
        |ListenPort = 5555
        |Address = $clientIp/${config.serverLanRange.bits}
        |
        |[Peer]
        |PublicKey = ${config.serverPublicKey}
        |Endpoint = ${config.serverEndpoint}
        |AllowedIPs = ${config.serverLanRange.toCidr}
      """.stripMargin

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
