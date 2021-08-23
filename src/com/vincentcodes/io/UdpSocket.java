package com.vincentcodes.io;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import com.vincentcodes.io.udpframes.Frame;

/**
 * This abstract class implements the most basic send/receive functions
 * for UdpSocketImpl and UdpServerSocketImpl.
 */
public abstract class UdpSocket implements Closeable{
    // Maximum length of a datagram packet is specified in a IETF document
    // IPv4 has a theoretical maximum packet size of 65,535 (16-bit)
    // https://stackoverflow.com/questions/42609561/udp-maximum-packet-size/42610200
    public static final int MAX_UDP_PACKET_SIZE = 65507;

    /**
     * default: 1472 (optimal udp size without pppoe. Optimal means that 
     * packet loss occurs less)
     * <p>
     * 5 bytes are used on my reliable udp implementation. Udp frames.
     * @see Frame
     */
    public static final int PAYLOAD_LENGTH = 1472 - 5;
    // public static final int PAYLOAD_LENGTH = 3072;
    public static final int PACKET_LENGTH = 5 + PAYLOAD_LENGTH;
    
    protected ConcurrentLinkedDeque<byte[]> inputData;
    protected ConcurrentLinkedDeque<Frame> inputDataFrames;
    
    protected boolean reliable = true;

    protected UdpSocket(){
        inputData = new ConcurrentLinkedDeque<>();
        inputDataFrames = new ConcurrentLinkedDeque<>();
    }

    /**
     * Creates a normal udp socket where packets are re-ordered and re-requested 
     */
    public static UdpSocket create(String dest, int port) throws SocketException{
        return new UdpSocketImpl(dest, port, true);
    }
    public static UdpSocket create(Inet4Address dest, int port) throws SocketException{
        return new UdpSocketImpl(dest, port, true);
    }
    /**
     * Creates a "raw" udp socket where packets will not be re-ordered and re-requested 
     */
    public static UdpSocket createRaw(String dest, int port) throws SocketException{
        return new UdpSocketImpl(dest, port, false);
    }
    public static UdpSocket createRaw(Inet4Address dest, int port) throws SocketException{
        return new UdpSocketImpl(dest, port, false);
    }

    /**
     * @param data
     * @return If false, it indicates this socket is closed, otherwise, 
     * operation is successful.
     */
    protected boolean addData(byte[] bytes){
        if(isClosed()) return false;
        
        if(!reliable){
            inputData.add(bytes);
            synchronized(inputData){
                inputData.notify();
            }
        }else{
            Frame frame = Frame.parse(bytes);
            inputDataFrames.add(frame);
            // debug("Receiving: " + frame.toString());
            synchronized(inputDataFrames){
                inputDataFrames.notify();
            }
        }
        return true;
    }

    /**
     * Allows sending payload size greater than SPECIFIED_BUF_LENGTH with 
     * one udp packet
     */
    protected abstract void sendUnsafe(byte[] bytes) throws IOException;

    protected abstract void sendUnsafe(byte[] bytes, int offset, int length) throws IOException;

    /**
     * Send bytes to the destination through udp without using frames. 
     * 
     * This is useful if you are creating a custom protocol based on 
     * udp.
     * @see #recvRaw()
     */
    private void sendRaw(byte[] bytes) throws IOException {
        int totalRead = 0; // start from 0
        byte[] payload = new byte[PACKET_LENGTH];
        for(; totalRead + PACKET_LENGTH < bytes.length; totalRead += PACKET_LENGTH){
            System.arraycopy(bytes, totalRead, payload, 0, PACKET_LENGTH);
            // byte[] payload = Arrays.copyOfRange(bytes, totalRead, totalRead + PACKET_LENGTH);
            sendUnsafe(payload);
        }
        if(totalRead + 1 >= bytes.length) return;
        payload = Arrays.copyOfRange(bytes, totalRead, bytes.length);
        sendUnsafe(payload);
    }

    /**
     * This is a blocking operation. It reads all bytes from one packet 
     * and it will not parse the bytes into a frame. 
     * @see #sendRaw(byte[])
     */
    private byte[] recvRaw() throws IOException {
        synchronized(inputData){
            try{
                while(inputData.peekFirst() == null && !isClosed()){
                    inputData.wait();
                }
            }catch(InterruptedException ignored){}
        }
        return inputData.pollFirst();
    }

