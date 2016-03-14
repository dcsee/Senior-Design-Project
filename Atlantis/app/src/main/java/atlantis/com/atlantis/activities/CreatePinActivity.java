package atlantis.com.atlantis.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.encryption.LocalEncryption;
import atlantis.com.atlantis.encryption.PINActionActivity;
import atlantis.com.atlantis.encryption.PINCreationFailedException;
import atlantis.com.atlantis.encryption.PINTextWatch;

public class CreatePinActivity extends ActionBarActivity implements PINActionActivity {

    private EditText mPinEditText;

    private final CreatePinActivity mSelfActivity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_pin);
        mPinEditText = (EditText) findViewById(R.id.pin_text);
        mPinEditText.requestFocus();
        new PINTextWatch(this, mPinEditText);
    }


    @Override
    public void onBackPressed() {
        Toast.makeText(this, R.string.try_to_go_back_before_login_message, Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    public void PINEntered(String PIN) {
        if (LocalEncryption.isUserPinValid(PIN)) {
            try {
                LocalEncryption.createPinKey(mSelfActivity, PIN);
                mSelfActivity.finish();
            } catch (PINCreationFailedException e) {
                e.printStackTrace();
                Toast.makeText(mSelfActivity, R.string.pin_creation_failed_message,
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(mSelfActivity, R.string.invalid_pin_entered, Toast.LENGTH_SHORT).show();
            mPinEditText.setText("");
        }
    }
}
