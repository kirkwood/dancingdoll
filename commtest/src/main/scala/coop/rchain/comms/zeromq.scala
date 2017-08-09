package coop.rchain.comm

import org.zeromq.ZMQ

object ZeromqComm {
  lazy val context = {
    ZMQ.context(1)
  }
}

class ZeromqComm(endpoint: Endpoint, peers: Array[Endpoint]) extends Comm {

  /*
   * Receiving stuff
   */

  lazy val receiver = {
    val uri = s"tcp://${endpoint format}"
    val s = ZeromqComm.context.socket(ZMQ.PULL)
    s.bind(uri)
    println(s"Bound sock $uri.")
    s
  }

  override def recv(): Result = {
    val stuff = receiver.recv(0)
    Response(stuff)
  }

  /*
   * Sending stuff
   */

  lazy val senders: Array[ZMQ.Socket] = 
    peers map { endpoint =>
      val s = ZeromqComm.context.socket(ZMQ.PUSH)
      val uri = s"tcp://${endpoint format}"
      s.connect(uri)
      println(s"Connected sender $uri.")
      s
    }

  override def send(data: Array[Byte]): Array[Result] = {
    senders map { s =>
      s.send(data, ZMQ.DONTWAIT) match {
        case false => Error("Couldn't send")
        case _ => Response((s"Sent $data: " getBytes) ++ data)
      }
    }
  }
}

