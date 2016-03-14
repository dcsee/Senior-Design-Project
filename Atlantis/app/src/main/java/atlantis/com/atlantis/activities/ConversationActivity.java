package atlantis.com.atlantis.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.adapters.MessagesAdapter;
import atlantis.com.atlantis.encryption.PINCreationFailedException;
import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;
import atlantis.com.atlantis.fragments.ConversationFragment;
import atlantis.com.atlantis.fragments.MessagesFragment;
import atlantis.com.atlantis.utils.DrawableUtils;
import atlantis.com.atlantis.utils.Events;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.db.DatabaseManager;
import atlantis.com.model.Conversation;
import atlantis.com.model.Message;
import atlantis.com.model.MessageContent;
import atlantis.com.model.Person;
import atlantis.com.model.exceptions.NotEnoughOTPException;
import atlantis.com.model.impls.OTPFileInputStream;
import atlantis.com.server.MessageCourier;

/**
 * Activity for showing messages in a conversation and replying
 */
public class ConversationActivity extends BaseActivity
        implements MessagesFragment.ConversationHolder, ConversationFragment.OnSendMessageListener {

    private static final int PICK_CONTENT_REQUEST_CODE = 1;

    private Conversation mConversation;
    private Person mSender;
    private ConversationFragment.OnSendMessageListener mListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        DatabaseManager manager = DatabaseManager.getInstance(this);
        mConversation = manager.getConversationWithId(
                getIntent().getIntExtra(Extras.CONVERSATION_ID, 0));
        mSender = mConversation.getSelf();

        receiveMessages();

        setTitle(mConversation.getName());
        mListener = this;
        getSupportActionBar().setElevation(0);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ConversationFragment())
                    .commit();
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mOTPSyncedEventReceiver,
                new IntentFilter(Events.OTP_SYNCED));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mOTPSyncedEventReceiver);
    }

    private final BroadcastReceiver mOTPSyncedEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                DatabaseManager databaseManager = DatabaseManager.getInstance(context);
                databaseManager.refreshConversation(mConversation);
                mSender = mConversation.getSelf();
                receiveMessages();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Request new messages to refresh the conversation
     */
    private void receiveMessages() {
        try {
            MessageCourier.getInstance().requestMessagesInConversation(mConversation, this);
        } catch (NotAuthenticatedException | NotEnoughOTPException | PINNotCreatedException | IOException | OTPFileInputStream.InvalidBufferException | PINCreationFailedException | SQLException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_refreshing_conversation, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_conversation, menu);

        Drawable icons[] = {
                menu.findItem(R.id.action_refresh).getIcon(),
                menu.findItem(R.id.action_sync).getIcon(),
        };
        DrawableUtils.tintDrawablesIfNotLollipop(icons, getResources().getColor(R.color.action_bar_icon_color));

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == R.id.action_refresh) {
            receiveMessages();
        } else if(id == R.id.action_sync) {
            // Start the notebooks activity
            Intent intent = new Intent(this, NotebooksActivity.class);
            intent.putExtra(Extras.CONVERSATION_ID, mConversation.getId());
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Conversation getConversation() {
        return mConversation;
    }

    /**
     * Send a message and notify listeners of new message
     * @param message The message to send (from fragment)
     */
    @Override
    public void onSendMessage(Message message) {
        message.setSender(mSender);
        message.setConversation(mConversation);
        message.setTimestamp();
        DatabaseManager manager = DatabaseManager.getInstance(this);
        manager.addMessage(message);

        try {
            mConversation.setDescription(message.getSerializedContent().getStringContent());

            manager.updateConversation(mConversation);

            MessageCourier messageCourier = MessageCourier.getInstance();
            messageCourier.sendMessageInConversation(message, mConversation, this);

            Intent intent = new Intent(Events.MESSAGE_CREATED);
            intent.putExtra(Extras.MESSAGE_ID, message.getId());
            intent.putExtra(Extras.CONVERSATION_ID, mConversation.getId());
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } catch (IOException | PINCreationFailedException | OTPFileInputStream.InvalidBufferException | PINNotCreatedException | NotAuthenticatedException | SQLException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_sending_message, Toast.LENGTH_SHORT).show();
        } catch (NotEnoughOTPException e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_not_enough_otp, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICK_CONTENT_REQUEST_CODE) {
            if(resultCode == Activity.RESULT_OK) {
                sendAttachmentMessage(data.getData());
            }
        }
    }

    private void sendAttachmentMessage(Uri attachmentURI) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), attachmentURI);
            Message message = new Message();
            MessageContent messageContent = new MessageContent(bitmap, this);
            message.setSerializedContent(messageContent);
            mListener.onSendMessage(message);
        } catch (IOException | NumberFormatException e) {
            Toast.makeText(this, R.string.error_loading_image, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(item.getTitle().equals(getResources().getString(R.string.context_menu_delete))) {
            int messageId = MessagesAdapter.getSelectedId();
            DatabaseManager manager = DatabaseManager.getInstance(this);
            try {
                Intent intent = new Intent(Events.MESSAGE_DELETED);
                intent.putExtra(Extras.MESSAGE_ID, messageId);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                manager.removeMessage(messageId);
                Toast.makeText(this, R.string.result_message_deleted, Toast.LENGTH_SHORT).show();
            } catch (SQLException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.error_deleting_message, Toast.LENGTH_SHORT).show();
            }
            return true;
        } else {
            Toast.makeText(this, R.string.result_select_failed, Toast.LENGTH_SHORT).show();
            return false;
        }
    }
}
