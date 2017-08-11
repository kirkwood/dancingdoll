package coop.rchain.comm

import coop.rchain.kv._

class MessageFactory(node_id: java.util.UUID) {
  def makeBytes(x: String): com.google.protobuf.ByteString =
    com.google.protobuf.ByteString.copyFromUtf8(x)

  val me = makeBytes(node_id toString)

  def header =
    Header()
      .withNodeId(me)
      .withTimestamp((new java.util.Date) getTime)

  def mutation(key: String, value: String) =
    Mutation()
      .withKey(makeBytes(key))
      .withValue(makeBytes(value))

  def hello = Hello

  def protocol =
    Protocol().withHeader(header)
}

class MessageHandler(me: java.util.UUID, comm: Comm, store: KeyValueStore, queue: java.util.concurrent.BlockingQueue[Protocol]) extends Thread {
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

  override def run(): Unit = {
    val setCmd = com.google.protobuf.ByteString.copyFromUtf8("set")

    while (true) {
      val cmd = (queue take)

      println(s"COMMAND: $cmd")

      import Protocol.Message
      cmd.message match {
        case Message.Hello(_) => ()
        case Message.Disconnect(_) => ()
        case Message.Ping(_) => ()
        case Message.Pong(_) => ()
        case Message.GetPeers(_) => ()
        case Message.Peers(_) => ()
        case Message.GetBlocks(_) => ()
        case Message.Blocks(_) => ()
        case Message.Mutation(m) => {
          store.add(new Key(m.key toStringUtf8), m.value toStringUtf8)
          cmd.header match {
            case Some(h: Header) => {
              println(h)
              // If this mutation originated here, propagate it to all
              // peers.
              if (h.nodeId.toStringUtf8 == uuid_str) {
                buf.reset
                cmd.writeTo(buf)
                (comm send buf.toByteArray) foreach { r =>
                  println(r match {
                    case Response(d) => s"data: $d: ‘" + new String(d) + "’"
                    case Error(msg) => s"error: $msg"
                  })
                }
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
  }
}
