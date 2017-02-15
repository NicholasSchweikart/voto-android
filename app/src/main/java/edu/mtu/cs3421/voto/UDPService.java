package edu.mtu.cs3421.voto;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.*;

/**
 * Created for Voto
 * By: nicholas on 2/3/17.
 * Description:
 */

public class UDPService {

    public static final String TAG = "UDP-Service";

    private int HOST_PORT;
    private DatagramSocket datagramSocket;
    private InetAddress HOST_INET_ADDRESS;

    private Handler handler;

    UDPService(Handler handler, String HOST_IP_STRING){
        Log.d(TAG, "Building a UDP socket");

        this.handler = handler;
        HOST_PORT = 9876;
        try {
            HOST_INET_ADDRESS = InetAddress.getByName(HOST_IP_STRING);
            datagramSocket = new DatagramSocket();
            datagramSocket.setSoTimeout(200);
            Log.d(TAG, "IP: " + datagramSocket.getInetAddress() + "PORT:" + datagramSocket.getLocalPort());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendVote(char voteLetter) {
        byte[] message = new byte[11];
        message = SystemConstants.VOTE_HEADER.getBytes();
        message[10] = ((byte)voteLetter);
        new send(message).start();
    }

    public void sendHandshake(){
        byte[] message = SystemConstants.HANDSHAKE_HEADER.getBytes();
        new send(message).start();
    }

    private class send extends Thread{

        private  byte[] message;
        byte[] buffer = new byte[512];
        private boolean sendSuccessful = false;
        send(byte[] message){
            this.message = message;
        }

        @Override
        public void run(){
            Log.d(TAG, "Sending Message");

            DatagramPacket packet = new DatagramPacket(message, message.length, HOST_INET_ADDRESS, HOST_PORT);

            while(!sendSuccessful){
                try {
                    datagramSocket.send(packet);
                    DatagramPacket rp = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(rp);
                    Log.d(TAG, "Send Successful");
                    sendSuccessful = true;
                }catch (SocketTimeoutException e){
                    Log.d(TAG,"Timeout Reached, resending...");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Message msg = handler.obtainMessage();
            msg.what = SystemConstants.MESSAGE_SUCCESS;
            handler.sendMessage(msg);
        }
    }
}
