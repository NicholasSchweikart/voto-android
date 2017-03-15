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
            Log.d(TAG, "IP: " + datagramSocket.getLocalAddress().toString() + "PORT: " + datagramSocket.getPort());
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
        new Voter(vote,voteNumber).start();
    }

    public void sendHandshake() {

        new Handshake().start();
    }

    public void pollNewMedia(){
        new MediaLoader().start();
    }

    private class Handshake extends Thread {
        private final int timeout = 500;
        private byte[] message;
        byte[] buffer = new byte[512];

        Handshake() {

        }

        @Override
        public void run() {
            Log.d(TAG, "Attempting Handshake...");

            message = MessageUtility.getHandshakeRequestMessage(myID);

            DatagramPacket packet = new DatagramPacket(message, message.length, HOST_INET_ADDRESS, HOST_PORT);

            byte[] res = sendMessage(packet,datagramSocket, 512, timeout);

            if(res == null){
                Log.e(TAG, "Hanshake Failure");
                listener.onHandshakeFailure();
            }
            else{
                listener.onHandshakeResponse(new String(res).trim());
            }

        }
    }

    private class Voter extends Thread {
        private final int timeout = 500;
        private byte[] message;
        byte[] buffer = new byte[512];
        private final String vote;
        private final byte voteNumber;

        Voter(String vote, byte voteNumber) {
            this.vote = vote;
            this.voteNumber = voteNumber;
        }

        @Override
        public void run() {
            Log.d(TAG, "Attempting Vote...");

            message = MessageUtility.getVoteMessage(myID,vote,voteNumber);

            DatagramPacket packet = new DatagramPacket(message, message.length, HOST_INET_ADDRESS, HOST_PORT);

            byte[] res = sendMessage(packet,datagramSocket, 512, timeout);

            if(res == null){
                Log.e(TAG, "Vote Failure");
                listener.onVoteFailure(voteNumber);
            }
            else{
                Log.d(TAG, "Vote Success");
                listener.onVoteSuccess(voteNumber);
            }

        }
    }

    private class MediaLoader extends Thread{
        private final int timeout = 500;
        Media media;
        MediaLoader(){

        }
        @Override
        public void run() {
            Log.d(TAG, "Pinging for new slide...");

            // Get the initial media ping message.
            byte[] mediaPingMessage = MessageUtility.getMediaPingMessage();
            DatagramPacket packet = new DatagramPacket(mediaPingMessage, mediaPingMessage.length, HOST_INET_ADDRESS, HOST_PORT);

            while(true){

                byte[] res = sendMessage(packet, datagramSocket,512,timeout);

                if(res == null){
                    Log.e(TAG, "Failed to ping for new media");
                    return;
                }

                MediaResponse response = new MediaResponse();
                MessageUtility.parseMediaPing(res,response);

                // Only update system if this is a new image
                if(media == null || (media.getImgID() != response.imgID)){
                    Log.d(TAG, "-- New Media Available --");
                    media = new Media(response.imgID, response.packetCount, response.imgLength);

                    // Blocks until entire slide is loaded, then alerts the UI
                    if(!loadMedia()){
                        Log.e(TAG, "Failed to load media");
                        media = null;
                    }else{
                        listener.onMediaAvailable(media);
                    }

                }else{
                    try {
                        sleep(2000);
                    } catch (InterruptedException e) {
                        Log.e(TAG,"Failed to sleep");
                    }
                }

            }
        }

        private boolean loadMedia(){
            while (true) {

                // Get the mediaRequestMessage.
                byte[] msgOut = MessageUtility.getMediaRequestMessage(media.getImgID(),media.getExpectingPacketNumber());

                // Build and send the packet.
                DatagramPacket packet = new DatagramPacket(msgOut, msgOut.length, HOST_INET_ADDRESS, HOST_PORT);
                byte[] msgIn = sendMessage(packet,datagramSocket,SLIDE_PACKET_MAX_SIZE,timeout);

                if(msgIn == null)
                    return false;

                // Process the message
                if(! MessageUtility.parseMediaResponse(msgIn,media) ){
                    Log.e(TAG, "Error wrong headers media request response");
                }

                if(media.isReady()){
                    return true;
                }

            }
        }
    }
    private byte[] sendMessage(DatagramPacket packet, DatagramSocket socket, int bufferSize, int timeout){
        int attempts = 0;
        byte[] buffer = new byte[bufferSize];
        while (true) {
            try {
                datagramSocket.setSoTimeout(timeout);
                socket.send(packet);
                DatagramPacket rp = new DatagramPacket(buffer, buffer.length);
                socket.receive(rp);

                Log.d(TAG, "Send Successful");
                return rp.getData();

            } catch (SocketTimeoutException e) {

                Log.d(TAG, "Timeout Reached, resending...");
                if (attempts == 6) {
                    Log.e(TAG, "Error to many attempts with no response!");
                    return null;
                }
                attempts += 1;

            } catch (IOException e) {
                Log.e(TAG, "IO Error on send");
                return null;
            }
        }
    }

    class MediaResponse{
        int imgLength;
        byte imgID;
        int packetCount;
        MediaResponse(){

        }
    }
}
