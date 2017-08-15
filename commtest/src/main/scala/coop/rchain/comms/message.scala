package coop.rchain.comm

import coop.rchain.kv._
import java.util.{UUID,Date}
import java.util.concurrent.BlockingQueue

class MessageFactory(node_id: UUID) {
  def makeBytes(x: String): com.google.protobuf.ByteString =
    com.google.protobuf.ByteString.copyFromUtf8(x)

  val me = makeBytes(node_id toString)

  def header =
    Header()
      .withNodeId(me)
      .withTimestamp((new Date) getTime)

  def mutation(key: String, value: String) =
    Mutation()
      .withKey(makeBytes(key))
      .withValue(makeBytes(value))

  def hello = Hello

  def node(p: Peer) =
    Node()
      .withId(makeBytes(p.id toString))
      .withHost(makeBytes(p.endpoint host))
      .withPort(p.endpoint port)

  def peers(ps: Array[Peer]) =
    Peers()
      .withNodes(ps map node)

  def protocol =
    Protocol().withHeader(header)
}

class MessageHandler(me: UUID, comm: Comm, store: KeyValueStore, queue: BlockingQueue[Protocol]) extends Thread {
  val factory = new MessageFactory(me)

  def sendMutation(key: String, value: String): Unit =
    queue add factory.protocol.withMutation(factory.mutation(key, value))

  def query(query: String) = {
    val key = new Key(query)
    QueryTools.queryResultsToArrayString(key, key.unifyQuery(store), store)
  }

  def dump = {
    val out = new java.io.ByteArrayOutputStream
    Console.withOut(out) {
      store display
    }
    out toString
  }

  val buf = new java.io.ByteArrayOutputStream
  val uuid_str = me toString

  import Protocol.Message
  def handle(msg: Protocol): Unit = {
    msg.message match {

      // Hello: Please add me to your list of peers.
      case Message.Hello(h) => {
        h.node match {
          case Some(n: Node) => {
            println("HELLO NODE: ", n)
            comm.addPeer(
              new Peer(
                UUID.fromString(n.id toString),
                new Endpoint(n.host toStringUtf8, n.port toInt)))
          }
          case None => ()
        }
      }

      case Message.Disconnect(_) => ()

      case Message.Ping(_) => ()
      case Message.Pong(_) => ()

      // GetPeers: Please send me a list of peers (ids and addresses),
      // and be sure to include yourself in that.
      case Message.GetPeers(m) => {
        msg.header match {
          case Some(h) => {
            val caller = UUID.fromString(h.nodeId toStringUtf8)
            val peers = comm.getPeers ++ Array(comm.peer)
            val resp = factory.protocol.withPeers(factory.peers(peers))
            buf.reset
            resp.writeTo(buf)
            comm.sendTo(buf.toByteArray, caller)
            println(s"GET PEERS FROM NODE $caller => $peers YIELDS $resp")
          }
          case None => ()
        }
      }
      case Message.Peers(p) => {
        p.nodes foreach { p =>
          comm.addPeer(
            new Peer(
              UUID.fromString(p.id toStringUtf8),
              new Endpoint(p.host toStringUtf8, p.port)))
        }
      }

      // GetBlocks: Please send me the list of mutations you have
      // recorded against the store.
      case Message.GetBlocks(_) => ()
      case Message.Blocks(_) => ()

      case Message.Mutation(m) => {
        store.add(new Key(m.key toStringUtf8), m.value toStringUtf8)
        msg.header match {
          case Some(h: Header) => {
            println(h)
            // If this mutation originated here, propagate it to all
            // peers.
            if (h.nodeId.toStringUtf8 == uuid_str) {
              buf.reset
              msg.writeTo(buf)
              comm.send(buf.toByteArray)
            }
          }
          case None => {
            println("No header?")
          }
        }
      }

      case Message.Empty => {
        println("Got EMPTY message; that ain't good.")
      }
    }
  }

  override def run(): Unit =
    while (true) handle(queue take)
}
