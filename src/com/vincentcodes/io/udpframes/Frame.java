package com.vincentcodes.io.udpframes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import com.vincentcodes.io.UdpSocket;

/**
 * Custom frames for Vincent's custom udp loss packet prevention protocol
 * <p>
 * The way of sending frames is shown as follows:
 * START, DATA..., END, (wait for) REPORT
 * <p>
 * If report frame has payload, then server go 
 * through the same procedure again until there 
 * are no lost packets 
 * <p>
 * When you are re-sending the packets, do no 
 * generate new seq numbers for the requested 
 * packets. 
 * <p>
 * Do give the first packet type 1 and
 * the last packet type 2. If there is only 
 * 1 packet needs to be re-sent, give it type 2
 */
public class Frame {
    /**
     * 0 - data,
     * 1 - start of data
     * 2 - end of data
     * 3 - report frame (payload contains missing seq numbers)
     */
    public int type; // 1 byte

    /**
     * Sequence number (used to put packets back into order)
     * Starts from 0. Can be used as array index
     */
    public int seq; // 4 bytes

    /**
     * Unlimited (but the max length that achieves best performance is 1472-5)
     * 
     * For report frame, the payload contains the missing seq number of the 
     * frames separated by comma (empty if there aren't lost packets)
     * eg. 2,5,9
     */
    public byte[] payload;

    public Frame(){}
    public Frame(int type, int seq, byte[] payload){
        this.type = type;
        this.seq = seq;
        this.payload = payload;
    }

    // This method hinders performance
    // public String toString(){
    //     return String.format("{Frame type: %d, seq: %d, payload: %s}", type, seq, payload.toString() + " " + payload.length);
    // }
    
    // private static ByteBuffer buffer = ByteBuffer.allocate(4);
    private static ByteArrayOutputStream baos = new ByteArrayOutputStream(UdpSocket.PACKET_LENGTH);

    public static Frame parse(byte[] bytes){
        Frame frame = new Frame();
        
        frame.type = bytes[0];
        frame.seq = getIntFrom4Bytes(bytes, 1);
        frame.payload = Arrays.copyOfRange(bytes, 5, bytes.length);
        return frame;
    }
    public byte[] toBytes(){
        try{
            baos.reset();
            
            // Write type
            baos.write(type);

            // Write sequence
            baos.write(intToByteArray(seq));

            // Write payload
            baos.write(payload);
        }catch(IOException ignored){}
        return baos.toByteArray();
    }

    private static byte[] intToByteArray(int value){
        return new byte[]{(byte)(value >> 24), (byte)(value >> 16), (byte)(value >> 8), (byte)value};
    }

    private static int getIntFrom4Bytes(byte[] bytes, int startingIndex){
        return ((bytes[startingIndex] & 0xff) << 8*3) | ((bytes[startingIndex+1] & 0xff) << 8*2) | ((bytes[startingIndex+2] & 0xff) << 8) | (bytes[startingIndex+3] & 0xff);
    }
}
