package edu.mtu.cs3421.voto;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

public class MainActivity extends AppCompatActivity implements UDPclient.UDPServiceListener {
    public static final String TAG = "Activity-Main";
    private EditText addressEditText;
    private String ipAddress;
    private  UDPclient udp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        Button joinButton = (Button)findViewById(R.id.joinButton);
        Button hostButton = (Button)findViewById(R.id.hostButton);

        addressEditText = (EditText)findViewById(R.id.ipEditText);

        // Load in the special ID or use the IP address if not enabled.
        final String id;
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String specialID = SP.getString("special_id", "NA");
        if(specialID.equals("NA")){
            id = wifiIpAddress();
        }else{
            id = specialID;
        }

        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ipAddress = addressEditText.getText().toString();

                // The result will come back through the interface.
                udp = new UDPclient(MainActivity.this, ipAddress, id);

            }
        });

        hostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO implement real host button
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume()");

    }

    private void startSession(){
        Intent intent = new Intent(getApplicationContext(),ActiveSessionActivity.class);
        intent.putExtra("IP_ADDRESS_STRING", ipAddress);
        startActivity(intent);
    }


    @Override
    public void onVoteSuccess(int message_id) {

    }

    @Override
    public void onHandshakeResponse(String reply) {

        Log.d(TAG, "Handshake Recieved");
        startSession();

    }

    @Override
    public void onMediaAvailable(Media media) {

    }

    @Override
    public void onReady() {

        udp.sendHandshake();
    }

    @Override
    public void onFailure(int failureCode, Object obj) {
        switch (failureCode){
            case UDPclient.HANDSHAKE_FAILURE:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"Host not found",Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case UDPclient.INIT_FAILURE:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"Invalid IP Address",Toast.LENGTH_SHORT).show();
                    }
                });
                break;
        }
    }

    private String wifiIpAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            Log.e("WIFI-IP", "Unable to get host address.");
            ipAddressString = null;
        }

        return ipAddressString;
    }
}
