package edu.mtu.cs3421.voto;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Here is where we implement all network tasks for the app.
 */
public class TCPService {
    public static final String TAG = "TCP-Service";
    private final TCPserviceListener listener;
    private int PORT = 9872;
    private String HOST;
    private Socket tcpSocket;

    DataInputStream inFromServer;
    DataOutputStream outToServer;

    public TCPService(TCPserviceListener listener) {
        Log.d(TAG, "Building TCP service");
        this.listener = listener;
    }

    public interface TCPserviceListener{
        void onVoteSent();
    }

    public void connect(String host, int port){
        this.HOST = host;
        this.PORT = port;
        Thread thread = new Thread(connect);
        thread.start();
    }
    public void sendVote(char voteLetter) {
        byte[] message = new byte[2];
        message[0] = ((byte) 'V');
        message[1] = ((byte)voteLetter);
        new send(message).start();
    }


    class send extends Thread{
        private  byte[] message;
        send(byte[] message){
            this.message = message;
        }
        @Override
        public void run(){
            Log.d(TAG, "Sending Vote");
            try {
                outToServer.write(message);
                outToServer.write('\n');
                outToServer.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
    private Runnable connect = new Runnable() {
        @Override
        public void run() {
            try {
                Log.d(TAG, "Attempting Connection");
                tcpSocket = new Socket(HOST,PORT);
                outToServer = new DataOutputStream(tcpSocket.getOutputStream());
                inFromServer = new DataInputStream(tcpSocket.getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };
}
