package edu.mtu.cs3421.voto;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Created for Voto
 * By: nicholas on 2/4/17.
 * Description:
 */

public class SessionFinder extends Thread{

    public static final String TAG = "Session-Finder";

    DatagramSocket socket;
    private InetAddress GROUP = null;
    private final int PORT;
    private boolean scan = true;

    public SessionFinder(int p) {
        PORT = p;
        try {
            GROUP = InetAddress.getByName("224.0.0.3");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket();

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
                Log.d(TAG, "Got a reply from: " + rp.getAddress().getHostAddress() + " Received: " + new String(rp.getData()).trim());
            }

            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
