package edu.mtu.cs3421.voto;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v4.app.NavUtils;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Layout;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.github.jorgecastilloprz.FABProgressCircle;

public class ActiveSessionActivity extends AppCompatActivity implements UDPclient.UDPServiceListener {
    private static final String TAG = "Active-Session";
    private UDPclient UDPclient;
    private FABProgressCircle aBtn, bBtn, cBtn, dBtn;
    private View controlsOverlay;
    GestureDetectorCompat mDetector;
    Vibrator vibrator;

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

        // Retrieve the HOST ip address from the intent.
        String ipAddressString = getIntent().getStringExtra("IP_ADDRESS_STRING");
        if(ipAddressString.equals("null")){

            // Create the UDPclient that will handle this entire session.
            UDPclient = new UDPclient(this,ipAddressString);
        }

        // Put the session id stuff up in the title bar
        getSupportActionBar().setTitle("Host@" + ipAddressString );
        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        controlsOverlay = (View)findViewById(R.id.controlsOverlay);

        // Initialize all the voting buttons
        aBtn = (FABProgressCircle)findViewById(R.id.aButton);
        aBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aBtn.show();
            }
        });

        bBtn = (FABProgressCircle)findViewById(R.id.bButton);
        bBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
    }

    @Override
    public void onVoteSuccess(int message_id) {

    }

    @Override
    public void onHandshakeResponse(String reply) {

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
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                               float velocityY) {
            // TODO Auto-generated method stub
            Log.d(TAG, "On Fling");
            controlsOverlay.setVisibility(View.INVISIBLE);
            return true;
        }
    };
}
