package com.vincentcodes.test.manual;

import java.io.FileInputStream;
import java.net.Socket;

import com.vincentcodes.io.UdpSocket;

public class Client {
    public static void main(String[] args) throws Exception{
        System.out.println("[*] Attempting to connect to 127.0.0.1:1234 on udp");
        UdpSocket socket = UdpSocket.create("127.0.0.1", 1234);
        // UdpSocket socket = UdpSocket.createRaw("127.0.0.1", 1234);

        new Thread(){
            public void run(){
                try{
                    performanceTest(socket);
                    socket.close();

                    // Total bytes send test
                    // FileInputStream fis = new FileInputStream("resource/test.mp3");

                    // Method 1: Send all in one go
                    // long currentMs = System.currentTimeMillis();
                    // byte[] payload = fis.readAllBytes();
                    // socket.send(payload);
                    // socket.send("[done]".getBytes());
                    // System.out.println("Total bytes sent: " + ((new File("resource/test.mp3")).length() + "[done]".length()));
                    // System.out.println("Time taken to send: " + (System.currentTimeMillis() - currentMs) + "ms");
                    // fis.close();
                    // socket.close();

                    // Method 2: Send data by separation (re-ordering happens each time you call send())
                    //           But this is better for streaming where few packets fit the data you need
                    // byte[] payload = new byte[131072];
                    // // byte[] payload = new byte[65536];
                    // int byteRead = 0;
                    // while((byteRead = fis.read(payload)) != -1){
                    //     if(byteRead != payload.length)
                    //         socket.send(Arrays.copyOf(payload, byteRead));
                    //     else socket.send(payload);
                    // }
                    // socket.send("[done]".getBytes());
                    // System.out.println("Total bytes sent: " + ((new File("resource/test.mp3")).length() + "[done]".length()));
                    // fis.close();
                    // socket.close();

                    // Echo test
                    // String data = "Aa0Aa1Aa2Aa3Aa4Aa5Aa6Aa7Aa8Aa9Ab0Ab1Ab2Ab3Ab4Ab5Ab6Ab7Ab8Ab9Ac0Ac1Ac2Ac3Ac4Ac5Ac6Ac7Ac8Ac9Ad0Ad1Ad2Ad3Ad4Ad5Ad6Ad7Ad8Ad9Ae0Ae1Ae2Ae3Ae4Ae5Ae6Ae7Ae8Ae9Af0Af1Af2Af3Af4Af5Af6Af7Af8Af9Ag0Ag1Ag2Ag3Ag4Ag5Ag6Ag7Ag8Ag9Ah0Ah1Ah2Ah3Ah4Ah5Ah6Ah7Ah8Ah9Ai0Ai1Ai2Ai3Ai4Ai5Ai6Ai7Ai8Ai9Aj0Aj1Aj2Aj3Aj4Aj5Aj6Aj7Aj8Aj9Ak0Ak1Ak2Ak3Ak4Ak5Ak6Ak7Ak8Ak9Al0Al1Al2Al3Al4Al5Al6Al7Al8Al9Am0Am1Am2Am3Am4Am5Am6Am7Am8Am9An0An1An2An3An4An5An6An7An8An9Ao0Ao1Ao2Ao3Ao4Ao5Ao6Ao7Ao8Ao9Ap0Ap1Ap2Ap3Ap4Ap5Ap6Ap7Ap8Ap9Aq0Aq1Aq2Aq3Aq4Aq5Aq6Aq7Aq8Aq9Ar0Ar1Ar2Ar3Ar4Ar5Ar6Ar7Ar8Ar9As0As1As2As3As4As5As6As7As8As9At0At1At2At3At4At5At6At7At8At9Au0Au1Au2Au3Au4Au5Au6Au7Au8Au9Av0Av1Av2Av3Av4Av5Av6Av7Av8Av9Aw0Aw1Aw2Aw3Aw4Aw5Aw6Aw7Aw8Aw9Ax0Ax1Ax2Ax3Ax4Ax5Ax6Ax7Ax8Ax9Ay0Ay1Ay2Ay3Ay4Ay5Ay6Ay7Ay8Ay9Az0Az1Az2Az3Az4Az5Az6Az7Az8Az9Ba0Ba1Ba2Ba3Ba4Ba5Ba6Ba7Ba8Ba9Bb0Bb1Bb2Bb3Bb4Bb5Bb6Bb7Bb8Bb9Bc0Bc1Bc2Bc3Bc4Bc5Bc6Bc7Bc8Bc9Bd0Bd1Bd2Bd3Bd4Bd5Bd6Bd7Bd8Bd9Be0Be1Be2Be3Be4Be5Be6Be7Be8Be9Bf0Bf1Bf2Bf3Bf4Bf5Bf6Bf7Bf8Bf9Bg0Bg1Bg2Bg3Bg4Bg5Bg6Bg7Bg8Bg9Bh0Bh1Bh2Bh3Bh4Bh5Bh6Bh7Bh8Bh9Bi0Bi1Bi2Bi3Bi4Bi5Bi6Bi7Bi8Bi9Bj0Bj1Bj2Bj3Bj4Bj5Bj6Bj7Bj8Bj9Bk0Bk1Bk2Bk3Bk4Bk5Bk6Bk7Bk8Bk9Bl0Bl1Bl2Bl3Bl4Bl5Bl6Bl7Bl8Bl9Bm0Bm1Bm2Bm3Bm4Bm5Bm6Bm7Bm8Bm9Bn0Bn1Bn2Bn3Bn4Bn5Bn6Bn7Bn8Bn9Bo0Bo1Bo2Bo3Bo4Bo5Bo6Bo7Bo8Bo9Bp0Bp1Bp2Bp3Bp4Bp5Bp6Bp7Bp8Bp9Bq0Bq1Bq2Bq3Bq4Bq5Bq6Bq7Bq8Bq9Br0Br1Br2Br3Br4Br5Br6Br7Br8Br9Bs0Bs1Bs2Bs3Bs4Bs5Bs6Bs7Bs8Bs9Bt0Bt1Bt2Bt3Bt4Bt5Bt6Bt7Bt8Bt9Bu0Bu1Bu2Bu3Bu4Bu5Bu6Bu7Bu8Bu9Bv0Bv1Bv2Bv3Bv4Bv5Bv6Bv7Bv8Bv9Bw0Bw1Bw2Bw3Bw4Bw5Bw6Bw7Bw8";
                    // // socket.send("ASDASDASDASD".getBytes());
                    // socket.send((data + data).getBytes());
                    // Thread.sleep(1);
                    // // socket.send("ASDASDASDASD".getBytes());
                    // socket.send(data.replace("A", "B").getBytes());
                    // Thread.sleep(1);
                    // // socket.send("ASDASDASDASD".getBytes());
                    // socket.send(data.replace("A", "C").getBytes());
                    // socket.send("[done]".getBytes());
                }catch(Exception e){
                    e.printStackTrace();
                }
                return;
            }

            /**
             * Tcp and Custom Udp performance test
             * 
             * As of 16/8/2021 (DD/MM/YYYY), tcp wins. I believe Arrays.copyOfRange took
             * too long to process, creating such difference. 640ms of time is taken to send.
             * 
             * It's 17/8/2021, udp improves by a large margin, it turns out Arrays.copyOfRange
             * is not the significant cause. 85ms of time taken to send. Tcp took 5ms to send.
             * 
             * I guess it's normal to have a slow send() method than tcp.
             * https://stackoverflow.com/questions/35321426/java-datagramsocket-receive-takes-more-time-than-send-for-the-same-packet-si
             * 
             * Finally (a possible solution, use multiple Threads and multiple DatagramSocket to distribute the load):
             * UDP loses.
             * https://forums.codeguru.com/showthread.php?435053-minimize-latency-between-UDP-sends
             */
            private void performanceTest(UdpSocket socket) throws Exception{
                System.out.println("Udp performance test (send all at once):");
                try(FileInputStream fis = new FileInputStream("resource/test.mp3")){
                    byte[] payload = fis.readAllBytes();
                    long currentMs = System.currentTimeMillis();
                    socket.send(payload);
                    System.out.println("Total bytes sent: " + (payload.length));
                    System.out.println("Time taken to send: " + (System.currentTimeMillis() - currentMs) + "ms");
                }

                // System.out.println("Udp performance test (multiple send() test):");
                // try(FileInputStream fis = new FileInputStream("resource/test.mp3")){
                //     byte[] payload = new byte[1467];
                //     long currentMs = System.currentTimeMillis();
                //     int totalRead = 0;
                //     int bufSize = 0;
                //     while((bufSize = fis.read(payload)) != -1){
                //         socket.send(payload);
                //         totalRead += bufSize;
                //     }
                //     socket.send("[done]".getBytes());
                //     System.out.println("Total bytes sent (including '[done]'): " + (totalRead + "[done]".length()));
                //     System.out.println("Time taken to send: " + (System.currentTimeMillis() - currentMs) + "ms");
                // }

                Thread.sleep(50);
                System.out.println("Tcp performance test:");
                long currentMs = System.currentTimeMillis();
                try(FileInputStream fis = new FileInputStream("resource/test.mp3"); Socket tcpSocket = new Socket("127.0.0.1", 1234)){
                    byte[] payload = fis.readAllBytes();
                    tcpSocket.getOutputStream().write(payload);
                    System.out.println("Total bytes sent (ie. file size): " + payload.length);
                    System.out.println("Time taken to send and establish connection: " + (System.currentTimeMillis() - currentMs) + "ms");
                }
            }
        }.start();

        // Echo test
        // String msg;
        // while(true){
        //     msg = new String(socket.recv());
        //     System.out.println("[+] Message from server '" + msg.replace("\n", "") + "'");
        //     if(msg.contains("[done]")) break;
        // }
        // socket.close();
        
        // Total bytes read test
        // int totalRead = 0;
        // while(true){
        //     byte[] bytes = socket.recv();
        //     totalRead += bytes.length;
        //     if(bytes.length < 100) break;
        // }
        // System.out.println("Total bytes read: " + (totalRead + "[done]".length()));
    }
}
