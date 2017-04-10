package edu.mtu.cs3421.voto.VotoComponents;

import android.util.Log;

import java.nio.ByteBuffer;

/**
 * Created by Nicholas Schweikart on 3/1/17.
 *
 * This provides a static set of utilities to parse incoming and outgoing Voto messages.
 */
public class MessageUtility {
    private static final String TAG = "message-util";

    // Const flag fields for message headers
    private static final byte
            HANDSHAKE_REQUEST   = (byte) 'R',
            VOTE_REQUEST        = (byte) 'V',
            VOTE_RESPONSE       = (byte) 'R',
            MEDIA_REQUEST       = (byte) 'M',
            MEDIA_PING          = (byte) 'P',
            MEDIA_RESPONSE      = (byte) 'R';

    /**
     * Builds a handshake request message.
     * @param id your unique ID if you have one, NULL if not
     * @return the byte[] message
     */
    public static byte[] getHandshakeRequestMessage(String id){
        byte[] message;
        int i = 0;
        if(id != null){
            byte len = (byte) id.length();
            byte[] idBuff = id.getBytes();
            message = new byte[ 1 + 1 + len];
            message[i++] = HANDSHAKE_REQUEST;
            message[i++] = len;
            System.arraycopy(idBuff,0,message,i,len);
        }else{
            message = new byte[ 1 + 1];
            message[i++] = HANDSHAKE_REQUEST;
            message[i] = 0;
        }
        return message;
    }

    /**
     * Gets a voto vote message for the host.
     * @param id the special ID for the user
     * @param vote the vote data
     * @param voteID the ID number for this vote
     * @return the Voto Vote message to send.
     */
    public static byte[] getVoteMessage(String id, String vote, byte voteID){

        byte[] message;
        int i = 0;
        byte voteLength = ((byte) vote.length());
        byte[] voteData = vote.getBytes();

        if(id != null){
            byte idLength = (byte) id.length();
            byte[] idBuff = id.getBytes();

            // [ V | ID_LEN | ID_DATA | VOTE_ID | VOTE_LEN | VOTE_DATA]
            message = new byte[ 1 + 1 + idLength + 1 + 1 + voteLength];
            message[i++] = VOTE_REQUEST;
            message[i++] = idLength;

            // Copy in the ID data
            System.arraycopy(idBuff,0,message,i,idLength);
            i += idLength;

            message[i++] = voteID;
            message[i++] = voteLength;

            System.arraycopy(voteData,0,message,i,voteLength);

        }else{
            // [ V | ID_LEN | VOTE_ID | VOTE_LEN | VOTE_DATA]
            message = new byte[ 1 + 1 + 1 + 1 + voteLength];
            message[i++] = VOTE_REQUEST;
            message[i++] = 0;           // No ID field
            message[i++] = voteID;
            message[i++] = voteLength;

            System.arraycopy(voteData,0,message,i,voteLength);
        }

        return message;
    }

    /**
     * Gets a media ping message for a Voto Host.
     * @return the ping message
     */
    public static byte[] getMediaPingMessage(){
        byte[] message = new byte[1 + 1];
        message[0] = MEDIA_REQUEST;
        message[1] = MEDIA_PING;
        return message;
    }

    /**
     * Gets a media request message for a voto host.
     * @param imgID the ID of the image you want data from
     * @param packetNumber the image data packet number you need
     * @return the media request message byte array
     */
    public static byte[] getMediaRequestMessage(byte imgID, int packetNumber){
        byte[] message = new byte[3 + 4];
        message[0] = MEDIA_REQUEST;
        message[1] = MEDIA_RESPONSE;
        message[2] = imgID;
        byte[] packetNum = ByteBuffer.allocate(4).putInt(packetNumber).array();
        System.arraycopy(packetNum, 0, message, 3, 4);
        return message;
    }

    /**
     * Parses a vote response message from the host
     * @param message the message byte[] to parse
     * @return the vote ID number that was acknowledged
     */
    public static byte parseVoteResponse(byte[] message){
        byte voteID = -1;
        if(message[0] == VOTE_REQUEST && message[1] == VOTE_RESPONSE){
            voteID = message[2];
        }
        return voteID;
    }

    /**
     * Parses a media ping response message from the HOST
     * @param msg the message from the host
     * @param res a Media Response object to pack with data from this response
     * @return true if the message parse was successful
     */
    public static boolean parseMediaPing(byte[] msg, UDPclient.MediaResponse res){

        // Check for proper message headers
        if(msg[0] == MEDIA_REQUEST && msg[1] == MEDIA_PING ){

            // Extract image ID
            res.imgID = msg[2];

            // Extract packet number
            res.packetCount = ByteBuffer.wrap(msg, 3, 4).getInt();

            res.imgLength = ByteBuffer.wrap(msg, 7, 4).getInt();

            Log.d(TAG, "ID: " + res.imgID + "COUNT: " + res.packetCount + "SIZE: " + res.imgLength);
            return true;
        }

        return false;
    }

    /**
     * Parses a media response message from a host
     * @param msg the message to parse
     * @param media the media object to load new data into from the response
     * @return true if the parse was successful.
     */
    public static boolean parseMediaResponse(byte[] msg, Media media){

        // Check for proper message headers
        if(msg[0] == MEDIA_REQUEST && msg[1] == MEDIA_RESPONSE ){

            // Extract image ID
            byte imgID = msg[2];

            // Extract packet number
            int packetNumber = ByteBuffer.wrap(msg, 3, 4).getInt();

            if(media.getImgID() == imgID && media.getExpectingPacketNumber() == packetNumber){
                int payloadLength = ByteBuffer.wrap(msg,7,4).getInt();
                Log.d(TAG, "Payload Len = " + payloadLength);
                byte[] payload = new byte[payloadLength];
                System.arraycopy(msg,11,payload,0,payloadLength);
                media.appendData(payload);
            }
            return true;
        }

        return false;
    }

}
