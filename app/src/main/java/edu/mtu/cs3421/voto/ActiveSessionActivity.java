package edu.mtu.cs3421.voto;

import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.github.jorgecastilloprz.FABProgressCircle;

public class ActiveSessionActivity extends AppCompatActivity {
    private static final String TAG = "Active-Session";
    private UDPService udpService;
    private FABProgressCircle aBtn, bBtn, cBtn, dBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_session);
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String ipAddressString = getIntent().getStringExtra("IP_ADDRESS_STRING");
        if(ipAddressString != "null")
            udpService = new UDPService(handler,ipAddressString);

        // Initialize all the voting buttons
        aBtn = (FABProgressCircle)findViewById(R.id.aButton);
        aBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aBtn.show();
            }
        });
    }

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

    final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what==SystemConstants.MESSAGE_SUCCESS){
                Log.d(TAG, "Message was sent successfully");
                //TODO respond baised on what we where trying to do
            }
            super.handleMessage(msg);
        }
    };
}
