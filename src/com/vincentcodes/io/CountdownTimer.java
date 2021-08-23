package com.vincentcodes.io;

/**
 * Time decreases by 1ms
 */
public class CountdownTimer extends Thread{
    private final int TIME;
    private int timelapsed = 0;
    private boolean isStopped = false;
    private boolean resetQueued = true;

    private VoidFunction exitHandler;
    private Object lock = new Object();

    public CountdownTimer(int duration){
        TIME = duration;
    }

    /**
     * If the time is up
     */
    public void setExitHandler(VoidFunction exitHandler){
        this.exitHandler = exitHandler;
    }

    @Override
    public void run(){
        try{
            while(resetQueued){
                resetQueued = false;
                while(!isStopped && timelapsed < TIME){
                    Thread.sleep(1);
                    timelapsed += 1;
                }
                if(!isStopped && exitHandler != null)
                    exitHandler.apply();
                synchronized(lock){
                    lock.wait();
                }
            }
        }catch(InterruptedException ignored){
        }catch(Exception e){
            e.printStackTrace();
        }
        return;
    }

    public void resetTimeLapsed(){
        timelapsed = 0;
    }

    public void reset(){
        resetTimeLapsed();
        resetQueued = true;
        isStopped = false;
        synchronized(lock){
            lock.notify();
        }
    }

    public void stopTimer(){
        isStopped = true;
    }

    public void killTimer(){
        stopTimer();
        interrupt();
    }
}
