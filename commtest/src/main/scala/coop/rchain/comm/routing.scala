package coop.rchain.comm

import coop.rchain.kademlia.{Peer => KademliaPeer}
import coop.rchain.comm.protocol.routing._
import scala.util.{Try,Success,Failure}
import scala.concurrent.duration.{Duration,MILLISECONDS}

// Implementation of Peer for kademlia protocol.
class PeerNode(val pKey: Array[Byte], val tcpPort: Int = 44444, val udpPort: Int = 44444) extends KademliaPeer {
  val key = pKey

  val rand = new scala.util.Random

  override def ping: Try[Duration] = {
    val dur = (1000.0*rand.nextDouble).asInstanceOf[Long]
    println(s"PING: $this")
    Thread.sleep(dur)
    if (dur > 500) {
      Failure(new Exception("timed out"))
    } else {
      Success(Duration(dur, MILLISECONDS))
    }
  }

  lazy private val sKey = key.map("%02x" format _).mkString
  override def toString = s"#{PeerNode $sKey}"
}
