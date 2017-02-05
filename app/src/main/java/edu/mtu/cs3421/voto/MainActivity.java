package edu.mtu.cs3421.voto;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity  {
    public static final String TAG = "Activity-Main";

    private TCPService tcpService;
    TextView hostIpTxt, hostNameTxt;
    private String name, ip;
    boolean update = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

         hostIpTxt = (TextView)findViewById(R.id.hostIpTextView);
         hostNameTxt = (TextView) findViewById(R.id.hostNameTextView);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent reviewIntent = new Intent(MainActivity.this, SessionListActivity.class);
                startActivityForResult(reviewIntent,1);
            }
        });

        Button aButton = (Button)findViewById(R.id.a_button);
        aButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO Send Vote Here over UDP
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
    public void onVoteSent() {
        Toast.makeText(this,"Vote Sent!", Toast.LENGTH_SHORT).show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "Got result from scanning network");
        if (requestCode == 1 && data != null) {

            String ipAddress = data.getStringExtra("IP_ADDRESS");
            String name = data.getStringExtra("HOST_NAME");
            Log.d(TAG, "Name:" + name + "IP: " + ipAddress);
            update = true;
            hostIpTxt.setText(ipAddress);
            hostNameTxt.setText(name);

            //TODO create UDPservice around the new IP
        }
    }
}
