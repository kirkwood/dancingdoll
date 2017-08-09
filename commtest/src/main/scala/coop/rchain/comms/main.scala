package coop.rchain.comm

import org.rogach.scallop._
import coop.rchain.kv._

import java.util.concurrent.{BlockingQueue,LinkedBlockingQueue}
import java.util.UUID

object Defaults {
  val listenPort = 44444
  val transport = Some("zeromq")
  val listen = Some(s"*:$listenPort")
  val httpPort = Some(8878)
}

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  version("0.0.1 rchain blah blah")

  // These keywords denote implemented transports
  val validTransports = Set("zeromq", "netty")

  val transport = opt[String](
    default = Defaults.transport,
    short = 't',
    descr = "Transport mechanism to use; one of: " + (validTransports mkString ", "))

  validate(transport) { t =>
    if (validTransports contains t) Right(Unit)
    else Left(s"Bad transport: $t (must be one of: " + (validTransports mkString ", ") + ")")
  }

  val listen = opt[String](
    default = Defaults.listen,
    short = 'p',
    descr = "Address (host:port) on which transport should listen.")

  // Defer analysis of this value until later; it's hard to verify
  // statically
  val peers = opt[String](
    required = true,
    default = None,
    descr = "Comma-separated list of peer nodes in host:port format.")

  val httpPort = opt[Int](
    default = Defaults.httpPort,
    validate = (0 <),
    short = 'H',
    descr = "Port on which HTTP server should listen.")

  verify
}

class Receiver(comm: Comm, commands: BlockingQueue[KeyValueCommand]) extends Thread {
  override def run(): Unit = {
    while (true) {
      val stuff = comm.recv()
      stuff match {
        case Response(d) => {
          println(s"Received: " + new String(d))
          commands add (KeyValueCommand parseFrom d)
        }
        case Error(e) => println(s"Error: $e")
      }
    }
  }
}

class Mutator(me: UUID, comm: Comm, store: KeyValueStore, commands: BlockingQueue[KeyValueCommand]) extends Thread {
  val buf = new java.io.ByteArrayOutputStream
  val uuid_str = me toString

  override def run(): Unit = {
    val setCmd = com.google.protobuf.ByteString.copyFromUtf8("set")

    while (true) {
      val cmd = (commands take)

      println(s"COMMAND: $cmd")

      if (cmd.command == setCmd) {
        store.add(new Key(cmd.key toStringUtf8), (cmd.value toStringUtf8))
        cmd.header match {
          case Some(h) => {
            if ((h.nodeId toStringUtf8) == uuid_str) {
              (buf reset)
              (cmd writeTo buf)
              (comm send (buf toByteArray)) foreach { r =>
                println(
                  r match {
                    case Response(d) => s"data: $d: ‘" + new String(d) + "’"
                    case Error(msg) => s"error: $msg"
                  }
                )
              }
            }
          }
          case None => ()
        }
      }
    }
  }
}

object CommTest {
  def makeEndpoint(spec: String): Endpoint = {
    EndpointFactory.fromString(spec, defaultPort = Defaults.listenPort)
  }

  def main(args: Array[String]) {
    val conf = new Conf(args)

    val listen = makeEndpoint(conf.listen())

    val peers = (conf.peers() split ",")
      .filter { x => x != "" }
      .map { x => makeEndpoint(x) }

    peers foreach { x => println("peer: " + x) }

    val db = new KeyValueStore

    println(conf.summary)

    val me = UUID.randomUUID
    println(s"I am $me")


    val cmdQueue = new java.util.concurrent.LinkedBlockingQueue[KeyValueCommand]

    val comm = 
      conf.transport() match {
        case "zeromq" =>
          new ZeromqComm(listen, peers)
        case "netty" =>
          new NettyComm(listen, peers)
      }

    val mutator = new Mutator(me, comm, db, cmdQueue)
    mutator start

    val http = new HttpServer(conf.httpPort(), db, me, cmdQueue)
    http start

    val receiver = new Receiver(comm, cmdQueue)
    receiver.start

    // for (i <- 1 to 10) {
    //   Thread.sleep(1000)
    //   (comm send (s"ME: $me ($i)" getBytes)) foreach { r =>
    //     println(
    //       r match {
    //         case Response(d) => s"data: $d: ‘" + new String(d) + "’"
    //         case Error(msg) => s"error: $msg"
    //       }
    //     )
    //   }
    // }
  }
}
