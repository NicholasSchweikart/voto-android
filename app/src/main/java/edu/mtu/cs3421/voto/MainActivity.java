package edu.mtu.cs3421.voto;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity implements UDPclient.UDPServiceListener {
    public static final String TAG = "Activity-Main";
    private EditText addressEditText;

    private String ipAddress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        Button joinButton = (Button)findViewById(R.id.joinButton);
        Button hostButton = (Button)findViewById(R.id.hostButton);

        addressEditText = (EditText)findViewById(R.id.ipEditText);

        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ipAddress = addressEditText.getText().toString();
                // The result will come back through the handler.
                new UDPclient(MainActivity.this,ipAddress).sendHandshake();
            }
        });

        hostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ipAddress = "null";
                startSession();
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
    public void onHandshakeResponse() {
        Log.d(TAG, "Handshake Recieved");
        startSession();
    }
}
