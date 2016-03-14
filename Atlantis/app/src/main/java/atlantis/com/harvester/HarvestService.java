package atlantis.com.harvester;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by jvronsky on 5/21/15.
 * Harvest service class to monitor to interact with the harvester.
 */
public class HarvestService extends Service {

    private final IBinder mBinder = new HarvesterBinder();
    private static boolean isServiceRunning = false;
    private HarvestTask mHarvestTask;


    /**
     * Run when service is started.
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!mHarvestTask.isHarvesting()) {
            try {
                start();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        isServiceRunning = true;
        return START_STICKY;
    }

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            mHarvestTask = new HarvestTask(this, this.getBaseContext());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mHarvestTask.kill();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * Check is app is harvesting.
     * @return whether or not service is harvesting.
     */
    public boolean isHarvesting() {
        return mHarvestTask.isHarvesting();
    }

    /**
     * Binder to interact with service.
     */
    public class HarvesterBinder extends Binder {
        public HarvestService getService() {
            return HarvestService.this;
        }
    }

    public void start() throws FileNotFoundException {
        // If harvesting is running don't re-run.
        if(mHarvestTask.getStatus() != HarvestTask.STATUS_RUNNING) {
            // start a new instance to run again.
            mHarvestTask = new HarvestTask(this, this.getBaseContext());
            mHarvestTask.execute();
        }
    }

    public int getAvailableOTP(int length, byte[] buffer) throws IOException {
        if(mHarvestTask != null) {
            return mHarvestTask.getOTPFromEntropy(length, buffer);
        }
        return -1;
    }

    public long getAmountOTP() {
        return mHarvestTask.getAmountOTP();
    }

    public void cancelService() {
        if(mHarvestTask != null) {
            mHarvestTask.cancel(true);
        }
    }

    public static void startService(Context context) {
        if(!isServiceRunning()) {
            context.getApplicationContext().startService(new Intent(context, HarvestService.class));
        }
    }

    public static boolean isServiceRunning() {
        return isServiceRunning;
    }

}
