package coop.rchain.comm

import java.net.InetSocketAddress

class Endpoint(host: String, port: Int) {
  override def toString = s"#{Endpoint $host:$port}"
  def format = s"$host:$port"

  def toInetSocketAddress: InetSocketAddress = new InetSocketAddress(host, port)
}

object EndpointFactory {
  def fromString(hostport: String, defaultHost: String = "*", defaultPort: Int = 44444): Endpoint = {
    val stuff = hostport split ":"
    stuff.size match {
      case 1 =>
        new Endpoint(stuff(0), defaultPort)
      case 2 =>
        if (stuff(0) == "") new Endpoint("localhost", stuff(1) toInt)
        else new Endpoint(stuff(0), stuff(1) toInt)
    }
  }
}

trait Result

case class Response(data: Array[Byte]) extends Result
case class Error(message: String) extends Result

trait Comm {
  def send(data: Array[Byte]): Array[Result]
  def recv(): Result
}

