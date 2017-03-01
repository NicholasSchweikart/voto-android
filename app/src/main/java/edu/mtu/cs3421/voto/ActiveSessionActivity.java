package edu.mtu.cs3421.voto;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Vibrator;
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

public class ActiveSessionActivity extends AppCompatActivity implements UDPclient.UDPServiceListener {
    private static final String TAG = "Active-Session";
    private UDPclient UDPclient;
    private FABProgressCircle aBtn, bBtn, cBtn, dBtn;
    private View controlsOverlay;
    private ImageView slidesImageView;
    GestureDetectorCompat mDetector;
    Vibrator vibrator;

    // System Control Vars
    private int voteID;
    private FABProgressCircle pendingVoteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
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
        String ipAddressString = getIntent().getStringExtra("IP_ADDRESS_STRING");

        // Init vote number to 0;
        voteID = 0;
        if(ipAddressString.equals("null")){

            // Create the UDPclient that will handle this entire session.
            // The result will come back through the interface.
            UDPclient udp = new UDPclient(ActiveSessionActivity.this,ipAddressString);
            if(!udp.isServiceReady()) {
                Toast.makeText(ActiveSessionActivity.this,"Invalid IP",Toast.LENGTH_SHORT).show();
            }
        }

        // Put the session id stuff up in the title bar
        getSupportActionBar().setTitle("Host@" + ipAddressString );
        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        controlsOverlay = (View)findViewById(R.id.controlsOverlay);

        // Initialize all the voting buttons
        aBtn = (FABProgressCircle)findViewById(R.id.aButton);
        aBtn.setOnClickListener(voteButtonListener);

        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
    }

    @Override
    public void onVoteSuccess(int vote_id) {

        if(vote_id == voteID){
            pendingVoteButton.hide();   // Clear the loader
            Toast.makeText(this,"Vote Sent!",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onVoteFailure(int vote_id) {

        // Check if we care about this failure
        if(vote_id == voteID){
            pendingVoteButton.hide();   // Clear the loader
            Toast.makeText(this,"Vote Failed!",Toast.LENGTH_SHORT).show();
        }
    }

    View.OnClickListener voteButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            // Check if a vote is pending
            if(pendingVoteButton != null){
                pendingVoteButton.hide();               // Clear the loader for the old vote
            }

            pendingVoteButton = (FABProgressCircle) v;  // Re-assign the pending vote button
            pendingVoteButton.show();

            voteID += 1;                                // Increment the current vote_id
            char vote = 'x';
            switch (v.getId()){                         // Insert the correct vote letter
                case R.id.aButton:
                    vote = 'A';
                    break;
                case R.id.bButton:
                    vote = 'B';
                    break;
                case R.id.cButton:
                    vote = 'C';
                    break;
                case R.id.dButton:
                    vote = 'D';
                    break;
            }
            UDPclient.sendVote(vote,voteID);            // Send of the new vote
        }
    };

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

    @Override
    public boolean onTouchEvent(MotionEvent event){
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            Log.d(TAG, "Long Press");
            controlsOverlay.setVisibility(View.VISIBLE);

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d(TAG, "On Fling");
            controlsOverlay.setVisibility(View.INVISIBLE);
            return true;
        }
    };

    @Override
    public void onHandshakeFailure() {
        // Ignore
    }

    @Override
    public void onHandshakeResponse(String reply) {
        // Ignore
    }
}
