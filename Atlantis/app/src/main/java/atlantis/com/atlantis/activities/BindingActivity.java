package atlantis.com.atlantis.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import atlantis.com.atlantis.utils.Events;
import atlantis.com.harvester.HarvestService;

/**
 * Created by jvronsky on 5/24/15.
 * Binding activity binds to the harvesting service.
 */
public class BindingActivity extends BaseActivity {

    protected HarvestService mHarvestService;
    private boolean mBound;
    // Service connection used to set up variables when activity attach to service.
    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            HarvestService.HarvesterBinder binder = (HarvestService.HarvesterBinder) service;
            mHarvestService = binder.getService();
            mBound = true;
            onBindComplete();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // If not bound, bind activity to Harvest service
        if(!getBound()) {
            Intent bind = new Intent(this, HarvestService.class);
            mBound = getApplication().bindService(bind, mServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unbind when activity leave.
        if(getBound()) {
            getApplicationContext().unbindService(mServiceConnection);
            mBound = false;
        }
    }

    protected void onBindComplete() {
        Intent intent = new Intent(Events.SERVICE_BOUND);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    protected boolean getBound() {
        return mBound;
    }
}
