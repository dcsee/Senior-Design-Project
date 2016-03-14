package atlantis.com.atlantis.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.adapters.ConversationsAdapter;
import atlantis.com.atlantis.decorations.DividerItemDecoration;
import atlantis.com.atlantis.encryption.LocalEncryption;
import atlantis.com.atlantis.encryption.PINCreationFailedException;
import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;
import atlantis.com.atlantis.utils.DrawableUtils;
import atlantis.com.atlantis.utils.Events;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.db.DatabaseManager;
import atlantis.com.harvester.HarvestService;
import atlantis.com.model.Conversation;
import atlantis.com.model.exceptions.NotEnoughOTPException;
import atlantis.com.model.impls.OTPFileInputStream;
import atlantis.com.server.MessageCourier;

/**
 * Activity showing all the conversations and button for starting new conversation
 */
public class MainActivity extends BaseActivity implements View.OnClickListener {

    private RecyclerView mConversationsRecyclerView;
    private ConversationsAdapter mConversationsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getApplicationContext().startService(new Intent(this, HarvestService.class));
        mConversationsRecyclerView = (RecyclerView) findViewById(R.id.conversations_recycle_view);
        mConversationsRecyclerView.setHasFixedSize(true);

        mConversationsRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));

        RecyclerView.LayoutManager conversationsLayoutManger = new LinearLayoutManager(this);
        mConversationsRecyclerView.setLayoutManager(conversationsLayoutManger);

        ImageView mNewConversationFAB = (ImageView) findViewById(R.id.fab_add);
        mNewConversationFAB.setOnClickListener(this);

        DrawableUtils.tintDrawablesIfNotLollipop(new Drawable[] {
                getResources().getDrawable(R.drawable.fab_add)
        }, getResources().getColor(R.color.action_bar_icon_color));

        LocalBroadcastManager.getInstance(this).registerReceiver(mNewConversationEventReceiver,
                new IntentFilter(Events.CONVERSATION_CREATED));
        LocalBroadcastManager.getInstance(this).registerReceiver(mDeleteConversationEventReceiver,
                new IntentFilter(Events.CONVERSATION_DELETED));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!LocalEncryption.getIsLocked(this)) {
            DatabaseManager manager = DatabaseManager.getInstance(this);

            try {
                // Load conversations and request new messages for each
                MessageCourier messageCourier = MessageCourier.getInstance();
                List<Conversation> conversations = manager.getAllConversations();
                for(Conversation conversation : conversations) {
                    try {
                        messageCourier.requestMessagesInConversation(conversation, this);
                        messageCourier.sendQueuedMessagesInConversation(conversation, this);
                    } catch (NotEnoughOTPException e) {
                        // Swallow not enough OTP to check
                        e.printStackTrace();
                    }
                }
                mConversationsAdapter = new ConversationsAdapter(conversations);
                mConversationsRecyclerView.setAdapter(mConversationsAdapter);
            } catch (NotAuthenticatedException | PINNotCreatedException | IOException | OTPFileInputStream.InvalidBufferException | PINCreationFailedException | SQLException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.error_loading_conversation, Toast.LENGTH_SHORT).show();
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mNewConversationEventReceiver);
    }

    private final MainActivity mSelfActivity = this;
    private final BroadcastReceiver mNewConversationEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(mConversationsAdapter != null) {
                mConversationsAdapter.addConversation(DatabaseManager.getInstance(mSelfActivity)
                        .getConversationWithId(intent.getIntExtra(Extras.CONVERSATION_ID, 0)));
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            Intent settingsIntent = new Intent (this, SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        if(id == R.id.change_pin_setting) {
            Intent intent = new Intent(this, ChangePinActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        Intent conversationIntent = new Intent(v.getContext(), NewConversationActivity.class);
        v.getContext().startActivity(conversationIntent);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item){
        if(item.getTitle().equals(getResources().getString(R.string.context_menu_delete))) {
            int conversationId = ConversationsAdapter.getSelectedId();
            DatabaseManager manager = DatabaseManager.getInstance(this);
            try {
                Intent intent = new Intent(Events.CONVERSATION_DELETED);
                intent.putExtra(Extras.CONVERSATION_ID, conversationId);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                manager.removeConversation(conversationId);
                Toast.makeText(this, R.string.result_conversation_deleted, Toast.LENGTH_SHORT).show();
            } catch (SQLException e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.error_deleting_conversation, Toast.LENGTH_SHORT).show();
            }
            return true;
        } else {
            Toast.makeText(this, R.string.result_select_failed, Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private final BroadcastReceiver mDeleteConversationEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mConversationsAdapter.removeConversationWithId(intent.getIntExtra(Extras.CONVERSATION_ID, 0));
        }
    };

}
