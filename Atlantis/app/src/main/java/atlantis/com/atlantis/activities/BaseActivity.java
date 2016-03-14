package atlantis.com.atlantis.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.Toast;

import atlantis.com.atlantis.encryption.ApplicationStatus;
import atlantis.com.atlantis.encryption.LocalEncryption;

/**
 * Created by jvronsky on 4/12/15.
 */
public class BaseActivity extends ActionBarActivity {

    // Whether this activity has started another one.
    private int launchedActivities = 0;
    private boolean mWentToLockScreen = false;
    final BaseActivity mSelfActivity = this;

    private boolean mHasFocus;

    public BaseActivity() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(isFirstTime()) {
            LocalEncryption.setIsLocked(this, false);
            startActivity(new Intent(this, CreatePinActivity.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(LocalEncryption.getIsLocked(this)) {
            mWentToLockScreen = true;
            startLockScreenActivity();
        } else if(mWentToLockScreen){
            mWentToLockScreen = false;
            onReturnFromLockScreen();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        incrementLaunchedActivities();
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mHasFocus = false;
        if(isLockNeeded()) {
            lockApp();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mHasFocus = hasFocus;
        if(hasFocus && launchedActivities > 0) {
            launchedActivities--;
        }
    }

    @Override
    public void onBackPressed() {
        if(!(this instanceof MainActivity)) {
            incrementLaunchedActivities();
        }
        super.onBackPressed();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        if(requestCode != -1) {
            incrementLaunchedActivities();
        }
        super.startActivityForResult(intent, requestCode);
    }

    protected void onReturnFromLockScreen() {

    }

    protected void quitWithErrorToast(String message, Intent nextActivity) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        startActivity(nextActivity);
        finish();
    }

    @Override
    public void startActivity(Intent intent) {
        incrementLaunchedActivities();
        super.startActivity(intent);
    }

    private void incrementLaunchedActivities() {
        launchedActivities++;
    }

    private void lockApp() {
        LocalEncryption.setIsLocked(this, true);
    }

    protected void startLockScreenActivity() {
        Intent intent = new Intent(this, LockScreenActivity.class);
        startActivity(intent);
    }

    private boolean isFirstTime() {
        return !LocalEncryption.doesPinExist(this);
    }

    private boolean isLockNeeded() {
        return (launchedActivities == 0) && !ApplicationStatus.isAppInForeground() && !mHasFocus;
    }
}
