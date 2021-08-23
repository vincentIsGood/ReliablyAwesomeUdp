package com.vincentcodes.io;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;

/**
 * A normal udp socket which connects to a udp server.
 */
public class UdpSocketImpl extends UdpSocket{
    private final DatagramSocket socket;
    private final DatagramPacket outPacket;
    private final DatagramPacket inPacket;

    /**
     * Creates a normal socket which connects to a udp server.
     * @param destAddr
     * @throws SocketException
     */
    public UdpSocketImpl(InetSocketAddress destAddr, boolean reliable) throws SocketException{
        super();
        this.socket = new DatagramSocket(); // do not give any params
        this.outPacket = new DatagramPacket(new byte[PAYLOAD_LENGTH], PAYLOAD_LENGTH, destAddr);
        this.inPacket = new DatagramPacket(new byte[MAX_UDP_PACKET_SIZE], MAX_UDP_PACKET_SIZE);
        super.reliable = reliable;

        socket.connect(destAddr);
        socket.setReceiveBufferSize(65536);

        startWorker();
    }

    public UdpSocketImpl(String dest, int port, boolean reliable) throws SocketException{
        this(new InetSocketAddress(dest, port), reliable);
    }

    public UdpSocketImpl(Inet4Address dest, int port, boolean reliable) throws SocketException{
        this(new InetSocketAddress(dest, port), reliable);
    }

    private void startWorker(){
        new Thread("Bytes listener"){
            @Override
            public void run(){
                try{
                    while(!isClosed()){
                        socket.receive(inPacket);
                        // inPacket.setLength(UdpSocket.PACKET_LENGTH);
                        byte[] payload = Arrays.copyOfRange(inPacket.getData(), 0, inPacket.getLength());
                        addData(payload);
                    }
                }catch(Exception e){
                    e.printStackTrace();
                    try{
                        close();
                    }catch(IOException ex){
                        e.printStackTrace();
                    }
                }
                return;
            }
        }.start();
    }

    /**
     * Allows sending payload size greater than SPECIFIED_BUF_LENGTH with 
     * one udp packet. This is actually faster than 
     * {@link #sendUnsafe(byte[], int, int)}
     */
    protected void sendUnsafe(byte[] bytes) throws IOException{
        outPacket.setData(bytes);
        socket.send(outPacket);
        // outPacket.setData(bytes, offset, length); // slower
        // socket.send(new DatagramPacket(bytes, bytes.length));
    }
    protected void sendUnsafe(byte[] bytes, int offset, int length) throws IOException{
        outPacket.setData(bytes, offset, length);
        socket.send(outPacket);
    }

    /**
     * Set DatagramSocket receive timeout
     * @param timeout the amount of time until a SocketTimeoutException to be thrown in milliseconds
     */
    public void setSoTimeout(int timeout) throws SocketException{
        socket.setSoTimeout(timeout);
    }

    /**
     * Enable/disable SO_BROADCAST
     */
    public void setSetBroadcast(boolean on) throws SocketException{
        socket.setBroadcast(on);
    }

    @Override
    public InetAddress getRemoteAddress(){
        return outPacket.getAddress();
    }

    @Override
    public int getRemotePort(){
        return outPacket.getPort();
    }

    @Override
    public String getRemoteFullAddr(){
        return getRemoteAddress().getHostAddress() + ":" + getRemotePort();
    }

    @Override
    public boolean isClosed(){
        return socket.isClosed();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

}