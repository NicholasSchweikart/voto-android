package edu.mtu.cs3421.voto;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Created for Voto
 * By: nicholas on 2/3/17.
 * Description:
 */

public class UDPService {

    public static final String TAG = "UDP-Service";
    private final UDPServiceListener listener;
    private int PORT;
    private String HOST;
    private DatagramSocket datagramSocket;

    UDPService(UDPServiceListener in){
        listener = in;
        try {
            datagramSocket = new DatagramSocket();
            Log.d(TAG, "IP: " + datagramSocket.getInetAddress() + "PORT:" + datagramSocket.getLocalPort());
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public interface UDPServiceListener{
        void onVoteSent();
    }

    public void sendVote(char voteLetter) {
        byte[] message = new byte[2];
        message[0] = ((byte) 'V');
        message[1] = ((byte)voteLetter);
        new send(message).start();
    }

    private class send extends Thread{
        private  byte[] message;
        byte[] buffer = new byte[1024];
        private boolean sendSuccessful = false;
        send(byte[] message){
            this.message = message;
        }
        @Override
        public void run(){
            Log.d(TAG, "Sending Vote");
            DatagramPacket packet = new DatagramPacket(message,message.length);
            try {
                datagramSocket.send(packet);
                datagramSocket.setSoTimeout(200);

                while(!sendSuccessful){
                    try {
                        DatagramPacket rp = new DatagramPacket(buffer, buffer.length);
                        datagramSocket.receive(rp);
                        Log.d(TAG, "Send Successful");
                        sendSuccessful =true;
                    }catch (SocketTimeoutException e){
                        Log.d(TAG,"Timout Reached, resending...");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            listener.onVoteSent();
        }
    }
}
