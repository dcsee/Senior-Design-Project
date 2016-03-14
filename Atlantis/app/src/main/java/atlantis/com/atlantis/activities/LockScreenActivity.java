package atlantis.com.atlantis.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.EditText;
import android.widget.Toast;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.encryption.LocalEncryption;
import atlantis.com.atlantis.encryption.PINActionActivity;
import atlantis.com.atlantis.encryption.PINCreationFailedException;
import atlantis.com.atlantis.encryption.PINTextWatch;
import atlantis.com.atlantis.utils.Extras;


public class LockScreenActivity extends ActionBarActivity implements PINActionActivity{

    private EditText mPinText;

    private final LockScreenActivity mSelfActivity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen);
        mPinText = (EditText) findViewById(R.id.pin_text);
        String instruction = getIntent().getStringExtra(Extras.LOCK_SCREEN_INSTRUCTION);
        // Set hint instruction based on where it originated.
        if(instruction == null) {
            mPinText.setHint(getString(R.string.default_lock_screen_hint));
        } else {
            mPinText.setHint(instruction);
        }
        mPinText.requestFocus();
        new PINTextWatch(this, mPinText);
    }

    private void unlockApp() {
        LocalEncryption.setIsLocked(mSelfActivity, false);
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, R.string.try_to_go_back_before_login_message,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void PINEntered(String PIN) {
        if (!LocalEncryption.isUserPinValid(PIN)) {
            Toast.makeText(mSelfActivity, R.string.invalid_pin_sequence,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        boolean loginResult = false;
        try {
            loginResult = LocalEncryption.login(mSelfActivity, PIN);
        } catch (PINCreationFailedException e) {
            Toast.makeText(mSelfActivity, R.string.login_failed_message,
                    Toast.LENGTH_SHORT).show();
        }
        if (loginResult) {
            unlockApp();
            finish();
        } else {
            Toast.makeText(mSelfActivity, R.string.wrong_pin_message,
                    Toast.LENGTH_SHORT).show();
            mPinText.setText("");
        }
    }
}
