package atlantis.com.atlantis.communications.nearcommunications.bluetooth;

import android.widget.ArrayAdapter;

import atlantis.com.atlantis.communications.nearcommunications.Device;

/**
 * Created by jvronsky on 2/28/15.
 */
public interface BluetoothInterface {

    ArrayAdapter<Device> getAdapter();
    void syncOTP();
    void connectToDevice(int position);


}
