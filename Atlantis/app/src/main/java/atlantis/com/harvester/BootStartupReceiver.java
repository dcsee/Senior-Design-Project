package atlantis.com.harvester;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by jvronsky on 5/24/15.
 * Starts harvesting service in startup.
 */
public class BootStartupReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        HarvestService.startService(context);
    }
}
