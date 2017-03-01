package edu.mtu.cs3421.voto;

import java.nio.ByteBuffer;

/**
 * Created by nicholasyamahamanschweikart on 3/1/17.
 */

public class MessageUtility {

    public static final byte
        HANDSHAKE_REQ_COM   = (byte) 'R',
        VOTE_COM            = (byte) 'V',
        MEDIA_REQUEST_COM   = (byte) 'M',
        MEDIA_PING          = (byte) 'P',
        MEDIA_REQ_RESPONSE  = (byte) 'R';


    /**
     * Builds a handshake request message.
     * @param id your unique ID if you have one, NULL if not
     * @return the byte[] message
     */
    public byte[] getHandshakeRequestMessage(String id){
        byte[] message;
        int i = 0;
        if(id != null){
            byte len = (byte) id.length();
            byte[] idBuff = id.getBytes();
            message = new byte[ 1 + 1 + len];
            message[i++] = HANDSHAKE_REQ_COM;
            message[i++] = len;
            System.arraycopy(idBuff,0,message,i,len);
        }else{
            message = new byte[ 1 + 1];
            message[i++] = HANDSHAKE_REQ_COM;
            message[i++] = 0;
        }
        return message;
    }

    public byte[] getVoteMessage(String id, String vote, byte voteID){

        byte[] message;
        int i = 0;
        byte voteLength = ((byte) vote.length());
        byte[] voteData = vote.getBytes();

        if(id != null){
            byte idLength = (byte) id.length();
            byte[] idBuff = id.getBytes();

            // [ V | ID_LEN | ID_DATA | VOTE_ID | VOTE_LEN | VOTE_DATA]
            message = new byte[ 1 + 1 + idLength + 1 + 1 + voteLength];
            message[i++] = VOTE_COM;
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
            message[i++] = VOTE_COM;
            message[i++] = 0;           // No ID field
            message[i++] = voteID;
            message[i++] = voteLength;

            System.arraycopy(voteData,0,message,i,voteLength);
        }

        return message;
    }

    public byte[] getMediaPingMessage(){
        byte[] message = new byte[1 + 1];
        message[0] = MEDIA_REQUEST_COM;
        message[1] = MEDIA_PING;
        return message;
    }

    public byte[] getMediaRequestMessage(byte imgID, byte packetNumber){
        byte[] message = new byte[1 + 1 + 1 + 1];
        message[0] = MEDIA_REQUEST_COM;
        message[1] = MEDIA_REQ_RESPONSE;
        message[2] = imgID;
        message[3] = packetNumber;
        return message;
    }

    public int parseVoteResponse(byte[] message){
        int voteID;

        return 0;
    }

    public MediaChunk parseMediaResponse(byte[] msg){

        // Check for proper message headers
        if(msg[0] == MEDIA_REQ_RESPONSE && msg[1] == MEDIA_REQ_RESPONSE ){

            // Extract image ID
            byte imgID = msg[2];

            // Extract packet number
            byte packetNumber = msg[3];

            int payloadLength = ByteBuffer.wrap(msg,4,4).getInt();

            byte[] payload = new byte[payloadLength];
            System.arraycopy(msg,7,payload,0,payloadLength);
            return new MediaChunk(imgID,packetNumber,payload);
        }

        return null;
    }

    public class MediaChunk{
        byte imgID, packetNumber;
        byte[] data;
        MediaChunk(byte imgID, byte packetNumber, byte[] data){
            this.data = data;
            this.imgID = imgID;
            this.packetNumber = packetNumber;
        }
    }
}
