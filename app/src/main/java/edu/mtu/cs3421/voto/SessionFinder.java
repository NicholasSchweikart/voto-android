package edu.mtu.cs3421.voto;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.DatagramSocket;

/**
 * Created for Voto
 * By: nicholas on 2/4/17.
 * Description:
 */

public class SessionFinder extends Thread {

    public static final String TAG = "Session-Finder";

    DatagramSocket socket;
    private InetAddress GROUP = null;
    private final int PORT;
    private boolean scan = true;

    private Handler handler;

    public SessionFinder(int p, Handler handler) {
        PORT = p;
        try {
            GROUP = InetAddress.getByName("224.0.1.35");
        } catch (Exception e) {
            Log.d(TAG, "Failed to get mutlicast IP");
        }
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);

            byte[] send = "VOTO_HANDSHAKE_REQUEST".getBytes();

            try {
                DatagramPacket dp = new DatagramPacket(send, send.length, GROUP, PORT);
                socket.send(dp);
            } catch (Exception e) {
                e.printStackTrace();
            }

            byte[] buffer = new byte[1024];
            DatagramPacket rp = new DatagramPacket(buffer, buffer.length);

            while(scan)
            {
                socket.receive(rp);
                String inFromServer = new String(rp.getData()).trim();

                Log.d(TAG, "Got a reply from: " + rp.getAddress().getHostAddress() + " Received: " + inFromServer);
                if (inFromServer.startsWith("VOTO_HANDSHAKE_RESPONSE_")) {
                    inFromServer = inFromServer.substring(24);
                    Log.d(TAG, "Posting to handler");
                    Message msg = handler.obtainMessage();
                    msg.what = SessionListActivity.ON_HOST;
                    msg.obj = new SessionHost(inFromServer,rp.getAddress().getHostAddress());
                    handler.sendMessage(msg);
                }
            }

            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
