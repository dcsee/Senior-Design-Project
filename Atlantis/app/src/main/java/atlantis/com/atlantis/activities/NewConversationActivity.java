package atlantis.com.atlantis.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.encryption.PINCreationFailedException;
import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;
import atlantis.com.atlantis.fragments.CreatingNotebookFragment;
import atlantis.com.atlantis.fragments.NewConversationFragment;
import atlantis.com.atlantis.utils.ContactUtils;
import atlantis.com.atlantis.utils.Events;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.db.DatabaseManager;
import atlantis.com.model.Conversation;
import atlantis.com.model.Notebook;
import atlantis.com.model.OTP;
import atlantis.com.model.Person;
import atlantis.com.model.impls.FileOTPManager;
import atlantis.com.model.impls.OTPFileOutputStream;
import atlantis.com.model.interfaces.OTPManager;

/**
 * Activity for starting a new conversation and generating the sending OTPs
 */
public class NewConversationActivity extends BindingActivity
        implements NewConversationFragment.OnCreateConversationListener, CreatingNotebookFragment.OnCancelNotebookListener, NewConversationFragment.OTPAmountHolder {

    private AsyncTask<Void, Integer, Void> mCreateNotebooksTask;
    private boolean mActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_conversation);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new NewConversationFragment())
                    .commit();
        }
        mActive = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_new_conversation, menu);
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
    protected void onResume() {
        super.onResume();

        mActive = true;
    }

    @Override
    protected void onPause() {
        super.onPause();

        mActive = false;
    }

    private boolean isActive() {
        return mActive;
    }

    /**
     * Self reference
     */
    private final NewConversationActivity mSelfActivity = this;

    /**
     * Starts the OTP creation task when a new conversation is requested from the fragment
     * @param name The name of the conversation
     * @param people The people in the conversation
     * @param length The length of the OTP to generate
     */
    @Override
    public void onCreateConversation(String name, List<Person> people, int length) {
        mCreateNotebooksTask = new CreateNotebooksTask(name, people, length);
        mCreateNotebooksTask.execute();
    }

    public void onCancelConversation() {
        mCreateNotebooksTask.cancel(true);
    }

    @Override
    public long getOTPAmount() {
        return mHarvestService.getAmountOTP();
    }

    /**
     * Helper for creating sending OTPs for each person and moving to notebooks activity
     */
    private class CreateNotebooksTask extends AsyncTask<Void, Integer, Void> {

        /**
         * Name of the conversation
         */
        private final String mName;
        /**
         * Length of the OTPs to generate
         */
        private final int mLength;
        /**
         * The people in the conversation
         */
        private final List<Person> mPeople;
        /**
         * Stores the OTP for each person
         */
        private final Map<Person, OTP> mSendingOTPsByPerson;
        /**
         * The conversation being created
         */
        private Conversation mConversation;

        /**
         * Initializes the task for creating new notebooks for a new conversation
         * @param name The name of the conversation
         * @param people The people in the conversation
         * @param length The length of the OTPs
         */
        private CreateNotebooksTask(String name, List<Person> people, int length) {
            this.mName = name;
            this.mLength = length;
            this.mPeople = people;
            mSendingOTPsByPerson = new HashMap<>();
        }

        /**
         * Setup creation of conversation
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dismissKeyboard();
            switchToCreatingNotebookFragment();
            setUpConversation();
        }

        /**
         * Creates a new conversation with a "self" person and adds to database
         */
        private void setUpConversation() {
            DatabaseManager manager = DatabaseManager.getInstance(mSelfActivity);

            Person sender = new Person();
            sender.setDisplay(getResources().getString(R.string.self_display));
            sender.setLookupKey(ContactUtils.getSelfLookupKey(mSelfActivity));

            manager.addPerson(sender);

            mConversation = new Conversation();
            mConversation.setName(mName);
            mConversation.setSelf(sender);
            manager.addConversation(mConversation);
        }

        /**
         * Used to hide keyboard when fragment changes to creating notebooks fragment
         */
        private void dismissKeyboard() {
            InputMethodManager inputMethodManager = (InputMethodManager) mSelfActivity.getSystemService(Activity.INPUT_METHOD_SERVICE);
            View focusedView = mSelfActivity.getCurrentFocus();
            if(focusedView != null) {
                inputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
            }
        }

        /**
         * Switches to the fragment that shows the loading notebooks
         */
        private void switchToCreatingNotebookFragment() {
            mSelfActivity.getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new CreatingNotebookFragment())
                    .commit();
        }

        /**
         * Create the sending OTPs for each person in the conversation
         * @param params Not used
         * @return null
         */
        @Override
        protected Void doInBackground(Void... params) {
            OTPManager otpManager;
            try {
                otpManager = new FileOTPManager(mSelfActivity);
                for(Person person : mPeople) {
                    mSendingOTPsByPerson.put(person, otpManager.createOTP(mLength, mHarvestService));
                    if(isCancelled()) {
                        break;
                    }
                }
            } catch (IOException | PINCreationFailedException | PINNotCreatedException | NotAuthenticatedException | OTPFileOutputStream.OTPFileOutputStreamException e) {
                e.printStackTrace();
                mSelfActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mSelfActivity, R.string.error_creating_conversation,
                                Toast.LENGTH_SHORT).show();
                    }
                });
                cancel(true);
            }

            return null;
        }

        /**
         * Adds each notebook and person to the database and sets up empty receiving OTPs
         * Notifies listeners of new conversation and moves to notebooks activity
         * @param aVoid Not used
         */
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            DatabaseManager manager = DatabaseManager.getInstance(mSelfActivity);

            for(Person person : mPeople) {
                manager.addPerson(person);

                OTP receivingOTP = new OTP();
                OTP sendingOTP = mSendingOTPsByPerson.get(person);
                manager.addOTP(receivingOTP);
                manager.addOTP(sendingOTP);

                Notebook notebook = new Notebook();
                notebook.setContact(person);
                notebook.setConversation(mConversation);
                notebook.setSendingOTP(sendingOTP);
                notebook.setReceivingOTP(receivingOTP);
                manager.addNotebook(notebook);
            }

            notifyConversationCreated();

            if(mSelfActivity.isActive()) {
                startActivities();
            }
        }

        protected void onCancelled(Void aVoid) {
            DatabaseManager manager = DatabaseManager.getInstance(mSelfActivity);
            Person sender = mConversation.getSelf();
            try {
                manager.removePerson(sender);
                manager.removeConversation(mConversation.getId());
            } catch (SQLException e) {
                Toast.makeText(mSelfActivity, R.string.error_deleting_conversation, Toast.LENGTH_SHORT).show();
            }
            finish();
        }

        /**
         * Pushes the conversation activity and the notebook activity to set up backstack
         */
        private void startActivities() {
            Intent conversationIntent = new Intent(mSelfActivity, ConversationActivity.class);
            conversationIntent.putExtra(Extras.CONVERSATION_ID, mConversation.getId());
            mSelfActivity.startActivity(conversationIntent);

            Intent notebooksIntent = new Intent(mSelfActivity, NotebooksActivity.class);
            notebooksIntent.putExtra(Extras.CONVERSATION_ID, mConversation.getId());
            notebooksIntent.putExtra(Extras.SHOW_COMPLETE, true);
            mSelfActivity.startActivity(notebooksIntent);
        }

        /**
         * Notifies listeners of new conversation
         */
        void notifyConversationCreated() {
            Intent intent = new Intent(Events.CONVERSATION_CREATED);
            intent.putExtra(Extras.CONVERSATION_ID, mConversation.getId());
            LocalBroadcastManager.getInstance(mSelfActivity).sendBroadcast(intent);
        }
    }
}
