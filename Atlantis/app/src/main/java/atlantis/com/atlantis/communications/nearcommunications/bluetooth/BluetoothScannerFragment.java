package atlantis.com.atlantis.communications.nearcommunications.bluetooth;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import atlantis.com.atlantis.R;

/**
 * Created by jvronsky on 2/28/15.
 */
public class BluetoothScannerFragment extends Fragment {
   public BluetoothScannerFragment() {

   }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scanner_bluetooth, container, false);
    }
}
