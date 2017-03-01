package edu.mtu.cs3421.voto;

import android.graphics.BitmapFactory;
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
    private static final String
            VOTE_HEADER = "vote_",
            HANDSHAKE_HEADER = "handshakeRequest_";

    // Message Type Values
    private static final int
            MESSAGE_TYPE_HANDSHAKE = 1,
            MESSAGE_TYPE_VOTE = 2;

    private static final int SLIDE_PACKET_MAX_SIZE = 71680;

    private final UDPServiceListener listener;
    private final int HOST_PORT;
    private DatagramSocket datagramSocket;
    private InetAddress HOST_INET_ADDRESS;
    private boolean serviceReady;
    private BitmapFactory bitmapFactory;
    private final String ID = ""; //TODO get ID from prefercences

    UDPclient(UDPServiceListener listener, String HOST_IP_STRING) {
        Log.d(TAG, "Opening a UDP socket");
        this.listener = listener;
        HOST_PORT = 9876;
        try {
            HOST_INET_ADDRESS = InetAddress.getByName(HOST_IP_STRING);
            datagramSocket = new DatagramSocket();
            datagramSocket.setSoTimeout(100);
            Log.d(TAG, "IP: " + datagramSocket.getInetAddress() + "PORT:" + datagramSocket.getPort());
            serviceReady = true;
        } catch (Exception e) {
            Log.e(TAG, "Error could not build new datagram socket!");
            serviceReady = false;
        }
    }

    public interface UDPServiceListener {
        void onVoteSuccess(int message_id);
        void onVoteFailure(int vote_id);
        void onHandshakeFailure();
        void onHandshakeResponse(String reply);
    }

    public boolean isServiceReady() {
        return serviceReady;
    }

    public void sendVote(char voteLetter, int voteNumber) {
        String messageString = VOTE_HEADER + "_" + voteLetter;
        new send(messageString.getBytes(), MESSAGE_TYPE_VOTE, voteNumber).start();
    }

    public void sendHandshake() {
        String messageString = HANDSHAKE_HEADER + ID;
        new send(messageString.getBytes(), MESSAGE_TYPE_HANDSHAKE, 0).start();
    }

    private class send extends Thread {

        private byte[] message;
        byte[] buffer = new byte[512];
        private final int TYPE, voteID;

        send(byte[] message, int TYPE, int voteID) {
            this.message = message;
            this.TYPE = TYPE;
            this.voteID = voteID;
        }

        @Override
        public void run() {
            Log.d(TAG, "Sending Message");
            int attempts = 0;

            DatagramPacket packet = new DatagramPacket(message, message.length, HOST_INET_ADDRESS, HOST_PORT);

            boolean waitingReply = true;

            while (waitingReply) {
                try {
                    datagramSocket.send(packet);
                    DatagramPacket rp = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(rp);

                    waitingReply = false;

                    Log.d(TAG, "Send Successful");
                    messageSendSuccessful(new String(rp.getData()).trim());

                } catch (SocketTimeoutException e) {

                    Log.d(TAG, "Timeout Reached, resending...");
                    if (attempts == 6) {
                        Log.e(TAG, "Error to many attempts with no response!");
                        messageFailureAlert();
                        return;
                    }
                    attempts += 1;

                } catch (IOException e) {
                    Log.e(TAG, "IO Error on send");
                    messageFailureAlert();
                    return;
                }
            }
        }

        private void messageFailureAlert(){
            switch (TYPE) {
                case MESSAGE_TYPE_HANDSHAKE:
                    listener.onHandshakeFailure();
                    break;
                case MESSAGE_TYPE_VOTE:
                    listener.onVoteFailure(voteID);
                    break;
            }
        }

        private void messageSendSuccessful(String response){
            switch (TYPE) {
                case MESSAGE_TYPE_HANDSHAKE:
                    listener.onHandshakeResponse(response);
                    break;
                case MESSAGE_TYPE_VOTE:
                    listener.onVoteSuccess(voteID);
                    break;
            }
        }
    }

    private class loadSlide extends Thread{

        byte[] slideBuffer; byte[] rpBuffer = new byte[SLIDE_PACKET_MAX_SIZE];
        int slideLength;

        loadSlide(){

        }
        @Override
        public void run() {
            Log.d(TAG, "Retrieving slide data");
            byte[] message = "GET>>".getBytes();
            DatagramPacket packet = new DatagramPacket(message, message.length, HOST_INET_ADDRESS, HOST_PORT);

            boolean notFinished = true;
            while(notFinished){
                try {
                    datagramSocket.send(packet);
                    DatagramPacket rp = new DatagramPacket(rpBuffer, rpBuffer.length);
                    datagramSocket.receive(rp);
                    proccessMessage(rpBuffer);

                } catch (SocketTimeoutException e) {
                    Log.d(TAG, "Timeout Reached, resending...");

                } catch (IOException e) {
                    Log.e(TAG, "IO Error on send");
                    return;
                }
            }
        }

        private void proccessMessage(byte[] message){

            // Process based on message header fields.
            switch(message[1]){

                // Data packet containing more image bytes
                case 'D':

                    break;

                // Start packet describing the entire slide
                case 'S':

                    break;
            }
        }
    }
}
