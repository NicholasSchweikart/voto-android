package edu.mtu.cs3421.voto.Activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

import edu.mtu.cs3421.voto.VotoComponents.Media;
import edu.mtu.cs3421.voto.R;
import edu.mtu.cs3421.voto.VotoComponents.UDPclient;

/**
 * This runs the live voting session and communicates the the Voto Host.
 */
public class ActiveSessionActivity extends AppCompatActivity implements UDPclient.UDPServiceListener {
    private static final String TAG = "Active-Session";

    private FloatingActionButton aBtn, bBtn, cBtn, dBtn, eBtn;
    private View controlsOverlay;
    private ImageView slidesImageView;
    private Media media;
    private UDPclient udpClient;
    GestureDetectorCompat mDetector;
    Vibrator vibrator;

    // System Control Vars
    private byte voteID;
    private boolean VOTING_LOCKED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Starting Live Activity");

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_active_session);
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Get the slide image view
        slidesImageView = (ImageView)findViewById(R.id.slideImageView);

        // Retrieve the HOST ip address from the intent.
        String hostIpAddressString = getIntent().getStringExtra("IP_ADDRESS_STRING");
        String hostSessionName = getIntent().getStringExtra("HOST_SESSION_NAME");

        // Init vote number to 0;
        voteID = 0;

        // Load in the unique identifier from preferences if it exists; otherwise we will use the
        // device IP address
        String id;
        SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String specialID = SP.getString("special_id", "NA");
        if(specialID.equals("NA")){
            id = wifiIpAddress();
        }else{
            id = specialID;
        }

        // Create the UDPclient that will handle this entire session.
        // The result will come back through the interface.
        udpClient = new UDPclient(ActiveSessionActivity.this, hostIpAddressString, id);

        // Lock down the voting interface until we have the slide loaded.
        VOTING_LOCKED = true;

        // Put the session id stuff up in the title bar
        getSupportActionBar().setTitle("Connected to:" + hostSessionName );
        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        controlsOverlay = (View)findViewById(R.id.controlsOverlay);

        // Initialize all the voting buttons
        aBtn = (FloatingActionButton) findViewById(R.id.aButton);
        aBtn.setOnClickListener(voteButtonListener);

        bBtn = (FloatingActionButton)findViewById(R.id.bButton);
        bBtn.setOnClickListener(voteButtonListener);

        cBtn = (FloatingActionButton)findViewById(R.id.cButton);
        cBtn.setOnClickListener(voteButtonListener);

        dBtn = (FloatingActionButton)findViewById(R.id.dButton);
        dBtn.setOnClickListener(voteButtonListener);

        eBtn = (FloatingActionButton)findViewById(R.id.eButton);
        eBtn.setOnClickListener(voteButtonListener);

        // Assign our custom Gesture Detector to control the voting overlay.
        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        if(udpClient != null){
            udpClient.stop();
        }
    }

    @Override
    public void onVoteSuccess(int vote_id) {
        Log.d(TAG, "Vote Successful!");
        if(vote_id == voteID){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Tell the user their vote was sent and processed.
                    Toast.makeText(getApplicationContext(),"Vote Sent!",Toast.LENGTH_SHORT).show();
                }
            });
        }else{
            Log.d(TAG, "We dont care about this vote anymore");
        }
    }

    @Override
    public void onMediaAvailable(Media mediaNew) {
        Log.d(TAG,"Showing new media");
        media = mediaNew;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                //update the background slide
                slidesImageView.setImageBitmap(media.getBitMap());

                // Unlock the voting interface
                VOTING_LOCKED = false;
            }
        });
    }

    @Override
    public void onReady() {

        // Start Media polling, this will terminate the session if no response is had after the
        // set timout delay.
        udpClient.pollNewMedia();
    }

    @Override
    public void onFailure(int failureCode, Object obj) {

        // Process any failure codes thrown by the UDP client
        switch (failureCode){
            case UDPclient.MEDIA_FAILURE:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(getApplicationContext(),"Session Over",Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
                finish();
                break;
            case UDPclient.VOTE_FAILURE:
                Log.e(TAG, "Vote Failure");
                // Check if we care about this failure
                if((byte)obj == voteID){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            Toast.makeText(getApplicationContext(),"Vote Failed!",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                break;
            default:
                break;
        }
    }

    // Listener that all vote buttons will share
    View.OnClickListener voteButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            voteID += 1;                                // Increment the current vote_id
            String vote = null;
            switch (v.getId()){                         // Insert the correct vote letter
                case R.id.aButton:
                    vote = "A";
                    break;
                case R.id.bButton:
                    vote = "B";
                    break;
                case R.id.cButton:
                    vote = "C";
                    break;
                case R.id.dButton:
                    vote = "D";
                    break;
                case R.id.eButton:
                    vote = "E";
                    break;
            }
            udpClient.sendVote(vote,voteID);            // Send of the new vote
        }
    };

    @Override
    public void onHandshakeResponse(String reply) {
        // IGNORE WE WONT GET HANDSHAKES EVER
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

    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            super.onSingleTapConfirmed(e);
            Log.d(TAG, "Tap event");

            // On tap we will toggle the visibilty of the UI controls overlay. This includes the
            // voting and navigation buttons.
            if(!VOTING_LOCKED){
                if(controlsOverlay.getVisibility() == View.VISIBLE)
                    controlsOverlay.setVisibility(View.INVISIBLE);
                else
                    controlsOverlay.setVisibility(View.VISIBLE);
            }
            return true;
        }
    }

    //---------------------- Menu Click Handling Area ----------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_active_session, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            default:
                return false;
        }
    }
}
