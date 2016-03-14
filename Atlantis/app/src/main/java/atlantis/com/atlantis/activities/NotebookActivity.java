package atlantis.com.atlantis.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import com.j256.ormlite.misc.TransactionManager;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.encryption.PINCreationFailedException;
import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;
import atlantis.com.atlantis.fragments.NewConversationFragment;
import atlantis.com.atlantis.fragments.NotebookFragment;
import atlantis.com.atlantis.utils.Events;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.db.DatabaseManager;
import atlantis.com.model.Notebook;
import atlantis.com.model.OTP;
import atlantis.com.model.impls.FileOTPManager;
import atlantis.com.model.impls.OTPFileOutputStream;
import atlantis.com.model.interfaces.OTPManager;

/**
 * Activity to show details about a notebook and re-sync the notebook
 */
public class NotebookActivity extends BindingActivity implements NotebookFragment.NotebookHolder,
        NotebookFragment.OnReSyncListener, NewConversationFragment.OTPAmountHolder {

    /**
     * The notebook being viewed
     */
    private Notebook mNotebook;
    /**
     * Self reference
     */
    private final NotebookActivity mSelfActivity = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_notebook);

        DatabaseManager manager = DatabaseManager.getInstance(this);
        try {
            mNotebook = manager.getNotebookWithId(
                    getIntent().getIntExtra(Extras.NOTEBOOK_ID, 0));
        } catch (SQLException e) {
            e.printStackTrace();

            Toast.makeText(this, R.string.error_loading_notebook, Toast.LENGTH_SHORT).show();
            finish();
        }

        if(savedInstanceState == null) {
            Fragment fragment = new NotebookFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment)
                    .commit();
        }
    }

    /**
     * Get the notebook for this activity
     * @return Then notebook
     */
    @Override
    public Notebook getNotebook() {
        return mNotebook;
    }

    /**
     * Re-sync the notebook
     * @param notebook The notebook to re-sync
     * @param newLength The length of the new notebook
     */
    @Override
    public void onReSyncNotebook(Notebook notebook, int newLength) {
        new CreateNotebookTask(notebook).execute(newLength);
    }

    @Override
    public long getOTPAmount() {
        return mHarvestService.getAmountOTP();
    }

    /**
     * Helper for creating the new OTPs moving to sync activity
     */
    private class CreateNotebookTask extends AsyncTask<Integer, Integer, OTP> {

        /**
         * The current notebook being re-synced
         */
        private final Notebook mCurrentNotebook;
        /**
         * The new sending OTP
         */
        private OTP mNewSendingOTP;
        /**
         * The dialog to show creation of the new OTP
         */
        private ProgressDialog mProgressDialog;

        /**
         * Initialized with a notebook to re-sync
         * @param currentNotebook The notebook
         */
        private CreateNotebookTask(Notebook currentNotebook) {
            this.mCurrentNotebook = currentNotebook;
        }

        /**
         * Show progress dialog
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mProgressDialog = new ProgressDialog(mSelfActivity);
            mProgressDialog.setTitle(R.string.creating_notebook_text);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.setProgressNumberFormat(null);
            mProgressDialog.setProgressPercentFormat(null);
            mProgressDialog.show();
        }

        /**
         * Create a new OTP or show error
         * @param integers The size of the OTP to generate
         * @return The new OTP
         */
        @Override
        protected OTP doInBackground(Integer... integers) {
            OTPManager otpManager;
            try {
                otpManager = new FileOTPManager(mSelfActivity);
                return otpManager.createOTP(integers[0], mHarvestService);
            } catch (NotAuthenticatedException | PINNotCreatedException | IOException | OTPFileOutputStream.OTPFileOutputStreamException | PINCreationFailedException e) {
                e.printStackTrace();
                mSelfActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mSelfActivity, getString(R.string.error_could_not_create_notebook),
                                Toast.LENGTH_SHORT).show();
                    }
                });
                cancel(true);
            }

            return null;
        }

        /**
         * Hide progress dialog and switch to new notebook in database
         * @param sendingOTP The created OTP
         */
        @Override
        protected void onPostExecute(OTP sendingOTP) {
            super.onPostExecute(sendingOTP);

            mProgressDialog.dismiss();
            mNewSendingOTP = sendingOTP;

            DatabaseManager manager = DatabaseManager.getInstance(mSelfActivity);
            try {
                // Update to new notebook in transaction
                Notebook newNotebook = TransactionManager.callInTransaction(
                        manager.getConnectionSource(),
                        mCreateNewNotebook);

                startActivities(newNotebook);
            } catch (SQLException e) {
                e.printStackTrace();
                Toast.makeText(mSelfActivity, getString(R.string.error_could_not_create_notebook),
                        Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Shows the sync activity and finished this activity so it returns to the notebook list
         * @param newNotebook The notebook to sync
         */
        private void startActivities(Notebook newNotebook) {
            mSelfActivity.finish();

            Intent syncIntent = new Intent(mSelfActivity, SyncActivity.class);
            syncIntent.putExtra(Extras.OTP_ID_RECEIVING,
                    newNotebook.getReceivingOTP().getId());
            syncIntent.putExtra(Extras.OTP_ID_SENDING,
                    newNotebook.getSendingOTP().getId());
            mSelfActivity.startActivity(syncIntent);
        }

        /**
         * Updates database to the new notebook and deletes the old one
         */
        private final Callable<Notebook> mCreateNewNotebook = new Callable<Notebook>() {
            @Override
            public Notebook call() throws Exception {
                DatabaseManager manager = DatabaseManager.getInstance(mSelfActivity);

                // Add new OTPs
                OTP receivingOTP = new OTP();
                manager.addOTP(receivingOTP);
                manager.addOTP(mNewSendingOTP);

                // Add new notebook
                Notebook newNotebook = new Notebook();
                newNotebook.setContact(mCurrentNotebook.getContact());
                newNotebook.setConversation(mCurrentNotebook.getConversation());
                newNotebook.setSendingOTP(mNewSendingOTP);
                newNotebook.setReceivingOTP(receivingOTP);
                manager.addNotebook(newNotebook);

                // Delete old OTP files
                FileOTPManager otpManager = new FileOTPManager(mSelfActivity);
                otpManager.deleteOTPFile(mCurrentNotebook.getReceivingOTP());
                otpManager.deleteOTPFile(mCurrentNotebook.getSendingOTP());

                // Delete old OTPs and notebook
                manager.removeOTP(mCurrentNotebook.getReceivingOTP());
                manager.removeOTP(mCurrentNotebook.getSendingOTP());
                manager.removeNotebook(mCurrentNotebook);

                // Broadcast out events
                LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(mSelfActivity);

                Intent intent = new Intent(Events.NOTEBOOK_DELETED);
                intent.putExtra(Extras.NOTEBOOK_ID, mCurrentNotebook.getId());
                broadcastManager.sendBroadcast(intent);

                intent = new Intent(Events.NOTEBOOK_CREATED);
                intent.putExtra(Extras.NOTEBOOK_ID, newNotebook.getId());
                broadcastManager.sendBroadcast(intent);

                return newNotebook;
            }
        };
    }

}
