package edu.mtu.cs3421.voto;

import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.IOException;
import java.net.*;

/**
 * Created for Voto
 * By: nicholas on 2/3/17.
 * Description:
 */

public class UDPclient {

    private static final String TAG = "UDP-Service";

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
    private String myID = ""; //TODO get myID from prefercences

    UDPclient(UDPServiceListener listener, String HOST_IP_STRING) {
        Log.d(TAG, "Opening a UDP socket");
        this.listener = listener;
        HOST_PORT = 9876;
        try {
            HOST_INET_ADDRESS = InetAddress.getByName(HOST_IP_STRING);
            datagramSocket = new DatagramSocket();
            datagramSocket.setSoTimeout(500);
            Log.d(TAG, "IP: " + datagramSocket.getLocalAddress().toString() + "PORT:" + datagramSocket.getPort());
            serviceReady = true;
        } catch (Exception e) {
            Log.e(TAG, "Error could not build new datagram socket!");
            serviceReady = false;
            e.printStackTrace();
        }
    }

    public void setMyID(String id){
        if(id == null)
            myID = datagramSocket.getLocalAddress().toString();
        else
            myID = id;
    }

    public interface UDPServiceListener {
        void onVoteSuccess(int message_id);
        void onVoteFailure(int vote_id);
        void onHandshakeFailure();
        void onHandshakeResponse(String reply);
        void onMediaAvailable(Media media);
    }

    public boolean isServiceReady() {
        return serviceReady;
    }

    public void sendVote(String vote, byte voteNumber) {
        new send(MessageUtility.getVoteMessage(myID,vote,voteNumber), MESSAGE_TYPE_VOTE, voteNumber).start();
    }

    public void sendHandshake() {

        new send(MessageUtility.getHandshakeRequestMessage(myID), MESSAGE_TYPE_HANDSHAKE, 0).start();
    }

    public void pollNewMedia(){
        new MediaLoader().start();
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

            while (true) {
                try {
                    datagramSocket.send(packet);
                    DatagramPacket rp = new DatagramPacket(buffer, buffer.length);
                    datagramSocket.receive(rp);

                    Log.d(TAG, "Send Successful");
                    messageSendSuccessful(new String(rp.getData()).trim());
                    return;

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

    private class MediaLoader extends Thread{
        byte[] rpBuffer = new byte[SLIDE_PACKET_MAX_SIZE];
        Media media;
        MediaLoader(){
        }
        @Override
        public void run() {
            Log.d(TAG, "Pinging for new slide");

            // Get the initial media ping message.
            byte[] msgOut = MessageUtility.getMediaPingMessage();
            DatagramPacket packet = new DatagramPacket(msgOut, msgOut.length, HOST_INET_ADDRESS, HOST_PORT);

            while(true){

                try {
                    // Sleep and retry again in 1 second.
                    sleep(1000);

                    datagramSocket.send(packet);
                    DatagramPacket rp = new DatagramPacket(rpBuffer, rpBuffer.length);
                    datagramSocket.receive(rp);

                    MediaResponse res = new MediaResponse();
                    MessageUtility.parseMediaPing(rp.getData(),res);

                    // Only update system if this is a new image
                    if(media == null || (media.getImgID() != res.imgID)){

                        media = new Media(res.imgID, res.packetCount, res.imgLength);

                        // Blocks until entire slide is loaded, then alerts the UI
                        loadMedia();
                        listener.onMediaAvailable(media);
                    }

                } catch (SocketTimeoutException e) {
                    Log.d(TAG, "Timeout Reached on media ping");

                } catch (IOException e) {
                    Log.e(TAG, "IO Error on send");
                    return;
                } catch (InterruptedException e) {
                    Log.e(TAG, "SLEEP ISSUE MEDIA PINGER");
                }
            }
        }

        private boolean loadMedia(){
            while (true) {
                try {

                    // Get the mediaRequestMessage.
                    byte[] msgOut = MessageUtility.getMediaRequestMessage(media.getImgID(),media.getExpectingPacketNumber());

                    // Build and send the packet.
                    DatagramPacket packet = new DatagramPacket(msgOut, msgOut.length, HOST_INET_ADDRESS, HOST_PORT);
                    datagramSocket.send(packet);

                    // Wait for the data to come back.
                    DatagramPacket rp = new DatagramPacket(rpBuffer, rpBuffer.length);
                    datagramSocket.receive(rp);
                    byte[] msgIn = rp.getData();

                    // Process the message
                    if(! MessageUtility.parseMediaResponse(msgIn,media) ){
                        Log.e(TAG, "Error wrong headers media request response");
                        return false;
                    }

                    if(media.isReady()){
                        return true;
                    }
                } catch (SocketTimeoutException e) {
                    Log.d(TAG, "Timeout Reached, resending...");

                } catch (IOException e) {
                    Log.e(TAG, "IO Error on send");
                    return false;
                }
            }
        }
    }
//
    class MediaResponse{
        int imgLength;
        byte imgID;
        byte packetCount;
        MediaResponse(){

        }
    }
}