    /**
     * Send a frame. Sequence number will not be assigned automatically
     * @see #recvFrame()
     */
    private void sendFrame(Frame frame) throws IOException {
        // debug("Sending: " + frame);
        sendUnsafe(frame.toBytes());
    }

    /**
     * Send payload with a {@link com.vincentcodes.io.udpframes.Frame Frame} 
     * using udp. Payload has no limit in size ({@code < Integer.MAX_VALUE}). 
     * Sequence number will be assigned automatically, data frames are sent
     * after start of data frame is sent. End of data frame is sent after
     * data frames are sent.
     * <p>
     * This method also waits for a report frame sent from the client. The 
     * method will re-send the lost packets again until {@code payload.length 
     * == 0}
     * @see #recv()
     */
    public void send(byte[] bytes) throws IOException{
        if(isClosed()) return;

        if(!reliable){
            sendRaw(bytes);
            return;
        }
        
        // Send data: START, DATA..., END
        List<Frame> sentPackets = new ArrayList<>((int)Math.ceil(bytes.length / (PAYLOAD_LENGTH * 1.0)));
        int totalRead = 0; // start from 0
        int seqNum = 0; // start from 0
        boolean firstPacket = true;
        Frame frame = null;
        for(; totalRead + PAYLOAD_LENGTH < bytes.length; totalRead += PAYLOAD_LENGTH){
            byte[] payload = Arrays.copyOfRange(bytes, totalRead, totalRead+PAYLOAD_LENGTH);
            if(firstPacket){
                frame = new Frame(1, seqNum++, payload);
                firstPacket = false;
            }else
                frame = new Frame(0, seqNum++, payload);
            // sendFrame(frame);
            sentPackets.add(frame);
        }
        byte[] payload = Arrays.copyOfRange(bytes, totalRead, bytes.length);
        frame = new Frame(2, seqNum++, payload);
        // sendFrame(frame);
        sentPackets.add(frame);

        
        // Start the timer beforehand. Wait at most 1s for the report frame 
        Container<Frame> lastFrame = new Container<>();
        lastFrame.value = sentPackets.get(sentPackets.size()-1);
        CountdownTimer readTimeoutTimer = new CountdownTimer(1000);
        readTimeoutTimer.setExitHandler(() ->{
            try{
                // Maybe the client didn't receive the last frame
                sendFrame(lastFrame.value);
            }catch(IOException e){
                throw new UncheckedIOException(e);
            }
        });
        
        for(Frame f : sentPackets)
            sendFrame(f);
        
        Frame reportFrame = null;
        boolean isLastFrame = false;
        readTimeoutTimer.start();
        while(true){
            readTimeoutTimer.reset();
            firstPacket = true;
            //Wait for the report frame
            // debug("Waiting for report frame");
            while(true){
                synchronized(inputDataFrames){
                    try{
                        inputDataFrames.wait();
                    }catch(InterruptedException e){close();return;}
                }
                // Catch the latest packet
                if(inputDataFrames.peekLast() != null && inputDataFrames.peekLast().type == 3){
                    isLastFrame =  true;
                    break;
                }
                // just in case recv() took all the packets and the report frame becomes the first
                if(inputDataFrames.peekFirst() != null && inputDataFrames.peekFirst().type == 3){
                    isLastFrame = false;
                    break;
                }
            }
            readTimeoutTimer.stopTimer();
            if(isLastFrame){
                reportFrame = inputDataFrames.pollLast();
            }else{
                reportFrame = inputDataFrames.pollFirst();
            }
            // debug("Report arrived: " + reportFrame.toString());

            // Exit if no packet loss
            if(reportFrame.payload.length == 0) break;
            
            String response = new String(reportFrame.payload).trim();

            reportFrame = null;
            String[] missingPacketsSeq = response.split(",");
            // debug("Respond missing packets: " + Arrays.toString(missingPacketsSeq));
            if(missingPacketsSeq.length == 1){
                int index = Integer.parseInt(response);
                frame = sentPackets.get(index);
                frame.type = 2;
                lastFrame.value = frame;
                sendFrame(sentPackets.get(index));
            }else{
                int index = Integer.parseInt(missingPacketsSeq[0]);
                frame = sentPackets.get(index);
                frame.type = 1;
                sendFrame(sentPackets.get(index));
                for(int i = 1; i < missingPacketsSeq.length-1; i++){
                    String num = missingPacketsSeq[i];
                    index = Integer.parseInt(num);
                    if(index >= 0 && index < sentPackets.size())
                        sendFrame(sentPackets.get(index));
                }
                index = Integer.parseInt(missingPacketsSeq[missingPacketsSeq.length-1]);
                frame = sentPackets.get(index);
                frame.type = 2;
                lastFrame.value = frame;
                sendFrame(sentPackets.get(index));
            }
        }
        readTimeoutTimer.killTimer();
        sentPackets.clear();
    }

