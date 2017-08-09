package coop.rchain.comm

import io.circe._
import io.circe.generic._
import io.circe.syntax._

import org.http4s._
import org.http4s.dsl._
import org.http4s.circe._
import org.http4s.server._
import org.http4s.server.blaze._

import coop.rchain.kv._
import java.util.concurrent.BlockingQueue
import java.util.UUID
import coop.rchain.kv.{KeyValueCommand,Header}

@JsonCodec case class KVPair(key: String, value: String)

class HttpServer(port: Int, store: KeyValueStore, me: UUID, commands: BlockingQueue[KeyValueCommand]) {

  var server: Server = null

  def makeBytes(x: String): com.google.protobuf.ByteString =
    com.google.protobuf.ByteString.copyFromUtf8(x)

  def makeCommand(method: String, cmd: String, key: String, value: String): KeyValueCommand = {
    KeyValueCommand()
      .withHeader(Header()
        .withVersion(0)
        .withNodeId(makeBytes(me toString))
        .withMethod(makeBytes(method))
        .withTimestamp((new java.util.Date) getTime)
      )
      .withCommand(makeBytes(cmd))
      .withKey(makeBytes(key))
      .withValue(makeBytes(value))
  }

  val echoService = HttpService {
    case GET -> Root =>
      Ok("Roger that.\n")
    case GET -> Root / "set" / key / value =>
      // store.add(new Key(key), value)
      val cmd = makeCommand("GET", "set", key, value)
      commands add cmd
      Ok(s"Setting $key to $value:\n=====\n$cmd\n=====\n")
    case req @ POST -> Root / "set" =>
      for {
        r <- req.as(jsonOf[KVPair])
        resp <- {
          val cmd = makeCommand("POST", "set", r.key, r.value)
          commands add cmd
          // store.add(new Key(r.key), r.value)
          Ok(s"Setting ${r key} to ${r value}:\n=====\n$cmd\n=====\n")
        }
      } yield (resp)
    case GET -> Root / "get" / key =>
      val query = new Key(key)
      val queryOutcome = QueryTools.queryResultsToArrayString(
        query,
        query.unifyQuery(store),
        store)
      Ok(((queryOutcome asJson) noSpaces) + "\n")
    case GET -> Root / "get" =>
      Ok("Fetch what?!.")
    case GET -> Root / "dump" =>
      val out = new java.io.ByteArrayOutputStream
      Console.withOut(out) {
        store display
      }
      Ok(out toString)
  }

  val bld = BlazeBuilder
    .bindHttp(port, "localhost")
    .mountService(echoService, "/")

  def start = {
    println(s"Starting HTTP on $port.")
    server = bld run
  }

  def stop =
    if (server != null) server shutdownNow
}
