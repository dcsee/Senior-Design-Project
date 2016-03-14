package atlantis.com.atlantis.encryption;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

/**
 * Created by jvronsky on 5/4/15.
 */
public class PINTextWatch implements TextWatcher {

    private static final int TIMEOUT = 50;
    private final PINActionActivity mPINActionActivity;
    private Handler mUIHandler;

    public PINTextWatch(PINActionActivity pinActionActivity, EditText editText) {
        this.mPINActionActivity = pinActionActivity;
        editText.addTextChangedListener(this);
        mUIHandler = new Handler();
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(final Editable s) {
        if(s.length() == LocalEncryption.USER_PIN_LENGTH) {
            mUIHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    pinEntered(s.toString());
                }
            }, TIMEOUT);
        }
    }

    private void pinEntered(String s) {
        mPINActionActivity.PINEntered(s.toString());
    }
}
