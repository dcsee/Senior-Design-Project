package atlantis.com.atlantis.communications.nearcommunications.usb;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.activities.MainActivity;
import atlantis.com.atlantis.activities.NearCommunicationActivity;
import atlantis.com.atlantis.communications.nearcommunications.BadSyncDataException;
import atlantis.com.atlantis.communications.nearcommunications.SyncManager;
import atlantis.com.atlantis.encryption.PINCreationFailedException;
import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;
import atlantis.com.atlantis.utils.Events;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.atlantis.utils.SharedPreferencesHelper;

public class UsbActivity extends NearCommunicationActivity {

    private static final String TAG = "UsbActivity";

    private final Activity mSelfActivity = this;
    private BroadcastReceiver mSyncCompleteReceiver;
    private UsbAccessoryHandler mUsbAccessoryHandler;
    private UsbCommunicator mUsbCommunicator;
    private SyncManager mSyncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb);
        mSyncCompleteReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(action.equals(SyncManager.SYNC_IS_COMPLETE)) {
                    finishNearCommunicationActivity();
                }
            }
        };
        try {
            extractExtras();
            checkUsbIsSupported();
            mUsbAccessoryHandler = new UsbAccessoryHandler(this);
            mUsbCommunicator = new UsbCommunicator(this, mUsbAccessoryHandler);
            mSyncManager = new SyncManager(this, mUsbCommunicator);
            mUsbCommunicator.startListening();
            IntentFilter intentFilter = new IntentFilter(SyncManager.SYNC_IS_COMPLETE);
            registerReceiver(mSyncCompleteReceiver, intentFilter);
        } catch (UsbIsNotSupportedException e) {
            e.printStackTrace();
            quitWithErrorToast(e.getMessage(), new Intent(this, MainActivity.class));
        } catch (PINCreationFailedException | SharedPreferencesHelper.CouldNotWriteToSharedPreferencesException
                | BadSyncDataException | PINNotCreatedException | IOException
                | NotAuthenticatedException e) {
            e.printStackTrace();
            quitWithErrorToast(getString(R.string.sync_activity_failed),
                    new Intent(this, MainActivity.class));
        }
    }

    private void checkUsbIsSupported() throws UsbIsNotSupportedException {
        if(getSystemService(Context.USB_SERVICE) == null) {
            throw new UsbIsNotSupportedException("Usb is not supported");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mSyncManager != null) {
            try {
                mSyncManager.terminateAndClear();
                unregisterReceiver(mSyncCompleteReceiver);
                mUsbAccessoryHandler.close();
            } catch (SharedPreferencesHelper.CouldNotWriteToSharedPreferencesException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mUsbAccessoryHandler.close();
    }

    @Override
    protected void finishNearCommunicationActivity() {
        Intent intent = new Intent(Events.OTP_SYNCED);
        intent.putExtra(Extras.OTP_ID, mReceivingOTP.getId());
        LocalBroadcastManager.getInstance(mSelfActivity).sendBroadcast(intent);
        finish();
    }
}
