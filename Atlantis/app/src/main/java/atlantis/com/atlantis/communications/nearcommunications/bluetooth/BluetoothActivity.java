package atlantis.com.atlantis.communications.nearcommunications.bluetooth;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.IOException;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.activities.NearCommunicationActivity;
import atlantis.com.atlantis.communications.nearcommunications.BadSyncDataException;
import atlantis.com.atlantis.communications.nearcommunications.Device;
import atlantis.com.atlantis.communications.nearcommunications.SyncManager;
import atlantis.com.atlantis.encryption.PINCreationFailedException;
import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;
import atlantis.com.atlantis.utils.DrawableUtils;
import atlantis.com.atlantis.utils.Events;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.atlantis.utils.SharedPreferencesHelper;

/**
 * Bluetooth activity used to transfer OTP over bluetooth
 * TODO:(Finish UI and handle messages logic for all errors)
 */
public class BluetoothActivity extends NearCommunicationActivity implements BluetoothInterface {

    private static final String TAG = "BLUETOOTH_ACTIVITY";

    private ArrayAdapter<Device> mArrayAdapter;

    private BluetoothCommunicator mBluetoothCommunicator;
    private SyncManager mSyncManager;

    // True, when bluetooth is on.
    private boolean mBluetoothOn;
    // Host device, one pressed to start
    private boolean mIsHost;

    private final BluetoothActivity mSelfActivity = this;
    private BroadcastReceiver mConnectedReceiver;
    private DeviceScanner mDeviceScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            extractExtras();
            mIsHost = false;
            mArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1);
            mBluetoothCommunicator = new BluetoothCommunicator(this, mArrayAdapter);
            mSyncManager = new SyncManager(this, mBluetoothCommunicator);
        } catch (PINCreationFailedException | PINNotCreatedException | NotAuthenticatedException | IOException | BadSyncDataException | SharedPreferencesHelper.CouldNotWriteToSharedPreferencesException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.sync_activity_failed, Toast.LENGTH_SHORT).show();
            finish();
            Log.d(TAG, e.getMessage());
        }
        setContentView(R.layout.activity_bluetooth);
        mConnectedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(action.equals(BluetoothCommunicator.CONNECTED_TO_DEVICE) && mIsHost) {
                    mSyncManager.startCommunication();
                }
                if(action.equals(SyncManager.SYNC_IS_COMPLETE)) {
                    finishNearCommunicationActivity();
                }
            }
        };
        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().add(R.id.container,
                    new BluetoothScannerFragment()).commitAllowingStateLoss();
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothCommunicator.CONNECTED_TO_DEVICE);
        filter.addAction(SyncManager.SYNC_IS_COMPLETE);
        registerReceiver(mConnectedReceiver, filter);
        mDeviceScanner = new DeviceScanner();
        mDeviceScanner.execute();
        mBluetoothOn = mBluetoothCommunicator.isWasConnectedToBluetoothBefore();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBluetoothCommunicator.destroy();
        unregisterReceiver(mConnectedReceiver);
        try {
            if(mSyncManager != null) {
                mSyncManager.terminateAndClear();
            }
        } catch (SharedPreferencesHelper.CouldNotWriteToSharedPreferencesException e) {
            e.printStackTrace();
        }
    }

    /**
     * When bluetooth is on, start the rest of the process.
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == BluetoothCommunicator.REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_OK) {
                mBluetoothCommunicator.startHost();
                mBluetoothOn = true;
                mBluetoothCommunicator.makeDiscoverable();
            } else {
                Toast.makeText(this, R.string.error_bluetooth_ack, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public ArrayAdapter<Device> getAdapter() {
        return mArrayAdapter;
    }

    @Override
    public void syncOTP() {
    }

    @Override
    public void connectToDevice(int position) {
        mIsHost = true;
        mBluetoothCommunicator.connect(mArrayAdapter.getItem(position));
    }

    @Override
    protected void finishNearCommunicationActivity() {
        Intent intent = new Intent(Events.OTP_SYNCED);
        intent.putExtra(Extras.OTP_ID, mReceivingOTP.getId());
        LocalBroadcastManager.getInstance(mSelfActivity).sendBroadcast(intent);

        finish();
    }

    /**
     * Device scanner is working till the first device is found. Once the device is found the
     * display will show it.
     */
    private class DeviceScanner extends AsyncTask<Void, Integer, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mSelfActivity.getSupportFragmentManager().beginTransaction().replace(R.id.container,
                    new BluetoothScannerFragment()).commitAllowingStateLoss();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            mSelfActivity.getSupportFragmentManager().beginTransaction().replace(R.id.container,
                    new BluetoothActivityFragment()).commitAllowingStateLoss();
        }

        @Override
        protected Void doInBackground(Void... params) {
            while(!(mBluetoothOn || mBluetoothCommunicator.isWasConnectedToBluetoothBefore()));
            mBluetoothCommunicator.scan();
            while(mArrayAdapter.getCount() == 0);
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bluetooth, menu);

        Drawable icons[] = {
                menu.findItem(R.id.action_refresh).getIcon()
        };
        DrawableUtils.tintDrawablesIfNotLollipop(icons, getResources().getColor(R.color.action_bar_icon_color));
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
}
