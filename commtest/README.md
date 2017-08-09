# rchain-comm-doodles
Communication doodles for rchain.

### NOTE
If upon running one of the examples, you see something like:
```
scala> zmqtestReqRep.server
OpenJDK 64-Bit Server VM warning: You have loaded library /tmp/jna5630595667825568300.tmp which might have disabled stack guard. The VM will try to fix the stack guard now.
It's highly recommended that you fix the library with 'execstack -c <libfile>', or link it with '-z noexecstack'.
```
then it's likely your Scala is linking the wrong thing. I don't know how to fix this except to include the correct thing in the `lib/` directory. Here's the one I think I want:
```
$ ls -l /usr/share/java/zmq.jar
lrwxrwxrwx 1 root root 13 Mar 14  2016 /usr/share/java/zmq.jar -> zmq-3.1.0.jar
$ file -L /usr/share/java/zmq.jar
/usr/share/java/zmq.jar: Java archive data (JAR)
$ jar -tf /usr/share/java/zmq.jar
META-INF/
META-INF/MANIFEST.MF
org/zeromq/EmbeddedLibraryTools.class
org/zeromq/ZAuth$1.class
org/zeromq/ZAuth$ZAPRequest.class
org/zeromq/ZAuth$ZAuthAgent.class
org/zeromq/ZAuth.class
org/zeromq/ZContext.class
org/zeromq/ZDispatcher$1.class
org/zeromq/ZDispatcher$SocketDispatcher$1.class
org/zeromq/ZDispatcher$SocketDispatcher$2.class
org/zeromq/ZDispatcher$SocketDispatcher$ZMessageBuffer.class
org/zeromq/ZDispatcher$SocketDispatcher.class
org/zeromq/ZDispatcher$ZMessageHandler.class
org/zeromq/ZDispatcher$ZSender.class
org/zeromq/ZDispatcher.class
org/zeromq/ZFrame.class
org/zeromq/ZLoop$IZLoopHandler.class
org/zeromq/ZLoop$SPoller.class
org/zeromq/ZLoop$STimer.class
org/zeromq/ZLoop.class
org/zeromq/ZMQ$Context.class
org/zeromq/ZMQ$Error.class
org/zeromq/ZMQ$Event.class
org/zeromq/ZMQ$PollItem.class
org/zeromq/ZMQ$Poller.class
org/zeromq/ZMQ$Socket.class
org/zeromq/ZMQ.class
org/zeromq/ZMQException.class
org/zeromq/ZMQForwarder.class
org/zeromq/ZMQQueue.class
org/zeromq/ZMQStreamer.class
org/zeromq/ZMsg.class
org/zeromq/ZThread$IAttachedRunnable.class
org/zeromq/ZThread$IDetachedRunnable.class
org/zeromq/ZThread$ShimThread.class
org/zeromq/ZThread.class
```
So, I set that up:
```
$ mkdir lib/
$ cd lib/
$ ln -s /usr/share/java/zmq.jar .
```
Maybe this is obvious to seasoned Scala/Java people.

## Building
I don't have anything fancy here, yet. I just make a pair of `sbt console`s and invoke everything from there.

## Running
There are currently three test versions, corresponding to different ZeroMQ socket schemes.
### PAIR
Two sockets of equal merit exchange messaeges in any order; the test simply has a sender and a receiver. The one calling `bind` (as opposed to `connect`) should be the longer-lived process:
#### `send`
```
scala> zmqtestPair.send
(SENT,[B@6fead119,63,true)
(SENT,[B@5ec7a318,63,true)
(SENT,[B@28e1d975,63,true)
(SENT,[B@31942c1e,63,true)
(SENT,[B@84f412c,63,true)
(SENT,[B@6b73fc5e,63,true)
(SENT,[B@4dffba34,63,true)
(SENT,[B@5d1b9b49,63,true)
(SENT,[B@2f2ff7e7,63,true)
(SENT,[B@5bca6f50,64,true)
```
#### `recv`
```
scala> zmqtestPair.recv
waiting...(recv,1)
(RECV,[B@687bc5ac,version: 1
node_id: "FancyZMQNode"
method: "SomeZMQMethod"
transaction_number: 123
time_since_epoch: 987654321
payload: "#{ZMQ-PROTOBUF TEST 1}"
)
waiting...(recv,2)
(RECV,[B@5a233920,version: 1
node_id: "FancyZMQNode"
method: "SomeZMQMethod"
transaction_number: 123
time_since_epoch: 987654321
payload: "#{ZMQ-PROTOBUF TEST 2}"
)
...
```

### REQ/REP
Test that works like a web server with one requestor and one responder. The requester (`client`) sends the more complex message in this test, and the server sends back a simple `ACK`:
#### `server`
```
scala> zmqtestReqRep.server
waiting...(Received request,[B@3a810822,version: 1
node_id: "FancyZMQNode"
method: "SomeZMQMethod"
transaction_number: 123
time_since_epoch: 987654321
payload: "#{ZMQ-PROTOBUF TEST 1}"
)
(Sent response,[B@7c10f841,code: ACK
)
...
```
#### `client`
```
scala> zmqtestReqRep.client
(Sent request,[B@39f3d247,63,true)
(Received response,[B@2f253a19,code: ACK
)
...
```

### PUB/SUB
In this example, the publisher sends out one message per second, whether or not there are any subscribers.
#### `pub`
```
scala> zmqtestPubSub.pub
(SENT #1,[B@628b8a87,54,true)
(SENT #2,[B@56a1e676,54,true)
(SENT #3,[B@c1765e2,54,true)
(SENT #4,[B@bdd296a,54,true)
(SENT #5,[B@353c4524,55,true)
(SENT #6,[B@6cf787a3,55,true)
(SENT #7,[B@5b78c7aa,55,true)
(SENT #8,[B@390c33d6,55,true)
...
```
#### `sub`
```
scala> zmqtestPubSub.sub
Received [B@e63fdd2 version: 12
node_id: "FancyZMQNode"
method: "SomeZMQMethod"
transaction_number: 127
time_since_epoch: 1500911544
payload: "#{Message #4}"

Received [B@5263d28c version: 12
node_id: "FancyZMQNode"
method: "SomeZMQMethod"
transaction_number: 128
time_since_epoch: 1500911545
payload: "#{Message #5}"

Received [B@26b18d06 version: 12
node_id: "FancyZMQNode"
method: "SomeZMQMethod"
transaction_number: 129
time_since_epoch: 1500911546
payload: "#{Message #6}"
...
````
