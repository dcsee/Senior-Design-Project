package atlantis.com.atlantis.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.encryption.ChangePINPoolManager;
import atlantis.com.atlantis.encryption.LocalEncryption;
import atlantis.com.atlantis.encryption.PINActionActivity;
import atlantis.com.atlantis.encryption.PINCreationFailedException;
import atlantis.com.atlantis.encryption.PINTextWatch;
import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;
import atlantis.com.db.DatabaseManager;
import atlantis.com.model.Notebook;

import static atlantis.com.atlantis.encryption.ChangePINPoolManager.getInstance;

public class ChangePinActivity extends BaseActivity implements PINActionActivity {

    private static final String DIALOG_TITLE = "Changing PIN";
    private static final String MESSAGE = "This might take a while";
    private final Activity mSelf = this;
    private ChangeOTPAsyncTask mChangeOTPAsyncTask;
    private ProgressDialog mProgressDialog;
    private String mPIN;

    private EditText mNewPIN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_pin);
        mNewPIN = (EditText) findViewById(R.id.change_pin_edit_text);
        mChangeOTPAsyncTask = new ChangeOTPAsyncTask();
        new PINTextWatch(this, mNewPIN);
    }

    @Override
    public void PINEntered(String PIN) {
        if (!LocalEncryption.isUserPinValid(PIN)) {
            Toast.makeText(mSelf, R.string.invalid_pin_sequence,
                    Toast.LENGTH_SHORT).show();
            return;
        }
        this.mPIN = PIN;
        mChangeOTPAsyncTask.execute();
    }

    private class ChangeOTPAsyncTask extends AsyncTask<Void, Void, Boolean> {

        ChangePINPoolManager mChangePINPoolManager;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(mSelf);
            mProgressDialog.setTitle(DIALOG_TITLE);
            mProgressDialog.setMessage(MESSAGE);
            mProgressDialog.setProgress(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(true);
            mSelf.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mProgressDialog.show();
                }
            });
            mChangePINPoolManager = getInstance();
            mChangePINPoolManager.setNewPin(mPIN);
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if(aBoolean == true) {
                try {
                    // When complete change the key for the app and login.
                    LocalEncryption.getInstance(mSelf).createPinKey(mSelf, mPIN);
                    LocalEncryption.getInstance(mSelf).login(mSelf, mPIN);
                    mSelf.finish();
                } catch (PINCreationFailedException | PINNotCreatedException | NotAuthenticatedException e) {
                    e.printStackTrace();
                }
                mProgressDialog.cancel();
            }
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            boolean result = true;
            try {
                // Load all the notesbooks
                DatabaseManager databaseManager = DatabaseManager.getInstance(mSelf);
                List<Notebook> notebooks = databaseManager.getAllNotebooks();
                List<Future<Boolean>> futures = mChangePINPoolManager.addAllOTPFilesAndRun(mSelf, notebooks);
                for(Future<Boolean> future : futures) {
                    if(future.get().equals(false)) {
                        result = false;
                    }
                }
            } catch (InterruptedException | ExecutionException | SQLException e) {
                return new Boolean(false);
            }
            return new Boolean(result);
        }
    }
}
