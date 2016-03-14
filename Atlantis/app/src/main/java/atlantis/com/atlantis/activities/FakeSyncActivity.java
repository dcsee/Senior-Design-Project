package atlantis.com.atlantis.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.encryption.PINCreationFailedException;
import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;
import atlantis.com.atlantis.fragments.FakeSyncFragment;
import atlantis.com.atlantis.fragments.FakeSyncingFragment;
import atlantis.com.atlantis.utils.Events;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.db.DatabaseManager;
import atlantis.com.model.OTP;
import atlantis.com.model.impls.FileOTPManager;
import atlantis.com.model.impls.OTPFileOutputStream;

public class FakeSyncActivity extends BindingActivity implements FakeSyncFragment.OnFragmentInteractionListener {

    private OTP mOTP;

    private static final int FAKE_OTP_SYNC_LENGTH = 1024 * 1024 * 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        setTitle(R.string.title_activity_sync);
        mOTP = DatabaseManager.getInstance(this).getOTPWithId(
                getIntent().getIntExtra(Extras.OTP_ID_RECEIVING, 0));
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new FakeSyncFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sync, menu);
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

    @Override
    public void onFragmentInteraction() {
        new FakeSyncTask().execute();
    }

    private final FakeSyncActivity mSelfActivity = this;

    private class FakeSyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mSelfActivity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new FakeSyncingFragment())
                    .commit();
        }

        @Override
        protected Void doInBackground(Void... params) {
            // Make an OTP coming from a fake sync
            // When SecureRandomGenerator is set to debug, will create same OTP for debugging
            FileOTPManager manager;
            try {
                manager = new FileOTPManager(mSelfActivity);
                OTP newOTP = manager.createOTP(FAKE_OTP_SYNC_LENGTH, mHarvestService);
                mOTP.setDataId(newOTP.getDataId());
                mOTP.setLength(newOTP.getLength());
            } catch (NotAuthenticatedException | OTPFileOutputStream.OTPFileOutputStreamException | PINCreationFailedException | IOException | PINNotCreatedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            DatabaseManager manager = DatabaseManager.getInstance(mSelfActivity);
            manager.updateOTP(mOTP);

            // Start notebooks activity
            Intent intent = new Intent(Events.OTP_SYNCED);
            intent.putExtra(Extras.OTP_ID, mOTP.getId());
            LocalBroadcastManager.getInstance(mSelfActivity).sendBroadcast(intent);

            mSelfActivity.finish();
        }
    }
}