    /**
     * Receive data. Will re-order or request lost packets.
     * @see #send(byte[])
     */
    public byte[] recv() throws IOException {
        if(isClosed()) return null;

        if(!reliable){
            return recvRaw();
        }
        
        BooleanContainer readTimeout = new BooleanContainer();
        CountdownTimer receiveTimeoutTimer = new CountdownTimer(1000);
        receiveTimeoutTimer.setExitHandler(() ->{
            readTimeout.value = true;
        });
        receiveTimeoutTimer.start();
        List<Frame> receivedPackets = new ArrayList<>();
        // Take frames until the "End of data" packet
        while(true){
            // Wait for all packets to arrive
            while(true){
                try{
                    Thread.sleep(1);
                }catch(InterruptedException e){close();return null;}
                if(readTimeout.value)
                    throw new IOException("Read timeout, exiting recv()");
                if(inputDataFrames.peekLast() != null && inputDataFrames.peekLast().type == 2){
                    break;
                }
            }
            // Re-order the packets
            while(true){
                Frame frame = inputDataFrames.pollFirst();
                fillArrayToSize(receivedPackets, frame.seq+1);
                receivedPackets.set(frame.seq, frame);
                receiveTimeoutTimer.resetTimeLapsed();
                if(frame.type == 2) break;
            }
            String missingPackets = findNullEleIndexes(receivedPackets, 10000);
            if(missingPackets.length() == 0){
                sendFrame(new Frame(3, receivedPackets.size(), new byte[0]));
                break;
            }
            // debug("Request missing packets: " + missingPackets);
            sendFrame(new Frame(3, receivedPackets.size(), missingPackets.getBytes()));
        }
        receiveTimeoutTimer.killTimer();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for(Frame frame : receivedPackets){
            baos.write(frame.payload);
        }
        receivedPackets.clear();
        return baos.toByteArray();
    }
    private <T> void fillArrayToSize(List<T> list, int size){
        while(list.size() < size){
            list.add(null);
        }
    }
    /**
     * @param limit number of element to take
     * @return
     */
    private <T> String findNullEleIndexes(List<T> list, int limit){
        int counter = 0;
        List<String> indexes = new ArrayList<>();
        for(int i = 0; i < list.size() && counter < limit; i++){
            if(list.get(i) == null){
                indexes.add(Integer.toString(i));
                counter++;
            }
        }
        if(counter == 0) return "";
        return String.join(",", indexes);
    }
    // private <T> int findNumNullEle(List<T> list){
    //     int counter = 0;
    //     for(int i = 0; i < list.size(); i++){
    //         if(list.get(i) == null){
    //             counter++;
    //         }
    //     }
    //     return counter;
    // }

    public abstract InetAddress getRemoteAddress();

    public abstract int getRemotePort();

    public abstract String getRemoteFullAddr();

    /**
     * This should be {@code false} until {@link #close()} is called
     * @return the state of this socket
     */
    public abstract boolean isClosed();

    /**
     * Closes the socket
     */
    @Override
    public abstract void close() throws IOException;

    // private boolean debug = true;
    // private void debug(String msg){
    //     if(debug)
    //         System.out.println(msg);
    // }

}