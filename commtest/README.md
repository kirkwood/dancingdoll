# rchain-comm-doodles
Communication doodles for rchain.

`coop.rchain.comm.CommTest` is a simple application that attempts to keep _n_ copies of a database in sync. Currently, it only ships mutations around, there is
 1. no discovery protocol; all nodes must be known upfront;
 1. no initial synchronization protocol; and,
 1. no arbitration or consensus
 
If that sounds limiting, then you're right. What it _does_ have is a
very abstracted communication subsystem that will allow us to try out
different mechanisms for getting bytes from point A to point
B. There's also an HTTP server baked into each process, which allows
one to interact with them.

For each copy of this you want to run, decide on
 1. a communication port and
 1. an HTTP port

### Example Invocation

#### Running the things

Suppose I want to run on one physical machine three nodes A, B, and C, listening on ports 33333, 33334, and 33335 (respectively). Give them each an HTTP server, with HTTP ports 8883, 8884, and 8885. Then, we know that for node A (for example), its peers will be listening on ports 33334 and 33335. This table makes the topology explicit:

| Node name (for your reference) | listen address | peer addresses | http interaction port |
| --- | --- | --- | --- |
| A | *:33333 | localhost:33334, localhost:33335 | 8883 |
| B | *:33334 | localhost:33333, localhost:33335 | 8884 |
| C | *:33335 | localhost:33333, localhost:33334 | 8885 |

(If the nodes run on machines with different addresses, they may not need to set the listen address or http port, as the defaults `*:33333` and `8878` may work fine.)

The three nodes may be run in three separate terminals (for instance) with:

Node A
```
$ sbt "run --peers localhost:33334,localhost:33335 --listen *:33333 --http-port 8883"
```

Node B
```
$ sbt "run --peers localhost:33333,localhost:33335 --listen *:33334 --http-port 8884"
```

Node C
```
$ sbt "run --peers localhost:33333,localhost:33334 --listen *:33335 --http-port 8885"
```

#### Interacting with the things
Griff's KV API is attached (loosely) to the HTTP endpoint, so you can issue HTTP requests of the following forms:

##### GET `/:key`
Return a JSON value that is the result of the query in `:key`.

##### GET `/dump`
Return some text which is the debugging print output of the store.

##### GET `/set/:key/:value`
Add the kv pair `(:key, :value)` to the store.

##### POST `/set`
Same as the GET version, but JSON of the form `{"key": :key, "value": :value}` may be in the POST body. This is useful if either `:key` or `:value` is complicated enough to make the command line irritating.

Here's an example interaction with the three processes A, B, and C above (with debugging output shortened in cases):
Add the pair `(A(x), foo)` to the store at node A
```
$ curl localhost:8883/'set/A(x)/foo'
```
and fetch it from node B
```
curl 'localhost:8884/get/A(x)'
["A(x)",["{x} -> [foo]"]]
```
Looks good. Let's set a bunch of values willy-nilly, which I have a script for (these are Griff's test stores):
```
ports="8883\n8884\n8885"
curl localhost:$(echo $ports | shuf -n 1)/set -d '{"key":"A(1)","value":"one"}'
curl localhost:$(echo $ports | shuf -n 1)/set -d '{"key":"A( 2  )","value":"two"}'
curl localhost:$(echo $ports | shuf -n 1)/set -d '{"key":"A(3, 4 ,5)","value":"three"}'
...
```
and then get them from everywhere:
```
$ for p in 8883 8884 8885; do curl -s localhost:${p}/dump > /tmp/dump.${p}; echo $p; done
8883
8884
8885
```
and see whether they agree
```
$ ls -l /tmp/dump.*
-rw-r--r-- 1 kirkwood kirkwood 647 Aug  9 12:45 /tmp/dump.8883
-rw-r--r-- 1 kirkwood kirkwood 647 Aug  9 12:45 /tmp/dump.8884
-rw-r--r-- 1 kirkwood kirkwood 647 Aug  9 12:45 /tmp/dump.8885
$ diff3 /tmp/dump.*
<no output>
```
