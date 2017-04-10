package edu.mtu.cs3421.voto.VotoComponents;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * Created by Nicholas Schweikart on 3/13/17.
 * A simple container to wrap around the incoming media from a Voto Host.
 */
public class Media {
    private static final String TAG = "media";
    // Image Stuff
    private byte imgID;
    private byte[] imgBuffer;
    private int imgSize, cursor, totalPackets, expectingPacketNumber;
    private boolean ready = false;
    Bitmap bitmap;

    /**
     * Creats a new Media Object to contain incoming data stream.
     * @param imgID the ID for this particular image
     * @param totalPackets the number of packets this image will take to load
     * @param imgLength the length of the image in bytes
     */
    Media(byte imgID, int totalPackets, int imgLength){
        this.imgID = imgID;
        this.totalPackets = totalPackets;
        this.imgSize = imgLength;
        imgBuffer = new byte[imgLength];
        expectingPacketNumber = 1;
        cursor = 0;
    }

    /**
     * Appends new bytes to the total byte buffer for this image.
     * @param data the bytes to append
     */
    public void appendData(byte[] data){

        Log.d(TAG, "cursor" + cursor);

        // Copy in the data
        System.arraycopy(data,0,imgBuffer,cursor,data.length);

        // Increment the position cursor and the expected packet.
        cursor += data.length;

        // Increment to the next expected packet number.
        expectingPacketNumber += 1;

        // Flag Media ready if we have gotten all the packets.
        if(expectingPacketNumber > totalPackets){
            bitmap = BitmapFactory.decodeByteArray(imgBuffer,0,imgBuffer.length);
            ready = true;
        }
    }

    /**
     * Returns the status of this image file.
     * @return true if the image is fully loaded
     */
    public boolean isReady(){
        return ready;
    }

    /**
     * Gets the bitmap assoiated with this image
     * @return the bitmap of the media image
     */
    public Bitmap getBitMap(){
        if(ready)
            return bitmap;
        return null;
    }

    /**
     * Returns the image ID for this media object
     * @return the ID of the image
     */
    public byte getImgID(){return imgID;}

    /**
     * Returns the next packet number we are expecting
     * @return the next packet number
     */
    public int getExpectingPacketNumber(){return expectingPacketNumber;}
}
