package edu.mtu.cs3421.voto.Activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
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
import java.net.UnknownHostException;
import java.nio.ByteOrder;

import edu.mtu.cs3421.voto.VotoComponents.Media;
import edu.mtu.cs3421.voto.R;
import edu.mtu.cs3421.voto.VotoComponents.UDPclient;

/**
 * Runs the laucher activity for the App. Allows the user to join a host and start a Voto session.
 */
public class MainActivity extends AppCompatActivity implements UDPclient.UDPServiceListener {
    public static final String TAG = "Activity-Main";
    private EditText addressEditText;
    private String ipAddress;
    private  UDPclient udp;
    private String id;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        // Access the UI components
        Button joinButton = (Button)findViewById(R.id.joinButton);
        addressEditText = (EditText)findViewById(R.id.ipEditText);

        // Set the On Click event for join to handshake with host.
        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ipAddress = addressEditText.getText().toString();

                // The result will come back through the interface.
                udp = new UDPclient(MainActivity.this, ipAddress, id);

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

        // Load in the special ID or use the IP address of this device if not available.
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String specialID = SP.getString("special_id", "NA");
        if(specialID.equals("JohnSmith12")){
            id = wifiIpAddress();
        }else{
            id = specialID;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume()");

    }

    /**
     * Start a new Voto Session with the host found by the handshake.
     * @param sessionName the name of the host that we will be connecting with
     */
    private void startSession(String sessionName){

        // Add relevant values into the Intent so the session activty knows whats up
        Intent intent = new Intent(getApplicationContext(),ActiveSessionActivity.class);
        intent.putExtra("IP_ADDRESS_STRING", ipAddress);
        if(sessionName != null)
            intent.putExtra("HOST_SESSION_NAME", sessionName);

        // Start the session activity
        startActivity(intent);
    }


    @Override
    public void onVoteSuccess(int message_id) {
        //IGNORE
    }

    @Override
    public void onHandshakeResponse(String reply) {

        Log.d(TAG, "Handshake Recieved");
        startSession(reply);
    }

    @Override
    public void onMediaAvailable(Media media) {
        //IGNORE
    }

    @Override
    public void onReady() {
        // Send a handshake message to the HOST IP.
        udp.sendHandshake();
    }

    @Override
    public void onFailure(int failureCode, Object obj) {
        // Process any failure codes thrown from the UDP client
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

    /**
     * Gets the IP address of this device via the WiFi manager.
     * @return the ip address as a string.
     */
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
