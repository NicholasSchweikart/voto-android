package edu.mtu.cs3421.voto;

import java.nio.ByteBuffer;

/**
 * Created by nicholasyamahamanschweikart on 3/1/17.
 */

public class MessageUtility {

    public static final byte
            HANDSHAKE_REQUEST   = (byte) 'R',
            VOTE_REQUEST        = (byte) 'V',
            VOTE_RESPONSE        = (byte) 'R',
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

    public static byte[] getMediaPingMessage(){
        byte[] message = new byte[1 + 1];
        message[0] = MEDIA_REQUEST;
        message[1] = MEDIA_PING;
        return message;
    }

    public static byte[] getMediaRequestMessage(byte imgID, byte packetNumber){
        byte[] message = new byte[4];
        message[0] = MEDIA_REQUEST;
        message[1] = MEDIA_RESPONSE;
        message[2] = imgID;
        message[3] = packetNumber;
        return message;
    }

    public static byte parseVoteResponse(byte[] message){
        byte voteID = -1;
        if(message[0] == VOTE_REQUEST && message[1] == VOTE_RESPONSE){
            voteID = message[2];
        }
        return voteID;
    }

    public static boolean parseMediaResponse(byte[] msg, Media media){

        // Check for proper message headers
        if(msg[0] == MEDIA_REQUEST && msg[1] == MEDIA_RESPONSE ){

            // Extract image ID
            byte imgID = msg[2];

            // Extract packet number
            byte packetNumber = msg[3];

            if(media.imgID == imgID && media.expectingPacketNumber == packetNumber){
                int payloadLength = ByteBuffer.wrap(msg,4,4).getInt();

                byte[] payload = new byte[payloadLength];
                System.arraycopy(msg,7,payload,0,payloadLength);
                media.appendData(payload);
            }
            return true;
        }

        return false;
    }

    public class Media{

        byte imgID, totalPackets, expectingPacketNumber;
        byte[] imgBuffer;
        int imgSize, cursor;
        boolean ready = false;

        Media(byte imgID, byte totalPackets, byte imgLength){
            this.imgID = imgID;
            this.totalPackets = totalPackets;
            this.imgSize = imgLength;
            imgBuffer = new byte[imgLength];
            expectingPacketNumber = 1;
            cursor = 0;
        }

        public void appendData(byte[] data){

            // Copy in the data
            System.arraycopy(data,0,imgBuffer,cursor,data.length);

            // Increment the position cursor and the expected packet.
            cursor += data.length;
            expectingPacketNumber += 1;

            // Flag Media ready if we have gotten all the packets.
            if(expectingPacketNumber > totalPackets){
                ready = true;
            }
        }


    }

}
