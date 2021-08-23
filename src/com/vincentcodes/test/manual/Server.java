package com.vincentcodes.test.manual;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.vincentcodes.io.UdpServerSocket;
import com.vincentcodes.io.UdpSocket;

public class Server {
    private static ExecutorService executor = Executors.newFixedThreadPool(4);
    public static UdpServerSocket server;

    public static void main(String[] args) throws Exception{
        System.out.println("[*] Starting server on port 1234");
        server = new UdpServerSocket(1234, true);
        server.startListening();

        UdpSocket socket;
        while((socket = server.accept()) != null){
            executor.submit(new ServerThread(socket));
        }

        server.close();
    }
}

class ServerThread extends Thread{
    private final UdpSocket socket;

    public ServerThread(UdpSocket socket){
        this.socket = socket;
    }

    @Override
    public void run(){
        try{
            performanceTest(socket);

            // Total bytes read test (less than 100) (w/ performance test)
            // int totalRead = 0;
            // while(true){
            //     byte[] bytes = socket.recv();
            //     totalRead += bytes.length;
            //     if(bytes.length < 100) break; // [done] is less than 100
            // }
            // System.out.println("Total bytes read: " + totalRead);

            // Echo Server
            // String msg = new String(socket.recv());
            // while(true){
            //     String addr = socket.getRemoteFullAddr();
            //     System.out.println("["+ addr +"] Message from client '"+ msg +"'");
            //     socket.send(("Echo: " + msg).getBytes());
            //     if(msg.contains("[done]")) break;
            //     msg = new String(socket.recv());
            // }

            // Total bytes send test
            // FileInputStream fis = new FileInputStream("resource/test.mp3");
            // byte[] payload = fis.readAllBytes();
            // socket.send(payload);
            // socket.send("[done]".getBytes());
            // System.out.println("Total bytes sent: " + (payload.length + "[done]".length()));
            // fis.close();
            // socket.close();
        }catch(Exception e){
            e.printStackTrace();
        }finally{
            try{
                socket.close();
            }catch(IOException e){ e.printStackTrace(); }
        }
        return;
    }

    /**
     * Tcp and Custom Udp performance test.
     * 
     * It took 87ms to read all messages. As for tcp, it took 12ms.
     */
    private void performanceTest(UdpSocket socket) throws Exception{
        System.out.println("Udp performance test (send all at once):");
        long currentMs = System.currentTimeMillis();
        int totalRead = 0;
        byte[] bytes = socket.recv();
        totalRead += bytes.length;
        System.out.println("Total bytes read: " + totalRead);
        System.out.println("Time taken to read: " + (System.currentTimeMillis() - currentMs) + "ms");

        
        // System.out.println("Udp performance test (multiple send() test):");
        // long currentMs = System.currentTimeMillis();
        // int totalRead = 0;
        // while(true){
        //     byte[] bytes = socket.recv();
        //     totalRead += bytes.length;
        //     if(bytes.length < 100) break; // [done] is less than 100
        // }
        // System.out.println("Total bytes read (including '[done]'): " + totalRead);
        // System.out.println("Time taken to read: " + (System.currentTimeMillis() - currentMs) + "ms");


        System.out.println("Tcp performance test:");
        try(ServerSocket tcpServer = new ServerSocket(1234)){
            Socket tcpSocket = tcpServer.accept();
            currentMs = System.currentTimeMillis();
            InputStream is = tcpSocket.getInputStream();
            System.out.println("Total bytes read: " + is.readAllBytes().length);
            System.out.println("Time taken to read: " + (System.currentTimeMillis() - currentMs) + "ms");
        }
    }
}