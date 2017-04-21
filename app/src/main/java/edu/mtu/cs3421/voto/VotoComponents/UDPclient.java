package edu.mtu.cs3421.voto.VotoComponents;


import android.util.Log;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

/**
 * Created for Voto
 * By: Nicholas Schweikart on 2/3/17.
 * Description: Builds an API around multiple UDP sockets for communication with the Voto host.
 */

public class UDPclient {

    private static final String TAG = "UDP-Service";

    private static final int MEDIA_PACKET_MAX_SIZE = 71680;

    // Error codes reported through listener.
    public static final int HANDSHAKE_FAILURE = 1,
        VOTE_FAILURE = 2,
        MEDIA_FAILURE = 3,
        INIT_FAILURE = 4;

    private final UDPServiceListener listener;
    private final int HOST_PORT;
    private InetAddress HOST_INET_ADDRESS;
    private String myID, HOST_IP_STRING; //TODO get myID from prefercences

    private DatagramSocket handshakeSocket, mediaRequestSocket, mediaLoadSocket, voteSocket;
    private MediaLoader mediaLoaderThread;
    private Handshake handshakeThread;
    private Voter voterThread;

    /**
     * Creates and initializes a UDP client for all transactions with Voto host.
     * @param listener {@link UDPclient.UDPServiceListener}
     * @param HOST_IP_STRING Your indented hosts IP address
     * @param myID Your ID for this session.
     */
    public UDPclient(UDPServiceListener listener, String HOST_IP_STRING, String myID) {
        Log.d(TAG, "New UDP Client: HOST: " + HOST_IP_STRING + "myID: " + myID);
        this.listener = listener;
        HOST_PORT = 9876;
        this.HOST_IP_STRING = HOST_IP_STRING;
        this.myID = myID;
        new InitAll().start();
    }

    public interface UDPServiceListener {
        /**
         * Triggered when a vote has been sent and ACKED by the host.
         * @param message_id the ID of the successful vote
         */
        void onVoteSuccess(int message_id);

        /**
         * Triggered when a handshake is AKCED by a Voto Host.
         * @param reply the reply message from the Voto Host
         */
        void onHandshakeResponse(String reply);

        /**
         * Triggered when new media has been downloaded from the Voto Host.
         * @param media {@link Media}
         */
        void onMediaAvailable(Media media);

        /**
         * Triggered when the UDP Client has successfully connected to a Voto Host.
         */
        void onReady();

        /**
         * Triggered when a failure is reported from any activity within the UDP Client.
         * @param failureCode integer failure code (see UDPclient class)
         * @param obj an option obj returned from the failure event. Parse accordingly.
         */
        void onFailure(int failureCode, Object obj);
    }

    /**
     * Sends out a vote to the host
     * @param vote the vote contents
     * @param voteNumber the ID number for the vote
     */
    public void sendVote(String vote, byte voteNumber) {
        new Voter(vote,voteNumber).start();
    }

    /**
     * Sends the target host a handshake message.
     */
    public void sendHandshake() {

        handshakeThread = new Handshake();
        handshakeThread.start();
    }

    /**
     * Starts polling the host for new media
     */
    public void pollNewMedia(){

        mediaLoaderThread = new MediaLoader();
        mediaLoaderThread.start();

    }

    /**
     * Stops the entire system. Cancels all valid threads.
     */
    public void stop(){

        if(mediaLoaderThread != null){
            mediaLoaderThread.cancel();
            mediaLoaderThread = null;
        }
    }

    /**
     * Initializes the UDP stuff. Will report error if an host ip is invalid.
     */
    private class InitAll extends Thread{
        InitAll(){

        }
        @Override
        public void run(){

            try {
                HOST_INET_ADDRESS = InetAddress.getByName(HOST_IP_STRING);
                listener.onReady();
            } catch (Exception e) {
                Log.e(TAG, "Error invalid host!");
                listener.onFailure(INIT_FAILURE, null);
            }

        }
    }

