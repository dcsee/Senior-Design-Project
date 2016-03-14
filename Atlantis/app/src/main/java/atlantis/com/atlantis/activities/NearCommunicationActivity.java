package atlantis.com.atlantis.activities;

import atlantis.com.atlantis.communications.nearcommunications.SyncManager;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.atlantis.utils.SharedPreferencesHelper;
import atlantis.com.db.DatabaseManager;
import atlantis.com.model.OTP;

/**
 * Created by jvronsky on 3/6/15.
 */
public abstract class NearCommunicationActivity extends BaseActivity {

    public static final String ACTION_COMMUNICATION_OVER = "com.atlantis.nearcommuniaction.over";

    protected abstract void finishNearCommunicationActivity();

    protected OTP mReceivingOTP;
    protected OTP mSendingOTP;

    /**
     * Extract OTP's extras.
     */
    protected void extractExtras() throws SharedPreferencesHelper.CouldNotWriteToSharedPreferencesException {
        mReceivingOTP = DatabaseManager.getInstance(this).getOTPWithId(
                getIntent().getIntExtra(
                        Extras.OTP_ID_RECEIVING, 0));
        mSendingOTP = DatabaseManager.getInstance(this).getOTPWithId(
                getIntent().getIntExtra(
                        Extras.OTP_ID_SENDING, 0));

        SharedPreferencesHelper sharedPreferencesHelper =
                new SharedPreferencesHelper(this, SyncManager.SYNC_SHARED_PREFERENCES_FOLDER);

        sharedPreferencesHelper.put(SyncManager.SENDING_OTP_KEY, mSendingOTP.getId());
        sharedPreferencesHelper.put(SyncManager.RECEIVING_OTP_KEY, mReceivingOTP.getId());
        sharedPreferencesHelper.put(SyncManager.NEXT_CHUNK_INDEX_KEY, 0);
        long currentTime = System.currentTimeMillis();
        sharedPreferencesHelper.put(SyncManager.TIMESTAMP_KEY, currentTime);
    }
}
