package com.vincentcodes.io;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * This implementation is used for UdpServerSocket only.
 */
public class UdpSocketServerImpl extends UdpSocket {
    // Use the server socket for send only.
    private final DatagramSocket serverSocket;
    private final DatagramPacket outPacket;
    
    private boolean isClosed = false;

    /**
     * Creates a normal socket which connects to a server. 
     * @param serverSocket all UdpSocket from accept() shares the same server socket, 
     * so that all udp packets all come from the same port
     * @param destAddr
     * @throws SocketException
     */
    public UdpSocketServerImpl(DatagramSocket serverSocket, InetSocketAddress destAddr, boolean reliable) throws SocketException{
        super();
        this.serverSocket = serverSocket;
        this.outPacket = new DatagramPacket(new byte[PAYLOAD_LENGTH], PAYLOAD_LENGTH, destAddr);
        super.reliable = reliable;
    }

    public UdpSocketServerImpl(DatagramSocket serverSocket, String dest, int port, boolean reliable) throws SocketException{
        this(serverSocket, new InetSocketAddress(dest, port), reliable);
    }

    public UdpSocketServerImpl(DatagramSocket serverSocket, Inet4Address dest, int port, boolean reliable) throws SocketException{
        this(serverSocket, new InetSocketAddress(dest, port), reliable);
    }

    /**
     * Allows sending payload size greater than SPECIFIED_BUF_LENGTH with 
     * one udp packet. This is actually faster than 
     * {@link #sendUnsafe(byte[], int, int)}
     */
    protected void sendUnsafe(byte[] bytes) throws IOException{
        outPacket.setData(bytes);
        serverSocket.send(outPacket);
    }
    protected void sendUnsafe(byte[] bytes, int offset, int length) throws IOException{
        outPacket.setData(bytes, offset, length);
        serverSocket.send(outPacket);
    }

    // Use addData(byte[] bytes) to receiveData

    @Override
    public InetAddress getRemoteAddress() {
        return outPacket.getAddress();
    }

    @Override
    public int getRemotePort() {
        return outPacket.getPort();
    }

    @Override
    public String getRemoteFullAddr() {
        return getRemoteAddress().getHostAddress() + ":" + getRemotePort();
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public void close() throws IOException {
        isClosed = true;
    }
    
}
