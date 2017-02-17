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

    // Message Headers
    public static final String VOTE_HEADER = "VOTO_placeVote",
            HANDSHAKE_HEADER = "VOTO_handshakeRequest";

    // Handler response values
    public static final int
            MESSAGE_SUCCESS = 1,
            MESSAGE_FAILURE = 2;

    private int HOST_PORT, MY_PORT;
    private DatagramSocket datagramSocket;
    private InetAddress HOST_INET_ADDRESS, MY_INET_ADDRESS;

    private Handler handler;

    UDPService(Handler handler, String HOST_IP_STRING){
        Log.d(TAG, "Building a UDP socket");

        this.handler = handler;
        HOST_PORT = 9876;
        try {
            HOST_INET_ADDRESS = InetAddress.getByName(HOST_IP_STRING);
            datagramSocket = new DatagramSocket();
            datagramSocket.setSoTimeout(200);
            MY_INET_ADDRESS = datagramSocket.getInetAddress();
            MY_PORT = datagramSocket.getPort();
            Log.d(TAG, "IP: " + MY_INET_ADDRESS + "PORT:" + MY_PORT);

        } catch (Exception e) {
            Log.e(TAG, "Error could build new datagram socket!");
        }
    }

    public void sendVote(char voteLetter) {
        String messageString = VOTE_HEADER + voteLetter;
        new send(messageString.getBytes()).start();
    }

    public void sendHandshake(){
        String messageString = HANDSHAKE_HEADER + MY_INET_ADDRESS + '_' + MY_PORT;
        new send(messageString.getBytes()).start();
    }

    private class send extends Thread{

        private  byte[] message;
        byte[] buffer = new byte[512];

        send(byte[] message){
            this.message = message;
        }

        @Override
        public void run(){
            Log.d(TAG, "Sending Message");
            int attempts = 0;
            Message msg = handler.obtainMessage();
            DatagramPacket packet = new DatagramPacket(message, message.length, HOST_INET_ADDRESS, HOST_PORT);

            while(true){
                try {
                    datagramSocket.send(packet);
                    DatagramPacket rp = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(rp);

                    Log.d(TAG, "Send Successful");
                    msg.what = MESSAGE_SUCCESS;
                    handler.sendMessage(msg);

                }catch (SocketTimeoutException e){

                    Log.d(TAG,"Timeout Reached, resending...");
                    if(attempts == 6){
                        Log.e(TAG, "Error to many attempts with no response!");
                        msg.what = MESSAGE_SUCCESS;
                        handler.sendMessage(msg);
                        return;
                    }
                    attempts += 1;

                } catch (IOException e) {
                    Log.e(TAG, "IO Error on send");
                }
            }
        }
    }
}