    /**
     * This handles handshaking with a Voto Host. It will run in its own thread, and report any
     * result back through the {@link UDPServiceListener}.
     */
    private class Handshake extends Thread{
        private final int timeout = 500;
        private byte[] message;

        Handshake() {

        }

        /**
         * Get a socket for the handshake to use and attempt to send the message to the target host.
         */
        @Override
        public void run() {
            Log.d(TAG, "Attempting Handshake...");

            // Get a socket for the handshake to use
            if(handshakeSocket == null)
                handshakeSocket = getDatagramSocket();

            if(handshakeSocket == null){
                Log.e(TAG, "Handshake Failure bad socket");
                listener.onFailure(HANDSHAKE_FAILURE,null);
                return;
            }

            // Get a new handshake message for this transaction.
            message = MessageUtility.getHandshakeRequestMessage(myID);

            DatagramPacket packet = new DatagramPacket(message, message.length, HOST_INET_ADDRESS, HOST_PORT);

            // Attempt to send the message to the target host.
            byte[] res = sendMessage(packet, handshakeSocket, 512, timeout);

            if(res == null){
                Log.e(TAG, "Handshake Failure");
                listener.onFailure(HANDSHAKE_FAILURE, null);
            }
            else{

                byte[] trimmed = Arrays.copyOfRange(res, 2, res.length-2);
                listener.onHandshakeResponse(new String(trimmed).trim());
            }

        }

    }

    /**
     * Simplifies the task of sending a Vote reliably to the host over a UDP socket.
     */
    private class Voter extends Thread {
        private final int timeout = 500;
        private final String vote;
        private final byte voteNumber;

        /**
         * Will attempt to send this vote to the Voto Host. This will run on a seperate thread, and
         * report back the result through the {@link UDPServiceListener}.
         * @param vote the vote data to send.
         * @param voteNumber the vote ID number for this vote.
         */
        Voter(String vote, byte voteNumber) {
            this.vote = vote;
            this.voteNumber = voteNumber;
        }

        /**
         * Make sure we can get UDP Socket, construct message, and send vote
         */
        @Override
        public void run() {
            Log.d(TAG, "Attempting Vote...");

            // Init the Vote Socket if this is the first time we are voting.
            if(voteSocket == null)
                voteSocket = getDatagramSocket();

            // Ensure that we where able to actually get a UDP socket.
            if(voteSocket == null){
                Log.e(TAG, "Vote Failure bad socket");
                listener.onFailure(VOTE_FAILURE, voteNumber);
                return;
            }

            // Construct a new vote message
            byte[] message = MessageUtility.getVoteMessage(myID,vote,voteNumber);

            DatagramPacket packet = new DatagramPacket(message, message.length, HOST_INET_ADDRESS, HOST_PORT);

            // Send the vote to the Voto Host
            byte[] res = sendMessage(packet, voteSocket, 512, timeout);

            if(res == null){
                Log.e(TAG, "Vote Failure");
                listener.onFailure(VOTE_FAILURE, voteNumber);
            }
            else{
                byte successfullID = MessageUtility.parseVoteResponse(res);
                if(successfullID != -1) {
                    listener.onVoteSuccess(successfullID);
                }
            }

        }

    }

    /**
     * This class allows for the background loading of all session media slides. It will run forever
     * in its own thread until the {@link UDPclient} is shutdown.
     *
     * New media is automatically detected and sent to the UI thread via the {@link UDPServiceListener}
     */
    private class MediaLoader extends Thread{
        private final int timeout = 500;
        private volatile boolean shutdown = false;
        Media media;

