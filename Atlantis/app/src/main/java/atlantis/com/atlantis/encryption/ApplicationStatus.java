package atlantis.com.atlantis.encryption;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/**
 * Created by jvronsky on 4/27/15.
 */
public class ApplicationStatus implements Application.ActivityLifecycleCallbacks{

    private static long resumed = 0;
    private static long paused = 0;

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        resumed++;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        paused++;
    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    public static boolean isAppInForeground() {
        return resumed > paused;
    }
}
