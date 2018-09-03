package org.tksfz.wgconfig

import java.io._

case class IpRange(ipAddress: String, netmask: Int) {
  override def toString = s"$ipAddress/$netmask"
}
case class ServerConfig(interface: String, serverEndpoint: String, serverPublicKey: String,
                        clientIpRange: IpRange, serverLanRange: IpRange)

class Wg {

  def newClientConfig(config: ServerConfig, clientIp: String) = {

    val (clientPublicKey, clientPrivateKey) = generatePrivateAndPublicKeys()

    // Server config
    val addServerConfig =
      s"""[Peer]
        |PublicKey = $clientPublicKey
        |AllowedIPs = ${config.clientIpRange}
        |""".stripMargin

    val clientConfig =
      s"""[Interface]
        |PrivateKey = $clientPrivateKey
        |ListenPort = 5555
        |Address = $clientIp/${config.serverLanRange.netmask}
        |
        |[Peer]
        |PublicKey = ${config.serverPublicKey}
        |Endpoint = ${config.serverEndpoint}
        |AllowedIPs = ${config.serverLanRange}
      """.stripMargin




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
