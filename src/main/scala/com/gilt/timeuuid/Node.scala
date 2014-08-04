package com.gilt.timeuuid

import java.net.{InetAddress, NetworkInterface}
import scala.util.Try
import java.security.MessageDigest

private[timeuuid] object Node {
  private lazy val macAddressNode = new MacAddressNode().id
  private lazy val localhostNetworkInterfacesNode = new LocalHostAndNetworkInterfacesNode().id

  val id: Long = macAddressNode.recover {
    case e: Throwable =>
      localhostNetworkInterfacesNode.recover {
        case e: Throwable => throw new IllegalStateException("Unable to determine the Node Id. Mac address, Localhost and Network Interfaces are not available.", e)
      }.get
  }.get
}

private[timeuuid] trait Node {
  def id: Try[Long]

  protected def convertMacAddressToLong(macAddress: Array[Byte]): Long = {
    assert(macAddress.length == 6, "macAddress should be 6 bytes, was " + macAddress.mkString(","))
    val (result, _) = macAddress.foldLeft((0L, 0)) {
      case ((node, index), byte) =>
        (node | (0x00000000000000ffL & byte.asInstanceOf[Long]) << (index * 8), index + 1)
    }
    result
  }
}

private[timeuuid] class MacAddressNode extends Node {
  lazy val id: Try[Long] = {
    for {
      localhost <- Try(InetAddress.getLocalHost)
      localNetworkInterface <- Try(NetworkInterface.getByInetAddress(localhost))
      localMacAddress <- Try(localNetworkInterface.getHardwareAddress)
      nodeId <- Try(convertMacAddressToLong(localMacAddress))
    } yield nodeId
  }
}

private[timeuuid] class LocalHostAndNetworkInterfacesNode extends Node {

  import scala.collection.JavaConverters._

  private lazy val localAddressBytes = Try {
    InetAddress.getLocalHost.getAddress
  }.recover { case e: Throwable => Array()}

  private lazy val networkAddressesBytes = Try {
    (for {
      networkInterface <- NetworkInterface.getNetworkInterfaces.asScala
      inetAddress <- networkInterface.getInetAddresses.asScala
      byte <- inetAddress.getAddress
    } yield byte).toArray
  }.recover { case e: Throwable => Array()}

  private lazy val bytes = localAddressBytes.get ++ networkAddressesBytes.get

  override def id: Try[Long] = {
    Try {
      require(bytes.length > 0, "unable to retrieve local address of network addresses")
      val result = convertMacAddressToLong(MessageDigest.getInstance("SHA-1").digest(bytes).take(6))
      // Setting multicast bit 1 as per spec
      // http://www.ietf.org/rfc/rfc4122.txt & Datastax/Cassandra/Astyanax
      result | 0x0000010000000000L
    }
  }
}
