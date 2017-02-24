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

public class UDPclient {

    public static final String TAG = "UDP-Service";

    // Message Headers
    public static final String
            VOTE_HEADER = "vote_",
            HANDSHAKE_HEADER = "handshakeRequest_";

    // Handler response values
    public static final int
            MESSAGE_TYPE_HANDSHAKE = 1,
            MESSAGE_TYPE_VOTE = 2;
    private final UDPServiceListener listener;
    private int HOST_PORT, MY_PORT;
    private DatagramSocket datagramSocket;
    private InetAddress HOST_INET_ADDRESS, MY_INET_ADDRESS;

    private Handler handler;

    private String ID = "";

    UDPclient(UDPServiceListener listener, String HOST_IP_STRING) throws SocketException, UnknownHostException {
        Log.d(TAG, "Opening a UDP socket");
        this.listener = listener;
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
            throw e;
        }
    }

    public interface UDPServiceListener{
        void onVoteSuccess(int message_id);
        void onHandshakeResponse(String reply);
    }

    public void sendVote(char voteLetter, int voteID) {
        String messageString = VOTE_HEADER + "_" + voteLetter;
        new send(messageString.getBytes(),MESSAGE_TYPE_VOTE, voteID).start();
    }

    public void sendHandshake(){
        String messageString = HANDSHAKE_HEADER + ID;
        new send(messageString.getBytes(), MESSAGE_TYPE_HANDSHAKE, 0).start();
    }

    private class send extends Thread {

        private  byte[] message;
        byte[] buffer = new byte[512];
        private final int TYPE, voteID;
        send(byte[] message, int TYPE, int voteID){
            this.message = message;
            this.TYPE = TYPE;
            this.voteID = voteID;
        }

        @Override
        public void run(){
            Log.d(TAG, "Sending Message");
            int attempts = 0;

            DatagramPacket packet = new DatagramPacket(message, message.length, HOST_INET_ADDRESS, HOST_PORT);

            boolean waitingReply = true;

            while(waitingReply){
                try {
                    datagramSocket.send(packet);
                    DatagramPacket rp = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(rp);

                    waitingReply = false;

                    Log.d(TAG, "Send Successful");
                    switch (TYPE){
                        case MESSAGE_TYPE_HANDSHAKE:
                            listener.onHandshakeResponse(new String(rp.getData()).trim());
                            break;
                        case MESSAGE_TYPE_VOTE:
                            listener.onVoteSuccess(voteID);
                            break;
                    }

                }catch (SocketTimeoutException e){

                    Log.d(TAG,"Timeout Reached, resending...");
                    if(attempts == 6){
                        Log.e(TAG, "Error to many attempts with no response!");

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
