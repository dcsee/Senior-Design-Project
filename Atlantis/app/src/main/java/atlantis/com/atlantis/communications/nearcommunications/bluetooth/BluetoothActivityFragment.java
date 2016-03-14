package atlantis.com.atlantis.communications.nearcommunications.bluetooth;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import atlantis.com.atlantis.R;

/**
 * Created by jvronsky on 2/28/15.
 */
public class BluetoothActivityFragment extends Fragment {

    private static final String TAG = "BLUETOOTH_ACTIVITY";

    private BluetoothInterface parentActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_activity_bluetooth, container, false);
        ListView scannedDevices = (ListView) rootView.findViewById(R.id.scannedDevicesList);
        parentActivity = (BluetoothInterface) getActivity();
        scannedDevices.setAdapter(parentActivity.getAdapter());
        scannedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                parentActivity.connectToDevice(position);
            }
        });

        return rootView;
    }
}