        MediaLoader(){

        }
        @Override
        public void run() {
            Log.d(TAG, "Pinging for new slide...");

            mediaRequestSocket = getDatagramSocket();
            mediaLoadSocket = getDatagramSocket();

            if(mediaRequestSocket == null || mediaLoadSocket == null){
                Log.e(TAG, "Media Loader Failure: cant get sockets!");
                listener.onFailure(MEDIA_FAILURE, null);
                return;
            }

            // Get the initial media ping message.
            byte[] mediaPingMessage = MessageUtility.getMediaPingMessage();
            DatagramPacket packet = new DatagramPacket(mediaPingMessage, mediaPingMessage.length, HOST_INET_ADDRESS, HOST_PORT);

            // Loop until we are shutdown
            while(!shutdown){

                // Send a media request packet to the Voto Host
                byte[] sendResponse = sendMessage(packet, mediaRequestSocket,512,timeout);

                if(sendResponse == null){
                    Log.e(TAG, "Failed to ping for new media");
                    listener.onFailure(MEDIA_FAILURE,null);
                    return;
                }

                MediaResponse response = new MediaResponse();

                if( MessageUtility.parseMediaPing(sendResponse, response) ){

                    // Only update system if this is a new image
                    if(media == null || (media.getImgID() != response.imgID) && (response.imgID != 0)){
                        Log.d(TAG, "-- New Media Available --");

                        media = new Media(response.imgID, response.packetCount, response.imgLength);
                        Log.d(TAG, "MEDIA ID: " + media.getImgID());

                        // Blocks until entire slide is loaded, then alerts the UI
                        if(!loadMedia()){
                            Log.e(TAG, "Failed to load media");
                            media = null;
                            listener.onFailure(MEDIA_FAILURE,null);
                            return;
                        }else{
                            listener.onMediaAvailable(media);
                        }

                    }else{
                        // We sleep for 1 second to limit the ammount of pings to the host
                        try {
                            sleep(1000);
                        } catch (InterruptedException e) {
                            Log.e(TAG,"Failed to sleep");
                            return;
                        }
                    }
                }
            }
        }

        /**
         * Loads the all of the data for the current media object.
         * @return true on success, false on failure
         */
        private boolean loadMedia(){
            while (!shutdown) {

                // Get the mediaRequestMessage.
                byte[] msgOut = MessageUtility.getMediaRequestMessage(media.getImgID(),media.getExpectingPacketNumber());

                // Build and send the packet.
                DatagramPacket packet = new DatagramPacket(msgOut, msgOut.length, HOST_INET_ADDRESS, HOST_PORT);
                byte[] msgIn = sendMessage(packet, mediaRequestSocket, MEDIA_PACKET_MAX_SIZE, timeout);

                if(msgIn == null)
                    return false;

                // Process the message
                if(! MessageUtility.parseMediaResponse(msgIn,media) ){
                    Log.e(TAG, "Error wrong headers media request response");
                    return false;
                }

                // If we have all of the data then stop
                if(media.isReady()){
                    return true;
                }

            }
            return false;
        }

        /**
         * Cancels this thread and effectivly shuts down the media loader.
         */
        public void cancel(){
            shutdown = true;
        }
    }

    /**
     * Implements a reliable send over the UDP socket. It will attempted to send your packet 6 times,
     * on error will return a null value.
     * @param packet the DataGram Packet to send
     * @param socket the Datagram socket you wish to use for this transaction
     * @param bufferSize the size of the response buffer to use
     * @param timeout the timout this socket should use
     * @return null if error, or the byte[] message response.
     */
    private byte[] sendMessage(DatagramPacket packet, DatagramSocket socket, int bufferSize, int timeout){
        int attempts = 0;
        byte[] buffer = new byte[bufferSize];
        while (true) {
            try {
                socket.setSoTimeout(timeout);
                socket.send(packet);
                DatagramPacket rp = new DatagramPacket(buffer, buffer.length);
                socket.receive(rp);

                //Log.d(TAG, "Send Successful");
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

    /**
     * Gets a new Datagram socket for you.
     * @return a new Datagram socket, or null if error.
     */
    private DatagramSocket getDatagramSocket(){
        Log.d(TAG, "Opening new UDP socket");
        DatagramSocket out = null;
        try {
            out = new DatagramSocket();
        } catch (Exception e) {
            Log.e(TAG, "Error could not build new datagram socket!");
        }
        return out;
    }

    /**
     * Contains data for a media response
     */
    class MediaResponse{
        int imgLength;
        byte imgID;
        int packetCount;
        MediaResponse(){

        }
    }
}
