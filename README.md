# Summary
The ReliablyAwesomeUdp is a java library created to create a reliable udp communication. Re-ordering and retransmission is the features of this project. It is also possible to use raw UDP Socket (without the fancy udp features but you can still enjoy the syntactic sugar).

I have made the library as close to `Socket` and `ServerSocket` as close as possible except that Udp do not use `InputStream` and `OutputStream`. So, I did not use those to implement this library.

## Basic API
Most of the api revolves around `UdpSocket`. To create a client...
```java
// Use either this
UdpSocket socket = UdpSocket.create("127.0.0.1", 1234);

// Or this
UdpSocket socket = UdpSocket.createRaw("127.0.0.1", 1234);
```

To take a connection from server, do this...
```java
UdpServerSocket server = new UdpServerSocket(1234, true); // true for "reliable", false for "raw"
server.startListening();

UdpSocket socket = server.accept();
```

To send or receive from `UdpSocket`, use `send(byte[])` and `recv()`
```java
socket.send(new byte[]{1,2,3,4});
```

```java
// Receives: byte[]{1,2,3,4}
socket.recv();
```

## Tests
I have made manual tests inside 2 files under src/com/vincentcodes/test/manual/. To use the api, feel free to take a look at the 2 files.

## Drawbacks
The following problems remain unsolved:
- Java's DatagramSocket.send(DatagramPacket) is kinda slow (ie. 0.1-0.5ms per send operation), maybe it's Windows problem?
- It took on average of 85ms to send the whole mp3 file of 4196KB. (Fortunately, the performance on receiving is not that concerning because it's kinda depends on how fast you send)

## Worth mentioning
If you feel the speed is a little bit slow it is possible to increase the payload, but this is not recommended. Anyways, the file you should be looking for to modify is UdpSocket.java. Inside the file, you may modify `PAYLOAD_LENGTH` to `PAYLOAD_LENGTH = 3072`, 3072 is tested and it should work fine.

### Contribution
Feel free to improve the library, I am stuck on how to improve its performance. 