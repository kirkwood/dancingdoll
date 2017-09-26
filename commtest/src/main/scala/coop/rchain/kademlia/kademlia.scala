package coop.rchain.kademlia

import scala.collection.mutable.{HashMap, MutableList, PriorityQueue}

trait Keyed {
  def key: Array[Byte]
}

trait Remote[A] {
  def ping: Unit
}

trait Peer[A] extends Remote[A] with Keyed

trait Latent {
  def latency: Double
}

trait Reputable {
  def reputation: Double
}

object LatencyOrder extends Ordering[Latent] {
  def compare(a: Latent, b: Latent) = a.latency compare b.latency
}

object ReputationOrder extends Ordering[Reputable] {
  def compare(a: Reputable, b: Reputable) = a.reputation compare b.reputation
}

class PeerNode(val pKey: Array[Byte]) extends Keyed {
  val key = pKey
  lazy private val sKey = key map { "%02x" format _ } mkString

  override def toString = s"#{PeerNode ${sKey}}"
}


class PeerTableEntry[A <: Keyed](val entry: A) extends Keyed {
  override def key = entry.key
}

object NodeTable {
  // Number of bits considered in the distance function.
  val width = 256

  // Maximum length of each row of the routing table.
  val redundancy = 20

  // Concurrency factor: system allows up to alpha outstanding network
  // requests at a time.
  val alpha = 3

  // UNIMPLEMENTED: this parameter controls an optimization that can
  // reduce the number hops required to find an address in the network
  // by grouping keys in buckets of a size larger than one.
  //
  // val bucketWidth = 1
}

class PeerTable[A <: Keyed](self: A) {
  val table = new Array[MutableList[PeerTableEntry[A]]](NodeTable.width)
  // val byLatency = PriorityQueue.empty(LatencyOrder.reverse)
  // val byReputation = PriorityQueue.empty(ReputationOrder)
  val pending = new HashMap[Array[Byte], (A, A)]

  private def ping(older: A, newer: A): Unit = {
    println(s"Pinging $older, might replace with $newer")
    pending(older.key) = (older, newer)
  }

  // Kademlia XOR distance function.
  def distance(a: A, b: A): Int = {
    // TODO: ensure keys same length
    var dist = 0
    for (i <- 0 to a.key.size - 1) {
      if (a.key(i) == b.key(i)) {
        dist += 8
      } else {
        for (j <- 7 to 0 by -1) {
          val m = (1 << j).asInstanceOf[Byte]
          if ((a.key(i) & m) != (b.key(i) & m)) {
            return dist + (7 - j)
          }
        }
      }
    }
    dist
  }

  def add(a: A) {
    val ind = distance(self, a)
    // TODO: Bounds check 0 <= ind < table.size
    table(ind) match {
      case null => {
        table(ind) = MutableList[PeerTableEntry[A]](new PeerTableEntry[A](a))
        // byLatency += a
        // byReputation += a
      }
      case l => {
        if (l.size < NodeTable.redundancy) {
          l += new PeerTableEntry[A](a)
        } else {
          // ping first (oldest) element; if it responds, move it to back
          // (newest); if it doesn't respond, remove it and place a in
          // back
          ping(l(0).entry, a)
        }
      }
    }
  }
}
