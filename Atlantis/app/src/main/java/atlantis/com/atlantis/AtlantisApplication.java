package atlantis.com.atlantis;

import android.app.Application;

import atlantis.com.atlantis.encryption.ApplicationStatus;

/**
 * Created by jvronsky on 4/27/15.
 */
public class AtlantisApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ApplicationStatus());
    }
}
