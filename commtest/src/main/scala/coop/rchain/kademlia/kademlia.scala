package coop.rchain.kademlia

import scala.collection.mutable.{HashMap, MutableList, PriorityQueue}

trait Keyed {
  def key: Array[Byte]
}

trait Remote {
  def ping: Unit
}

trait Peer extends Remote with Keyed

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

class PeerTableEntry[A <: Keyed](val entry: A) extends Keyed {
  override def key = entry.key

  override def toString = s"#{PeerTableEntry $entry}"
}

object NodeTable {
  // Number of bits considered in the distance function. Taken from the passed-in "home" value to the table.
  // val width = 256

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

class PeerTable[A <: Keyed](home: A,
                            val k: Int = NodeTable.redundancy,
                            val alpha: Int = NodeTable.alpha) {
  val width = 8 * home.key.size
  val table = new Array[MutableList[PeerTableEntry[A]]](width)
  // val byLatency = PriorityQueue.empty(LatencyOrder.reverse)
  // val byReputation = PriorityQueue.empty(ReputationOrder)
  val pending = new HashMap[Array[Byte], (A, A)]

  private def ping(older: A, newer: A): Unit = {
    // TODO constrain to alpha in flight
    println(s"STUB: Pinging $older, might replace with $newer.")
    pending synchronized {
      pending.get(older.key) match {
        case Some(_) => {
          // Protocol error -- need throttling
          println(s"Multiple pings in flight for $older.")
        }
        case None => pending(older.key) = (older, newer)
      }
    }
  }

  // Kademlia XOR distance function.
  def distance(a: A, b: A): Int = {
    // TODO: ensure keys same length
    if (a == b || a.key == b.key) {
      return width
    }

    var dist = 0
    for (i <- 0 to width - 1) {
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
    val ind = distance(home, a)
    // TODO: Bounds check 0 <= ind < table.size
    table synchronized {
      table(ind) match {
        case null => {
          table(ind) = MutableList[PeerTableEntry[A]](new PeerTableEntry[A](a))
          // byLatency += a
          // byReputation += a
        }
        case l => {
          if (l.size < k) {
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
}
