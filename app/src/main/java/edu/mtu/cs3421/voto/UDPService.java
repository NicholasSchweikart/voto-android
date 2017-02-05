package edu.mtu.cs3421.voto;

import android.util.Log;

import java.net.*;

/**
 * Created for Voto
 * By: nicholas on 2/3/17.
 * Description:
 */

public class UDPService {

    public static final String TAG = "UDP-Service";
    private final UDPServiceListener listener;
    private int PORT;
    private String HOST_ID;
    private DatagramSocket datagramSocket;

    private InetAddress serverAddress;

    UDPService(UDPServiceListener in, int p, String hostName, String server_id){
        listener = in;
        PORT = p;

        try {
            serverAddress = InetAddress.getByName(hostName);
            HOST_ID = server_id;

            datagramSocket = new DatagramSocket();
            Log.d(TAG, "IP: " + datagramSocket.getInetAddress() + "PORT:" + datagramSocket.getLocalPort());
        } catch (Exception e) {
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
            DatagramPacket packet = new DatagramPacket(message,message.length, serverAddress, PORT);
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
                        Log.d(TAG,"Timeout Reached, resending...");
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            listener.onVoteSent();
        }
    }
}
