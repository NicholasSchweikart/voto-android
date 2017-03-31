package edu.mtu.cs3421.voto;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
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

import com.github.jorgecastilloprz.FABProgressCircle;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;

public class ActiveSessionActivity extends AppCompatActivity implements UDPclient.UDPServiceListener {
    private static final String TAG = "Active-Session";

    private FABProgressCircle aBtn, bBtn, cBtn, dBtn;
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

        // Init vote number to 0;
        voteID = 0;

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
        getSupportActionBar().setTitle("Host@" + hostIpAddressString );
        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        controlsOverlay = (View)findViewById(R.id.controlsOverlay);

        // Initialize all the voting buttons
        aBtn = (FABProgressCircle)findViewById(R.id.aButton);
        aBtn.setOnClickListener(voteButtonListener);

        bBtn = (FABProgressCircle)findViewById(R.id.bButton);
        bBtn.setOnClickListener(voteButtonListener);

        cBtn = (FABProgressCircle)findViewById(R.id.cButton);
        cBtn.setOnClickListener(voteButtonListener);

        dBtn = (FABProgressCircle)findViewById(R.id.dButton);
        dBtn.setOnClickListener(voteButtonListener);

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

                    Toast.makeText(getApplicationContext(),"Vote Sent!",Toast.LENGTH_SHORT).show();
                }
            });
        }else{
            Log.d(TAG, "We dont care about this vote anymore");
        }
    }

    @Override
    public void onMediaAvailable(Media mediaNew) {
        media = mediaNew;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //update the background slide
                slidesImageView.setImageBitmap(media.getBitMap());

                VOTING_LOCKED = false;
            }
        });
    }

    @Override
    public void onReady() {

        // Start Media polling, this never stops until the session is over...
        udpClient.pollNewMedia();
    }

    @Override
    public void onFailure(int failureCode, Object obj) {

        switch (failureCode){
            case UDPclient.MEDIA_FAILURE:

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
            }
            udpClient.sendVote(vote,voteID);            // Send of the new vote
        }
    };

    @Override
    public void onHandshakeResponse(String reply) {
        // Ignore
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

    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            Log.d(TAG, "Long Press");

            if(!VOTING_LOCKED){
                controlsOverlay.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(TAG, "On Fling");
            controlsOverlay.setVisibility(View.INVISIBLE);
            return true;
        }
    }

    //------------- Menu Click Handling Area --------------------
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
