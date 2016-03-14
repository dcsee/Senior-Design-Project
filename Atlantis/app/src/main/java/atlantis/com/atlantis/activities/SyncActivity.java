package atlantis.com.atlantis.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.adapters.SyncMethodsAdapter;
import atlantis.com.atlantis.fragments.SyncMethodFragment;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.db.DatabaseManager;
import atlantis.com.model.OTP;

public class SyncActivity extends BaseActivity implements SyncMethodsAdapter.NotebookHolder {

    private OTP mReceivingOTP;
    private OTP mSendingOTP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);
        setTitle(R.string.title_activity_sync);
        mReceivingOTP = DatabaseManager.getInstance(this).getOTPWithId(getIntent().getIntExtra(
                Extras.OTP_ID_RECEIVING, 0));
        mSendingOTP = DatabaseManager.getInstance(this).getOTPWithId(getIntent().getIntExtra(
                Extras.OTP_ID_SENDING, 0));
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new SyncMethodFragment())
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
    public OTP getSendingOTP() {
        return mSendingOTP;
    }

    @Override
    public OTP getReceivingOTP() {
        return mReceivingOTP;
    }
}
