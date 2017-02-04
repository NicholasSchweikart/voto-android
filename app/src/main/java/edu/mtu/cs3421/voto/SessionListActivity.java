package edu.mtu.cs3421.voto;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

public class SessionListActivity extends Activity implements AdapterView.OnItemClickListener, SessionFinder.SessionFinderListener {

    private ArrayList<SessionHost> arrayOfHosts;
    SessionHostsAdapter sessionHostsAdapter;
    ListView listView;

    SessionFinder sf = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_sessionlist);

        listView = (ListView) findViewById(R.id.list);
        arrayOfHosts = new ArrayList<SessionHost>();
        sessionHostsAdapter = new SessionHostsAdapter(getApplicationContext(), arrayOfHosts);
        listView.setAdapter(sessionHostsAdapter);

        //Start session finder with this as listener
        sf = new SessionFinder(9876, this);
        sf.start();
    }

    @Override
    public void onHandshakeResponse(String id, String hostAddress) {
        addItem(id, hostAddress);
    }

    private void addItem(String name, String ip){
        // Add item to adapter
        SessionHost newHost = new SessionHost(name,ip);
        sessionHostsAdapter.add(newHost);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        SessionHost host = (SessionHost) listView.getSelectedItem();
        Intent data = new Intent();
        data.putExtra("HOST_NAME", host.name);
        data.putExtra("IP_ADDRESS", host.ipAddress);
        setResult(1,data);
        finish();
    }

}
