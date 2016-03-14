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
import android.view.WindowManager;

import java.io.IOException;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.activities.LockScreenActivity;
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

public class UsbHostActivity extends NearCommunicationActivity {

    public static final String ACTION_HOST_IS_READY = "HOST_IS_READY";
    public static final String LOCK_SCREEN_INSTRUCTION = "Confirm PIN";

    private final Activity mSelf = this;
    private UsbCommunicator mUsbCommunicator;
    private SyncManager mSyncManager;
    private UsbHostHandler mUsbHostHandler;
    private BroadcastReceiver mSyncReceiver;
    private boolean mSyncStarted;
    private boolean mFinishedSuccessfully;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb_host);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mFinishedSuccessfully = false;
        mSyncReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if(action.equals(ACTION_HOST_IS_READY)) {
                    if(mSyncManager != null) {
                        mSyncManager.startCommunication();
                    }
                }
                if(action.equals(SyncManager.SYNC_IS_COMPLETE)) {
                    finishNearCommunicationActivity();
                }
            }
        };
    }

    @Override
    protected void onReturnFromLockScreen() {
        super.onReturnFromLockScreen();
        try {
            IntentFilter intentFilter = new IntentFilter(ACTION_HOST_IS_READY);
            intentFilter.addAction(SyncManager.SYNC_IS_COMPLETE);
            registerReceiver(mSyncReceiver, intentFilter);
            mUsbHostHandler = new UsbHostHandler(this);
            mUsbCommunicator = new UsbCommunicator(this, mUsbHostHandler);
            mSyncManager = new SyncManager(this, mUsbCommunicator);
            mUsbCommunicator.startListening();
            mSyncStarted = true;
        } catch (UsbIsNotSupportedException e) {
            e.printStackTrace();
            quitWithErrorToast(e.getMessage(), new Intent(this, MainActivity.class));
        } catch (BadSyncDataException | PINNotCreatedException | IOException
                | NotAuthenticatedException | PINCreationFailedException e) {
            e.printStackTrace();
            quitWithErrorToast(getString(R.string.sync_activity_failed),
                    new Intent(this, MainActivity.class));
        }
    }



    @Override
    protected void startLockScreenActivity() {
        Intent intent = new Intent(this, LockScreenActivity.class);
        intent.putExtra(Extras.LOCK_SCREEN_INSTRUCTION, LOCK_SCREEN_INSTRUCTION);
        startActivity(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mSyncStarted) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mUsbHostHandler != null && mSyncManager != null) {
            mUsbHostHandler.close();
            unregisterReceiver(mSyncReceiver);
            try {
                if(mFinishedSuccessfully) {
                    mSyncManager.terminateAndClear();
                } else {
                    mSyncManager.terminateAndSave();
                }
                mUsbHostHandler = null;
            } catch (IOException | PINNotCreatedException | PINCreationFailedException
                    | NotAuthenticatedException | SharedPreferencesHelper.CouldNotWriteToSharedPreferencesException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void finishNearCommunicationActivity() {
        Intent intent = new Intent(Events.OTP_SYNCED);
        mReceivingOTP = mSyncManager.getReceivingOTP();
        intent.putExtra(Extras.OTP_ID, mReceivingOTP.getId());
        LocalBroadcastManager.getInstance(mSelf).sendBroadcast(intent);
        mFinishedSuccessfully = true;
        finish();
    }
}
