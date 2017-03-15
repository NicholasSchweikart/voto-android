package edu.mtu.cs3421.voto;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * Created by nicholasyamahamanschweikart on 3/13/17.
 */

public class Media {
    private static final String TAG = "media";
    // Image Stuff
    private byte imgID, totalPackets, expectingPacketNumber;
    private byte[] imgBuffer;
    private int imgSize, cursor;
    private boolean ready = false;
    Bitmap bitmap;

    //TODO and Question fields like correct answer

    Media(byte imgID, byte totalPackets, int imgLength){
        this.imgID = imgID;
        this.totalPackets = totalPackets;
        this.imgSize = imgLength;
        imgBuffer = new byte[imgLength];
        expectingPacketNumber = 1;
        cursor = 0;
    }

    public void appendData(byte[] data){

        Log.d(TAG, "cursor" + cursor);

        // Copy in the data
        System.arraycopy(data,0,imgBuffer,cursor,data.length);

        // Increment the position cursor and the expected packet.
        cursor += data.length - 1;

        expectingPacketNumber += 1;

        // Flag Media ready if we have gotten all the packets.
        if(expectingPacketNumber > totalPackets){
            bitmap = BitmapFactory.decodeByteArray(imgBuffer,0,imgBuffer.length);
            ready = true;
        }
    }

    public boolean isReady(){
        return ready;
    }

    public Bitmap getBitMap(){
        if(ready)
            return bitmap;
        return null;
    }

    public byte getImgID(){return imgID;}
    public byte getExpectingPacketNumber(){return expectingPacketNumber;}
}
