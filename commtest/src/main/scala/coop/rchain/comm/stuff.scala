import org.zeromq.ZMQ
import coop.rchain.comms.{Code, Envelope, QuickResponse}

object util {
  def makeBytes(x: String): com.google.protobuf.ByteString =
    com.google.protobuf.ByteString.copyFromUtf8(x)
}

object zmqtestPair {
  val context = ZMQ.context(1)

  def send = {
    val s = context.socket(ZMQ.PAIR)
    val url = "tcp://*:10203"
    s.bind(url)

    val env = Envelope()
      .withVersion(1)
      .withTransactionNumber(123)
      .withNodeId(util.makeBytes("FancyZMQNode"))
      .withMethod(util.makeBytes("SomeZMQMethod"))
      .withPayload(util.makeBytes("ZMQFOOBARBAZZMQ"))
      .withTimeSinceEpoch(987654321)

    for (i <- 1 to 10) {
      val buf = new java.io.ByteArrayOutputStream()
      (env withPayload util.makeBytes("#{ZMQ-PROTOBUF TEST " + i + "}")) writeTo buf
      val barray = (buf toByteArray)
      val resp = s send (barray, 0)
      println("SENT", barray, (barray size), resp)
    }
  }

  def recv = {
    val s = context.socket(ZMQ.PAIR)
    val url = "tcp://localhost:10203"
    s.connect(url)

    for (i <- 1 to 10) {

      print("waiting...")

      val barray = (s recv 0)

      println("recv", i)
      barray match {
        case null => println("nothing here")
        case _ => {
          val env = Envelope.parseFrom(barray)
          println("RECV", barray, env)
        }
      }
    }

    s close
  }
}

object zmqtestReqRep {
  val context = ZMQ.context(1)

  def client = {
    val s = context.socket(ZMQ.REQ)
    val url = "tcp://localhost:10203"
    s.connect(url)

    val env = Envelope()
      .withVersion(1)
      .withTransactionNumber(123)
      .withNodeId(util.makeBytes("FancyZMQNode"))
      .withMethod(util.makeBytes("SomeZMQMethod"))
      .withPayload(util.makeBytes("ZMQFOOBARBAZZMQ"))
      .withTimeSinceEpoch(987654321)

    val buf = new java.io.ByteArrayOutputStream()

    for (i <- 1 to 10) {
      (buf reset)
      (env withPayload util.makeBytes("#{ZMQ-PROTOBUF TEST " + i + "}")) writeTo buf
      val barray = (buf toByteArray)
      val resp = s send (barray, 0)
      println("Sent request", barray, (barray size), resp)
      val barray_in = (s recv 0)
      barray_in match {
        case null => println("No response received")
        case _ => {
          val resp = QuickResponse.parseFrom(barray_in)
          println("Received response", barray_in, resp)
        }
      }
    }
  }

  def server = {
    val s = context.socket(ZMQ.REP)
    val url = "tcp://*:10203"
    s.bind(url)

    val buf = new java.io.ByteArrayOutputStream()
    val resp = QuickResponse().withCode(Code.ACK)
    resp writeTo buf
    val barray_out = buf toByteArray

    @annotation.tailrec
    def serve: Unit = {
      print("waiting...")

      val barray_in = (s recv 0)

      // println ("recv", i)

      barray_in match {
        case null => println("nothing here")
        case _ => {
          val env = Envelope.parseFrom(barray_in)
          println("Received request", barray_in, env)
        }
      }

      s send (barray_out, 0)
      println("Sent response", barray_out, resp)

      serve
    }

    serve

  }
}

object zmqtestPubSub {
  val context = ZMQ.context(1)

  def pub = {
    val s = context.socket(ZMQ.PUB)
    val url = "tcp://*:10203"
    s.bind(url)

    val base = 123
    val env = Envelope()
      .withVersion(12)
      .withNodeId(util.makeBytes("FancyZMQNode"))
      .withMethod(util.makeBytes("SomeZMQMethod"))

    val buf = new java.io.ByteArrayOutputStream()

    var i = 0

    @annotation.tailrec
    def loop: Unit = {

      i += 1

      buf reset

      (env
        .withTransactionNumber(base + i)
        .withPayload(util.makeBytes("#{Message #" + i + "}"))
        .withTimeSinceEpoch(System.currentTimeMillis / 1000)
        .withTransactionNumber(base + i)) writeTo buf

      // env writeTo buf
      val barray = (buf toByteArray)
      val resp = s send (barray, 0)
      println("SENT #" + i, barray, (barray size), resp)
      Thread sleep 1000
      loop
    }
    loop
  }

  def sub = {
    val s = context.socket(ZMQ.SUB)
    val url = "tcp://localhost:10203"
    s.connect(url)
    s.subscribe(Array[Byte]())

    for (i <- 1 to 10) {

      val barray = (s recv 0)
      barray match {
        case null => println("Nothing received (?)")
        case _ => {
          val env = Envelope.parseFrom(barray)

          println("Received " + barray + " " + env)

        }
      }
    }
  }
}
