package edu.mtu.cs3421.voto;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created for Voto
 * By: nicholas on 2/4/17.
 * Description:
 */

public class SessionHostsAdapter extends ArrayAdapter<SessionHost> {

    public SessionHostsAdapter(Context context, ArrayList<SessionHost> sessionHosts) {
        super(context, 0, sessionHosts);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // Get the data item for this position
        SessionHost host = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.session_host_item, parent, false);
        }

        // Lookup view for data population
        TextView name = (TextView) convertView.findViewById(R.id.hostNameTextView);
        TextView ipaddress = (TextView) convertView.findViewById(R.id.ipAddressTextView);

        // Populate the data into the template view using the data object
        name.setText(host.name);
        ipaddress.setText(host.ipAddress);

        // Return the completed view to render on screen
        return convertView;
    }

}
