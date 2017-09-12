package coop.rchain.comm

import java.net.{DatagramSocket,DatagramPacket}
import java.util.UUID

object UnicastComm {
}

class UnicastComm(p: Peer) extends Comm {
  lazy val receiver = new DatagramSocket(p.endpoint.port)

  val recv_buffer = new Array[Byte](65536)
  val recv_dgram = new DatagramPacket(recv_buffer, recv_buffer.size)

  override def recv(): Result = {
    receiver.receive(recv_dgram)
    Response(recv_dgram getData)
  }

  override def send(data: Array[Byte]) = ()
  override def sendTo(data: Array[Byte], id: UUID) = ()
  override def addPeer(p: Peer) = ()
  override def getPeers() = new Array[Peer](0)
  override def removePeer(pid: UUID) = ()
  override def removePeer(p: Peer) = ()
}
