package edu.mtu.cs3421.voto;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

public class SessionListActivity extends Activity{
    public static final String TAG = "SessionList-Activity";
    public static final int ON_HOST = 0;
    private ArrayList<SessionHost> arrayOfHosts;
    SessionHostsAdapter sessionHostsAdapter;
    ListView listView;

    SessionFinder sf = null;

    final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what==ON_HOST){
                SessionHost in = (SessionHost) msg.obj;
                addItem(in.name, in.ipAddress);
            }
            super.handleMessage(msg);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_sessionlist);

        listView = (ListView) findViewById(R.id.list);
        arrayOfHosts = new ArrayList<SessionHost>();
        sessionHostsAdapter = new SessionHostsAdapter(getApplicationContext(), arrayOfHosts);
        listView.setAdapter(sessionHostsAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.d(TAG, "onItemClick()");
                SessionHost host = (SessionHost) listView.getItemAtPosition(i);
                Intent data = new Intent();
                data.putExtra("HOST_NAME", host.name);
                data.putExtra("IP_ADDRESS", host.ipAddress);
                setResult(1,data);
                finish();
            }
        });
        //Start session finder with this as listener
        sf = new SessionFinder(9876, handler);
        sf.start();
    }

    private void addItem(String name, String ip){
        // Add item to adapter
        SessionHost newHost = new SessionHost(name,ip);
        sessionHostsAdapter.add(newHost);
    }

}
