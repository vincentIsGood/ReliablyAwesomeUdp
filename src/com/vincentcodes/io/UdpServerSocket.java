package com.vincentcodes.io;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

// TODO: packet loss problem. Resend if needed
public class UdpServerSocket implements Closeable{
    private DatagramSocket serverSocket;
    private DatagramPacket packetIn;

    private volatile boolean isServerClosed = false;
    private Map<String, UdpSocketServerImpl> ongoingConnections;
    private boolean reliable = false;

    // How to implement a blocking function call in Java
    // https://stackoverflow.com/questions/7735328/implement-a-blocking-function-call-in-java
    private ConcurrentLinkedDeque<UdpSocketServerImpl> untouchedConnections;

    /**
     * @param port to be listened on
     * @param reliable create a udp socket with the ability to re-order and re-request packets
     * @throws SocketException
     */
    public UdpServerSocket(int port, boolean reliable) throws SocketException{
        this.serverSocket = new DatagramSocket(port);
        packetIn = new DatagramPacket(new byte[UdpSocket.MAX_UDP_PACKET_SIZE], UdpSocket.MAX_UDP_PACKET_SIZE);
        ongoingConnections = new HashMap<>();
        untouchedConnections = new ConcurrentLinkedDeque<>();
        this.reliable = reliable;
        
        serverSocket.setReceiveBufferSize(65536);
    }
    /**
     * Enables rawSocket by default
     */
    public UdpServerSocket(int port) throws SocketException{
        this(port, true);
    }

    public void startListening(){
        new Thread("UdpServerSocket connection and data listener"){
            public void run(){
                try{
                    while(!isServerClosed){
                        // Receive a connection (with data, necessary)
                        serverSocket.receive(packetIn);
                        // packetIn.setLength(UdpSocket.PACKET_LENGTH);
                        
                        byte[] payload = Arrays.copyOfRange(packetIn.getData(), 0, packetIn.getLength());
                        
                        // Find / Create a proper socket
                        InetSocketAddress remoteAddr = (InetSocketAddress)packetIn.getSocketAddress();
                        String remoteFullAddr = remoteAddr.getAddress().getHostAddress() + ":" + remoteAddr.getPort();
                        UdpSocketServerImpl client;
                        if(!ongoingConnections.containsKey(remoteFullAddr)){
                            // Create a UdpSocket (pass back the data into the connection socket)
                            // Server socket is shared
                            client = new UdpSocketServerImpl(serverSocket, remoteAddr, reliable);
                            ongoingConnections.put(remoteFullAddr, client);

                            synchronized(untouchedConnections){
                                untouchedConnections.notify();
                                untouchedConnections.add(client);
                            }
                        }else
                            client = ongoingConnections.get(remoteFullAddr);
                        
                        // Add data to the proper socket
                        if(!client.addData(payload))
                            ongoingConnections.remove(remoteFullAddr);
                    }
                }catch(Exception e){
                    e.printStackTrace();
                    close();
                }
                return;
            }
        }.start();
    }

    /**
     * Accepts an incoming connection, before this, you need to invoke
     * {@link #startListening()}
     * @return a connected UdpSocket, null if the server is closed
     */
    public UdpSocket accept() throws IOException{
        if(isServerClosed) return null;
        synchronized(untouchedConnections){
            try{
                while(untouchedConnections.peekLast() == null){
                    untouchedConnections.wait();
                }
            }catch(InterruptedException ignored){}
        }
        return untouchedConnections.pollLast();
    }

    /**
     * @return the port which this server socket listens on
     */
    public int getLocalPort(){
        return serverSocket.getLocalPort();
    }

    public InetAddress getLocalAddress(){
        return serverSocket.getLocalAddress();
    }

    public SocketAddress getLocalSocketAddress(){
        return serverSocket.getLocalSocketAddress();
    }

    public boolean isClosed(){
        return isServerClosed;
    }

    /**
     * @param timeoutMs in miliseconds
     * @throws IOException
     */
    public void setSoTimeout(int timeoutMs) throws IOException{
        serverSocket.setSoTimeout(timeoutMs);
    }

    /**
     * Enable/disable SO_BROADCAST
     */
    public void setBroadcast(boolean on) throws IOException{
        serverSocket.setBroadcast(on);
    }

    /**
     * Closes the server
     */
    @Override
    public void close(){
        isServerClosed = true;
        serverSocket.close();
    }

}
