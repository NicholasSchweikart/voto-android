package edu.mtu.cs3421.voto;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Here is where we implement all network tasks for the app.
 */
public class NetworkService {
    public static final String TAG = "Network-Service";
    private static final int PORT = 5555;
    private final String HOST;
    private NetworkServiceListener listener;
    private TCPthread tcpThread;
    public NetworkService(String HOST,NetworkServiceListener listener) {
        this.listener = listener;
        this.HOST = HOST;

        // TCP thread for large data (future ops)
        tcpThread = new TCPthread();
    }

    public interface NetworkServiceListener{

    }

    private class TCPthread extends Thread{
        private Socket socket;
        private byte[] buffer = new byte[4096];

        TCPthread(){

        }
        @Override
        public void run() {
            // Perform our network ops in this loop, anything blocking really...

            try {
                Log.d(TAG, "TCP Connecting to " + HOST + " on port " + PORT);
                socket = new Socket(HOST, PORT);

                Log.d(TAG, "Just connected to " + socket.getRemoteSocketAddress());
                OutputStream outToServer = socket.getOutputStream();
                DataOutputStream out = new DataOutputStream(outToServer);
                out.writeUTF("Hello from " + socket.getLocalSocketAddress());

                InputStream inFromServer = socket.getInputStream();
                BufferedInputStream in = new BufferedInputStream(inFromServer);

                while(true)
                {
                    int numBytes = in.read(buffer);
                    if(numBytes == -1)
                        break;
                    System.out.println("Server says " + buffer);
                }

                socket.close();
            }catch(Exception e) {
                e.printStackTrace();
            }

        }
        public void disconnect(){
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private class UDPthread extends Thread{
        private DatagramSocket datagramSocket;
        private byte[] buffer = new byte[4096];

        UDPthread(){

        }
        @Override
        public void run() {
            // Perform our network ops in this loop, anything blocking really...

            try {
                Log.d(TAG, "UDP Connecting to " + HOST + " on port " + PORT);
                datagramSocket = new DatagramSocket();
                byte[] message = "UDP is da best".getBytes();
                InetAddress address = InetAddress.getByName(HOST);
                DatagramPacket packet = new DatagramPacket(message, message.length, address, PORT);
                DatagramPacket fromServer = new DatagramPacket(buffer, buffer.length);
                datagramSocket.setSoTimeout(3000);
                while(true)
                {
                    datagramSocket.send(packet);
                    try {
                        datagramSocket.receive(fromServer);
                    }catch (SocketTimeoutException e){
                        // Resend the data it never made it
                        datagramSocket.send(packet);
                    }
                    Thread.sleep(1000);
                }
            }catch(Exception e) {
                e.printStackTrace();
            }

        }
        public void disconnect(){
            try {
                datagramSocket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
