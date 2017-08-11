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

class MessageHandler(store: KeyValueStore, factory: MessageFactory, queue: java.util.concurrent.BlockingQueue[Protocol]) {
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
}
