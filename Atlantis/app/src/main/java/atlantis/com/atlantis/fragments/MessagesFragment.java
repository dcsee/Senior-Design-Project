package atlantis.com.atlantis.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.j256.ormlite.dao.CloseableIterator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.adapters.MessagesAdapter;
import atlantis.com.atlantis.utils.Events;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.db.DatabaseManager;
import atlantis.com.model.Conversation;
import atlantis.com.model.Message;

/**
 * A simple {@link Fragment} subclass.
 * to handle interaction events.
 */
public class MessagesFragment extends Fragment {

    private MessagesAdapter mMessagesAdapter;
    private RecyclerView mRecyclerView;
    private ConversationHolder mConversationHolder;

    public interface ConversationHolder {
        Conversation getConversation();
    }

    public MessagesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_messages, container, false);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.messages_recycle_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        mConversationHolder = (ConversationHolder) getActivity();
        Conversation conversation = mConversationHolder.getConversation();
        List<Message> messages = new ArrayList<>();
        CloseableIterator<Message> messageIterator = conversation.getMessages().closeableIterator();
        while(messageIterator.hasNext()) {
            messages.add(messageIterator.next());
        }
        messageIterator.closeQuietly();
        mMessagesAdapter = new MessagesAdapter(messages, conversation.getSelf(), getActivity());
        mRecyclerView.setAdapter(mMessagesAdapter);
        mRecyclerView.scrollToPosition(mMessagesAdapter.getItemCount() - 1);

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        broadcastManager.registerReceiver(mDeletedMessageEventReceiver,
                new IntentFilter(Events.MESSAGE_DELETED));
        broadcastManager.registerReceiver(mNewMessageEventReceiver,
                new IntentFilter(Events.MESSAGE_CREATED));
        broadcastManager.registerReceiver(mMessageDeliveredEventReceiver,
                new IntentFilter(Events.MESSAGE_DELIVERED));

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        broadcastManager.unregisterReceiver(mDeletedMessageEventReceiver);
        broadcastManager.unregisterReceiver(mNewMessageEventReceiver);
        broadcastManager.unregisterReceiver(mMessageDeliveredEventReceiver);
    }

    private final MessagesFragment mSelfFragment = this;

    private final BroadcastReceiver mNewMessageEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Conversation conversation = mConversationHolder.getConversation();
            if(intent.getIntExtra(Extras.CONVERSATION_ID, 0) == conversation.getId()) {
                mMessagesAdapter.addMessage(DatabaseManager.getInstance(mSelfFragment.getActivity())
                        .getMessageWithId(intent.getIntExtra(Extras.MESSAGE_ID, 0)));
                mRecyclerView.scrollToPosition(mMessagesAdapter.getItemCount() - 1);
            }
        }
    };

    /**
     * Receiver for when a message has been delivered. Update the message delivered.
     */
    private final BroadcastReceiver mMessageDeliveredEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                mMessagesAdapter.updateMessageWithId(intent.getIntExtra(Extras.MESSAGE_ID, 0));
            } catch (SQLException e) {
                // Swallow event
                e.printStackTrace();
            }
        }
    };

    private final BroadcastReceiver mDeletedMessageEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mMessagesAdapter.removeMessageWithId(intent.getIntExtra(Extras.MESSAGE_ID, 0));
        }
    };
}
