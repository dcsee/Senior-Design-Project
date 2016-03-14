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
import android.widget.Toast;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.adapters.NotebooksAdapter;
import atlantis.com.atlantis.decorations.DividerItemDecoration;
import atlantis.com.atlantis.fragments.MessagesFragment.ConversationHolder;
import atlantis.com.atlantis.utils.Events;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.db.DatabaseManager;
import atlantis.com.model.Notebook;

/**
 * Fragment for showing list of notebooks and help text
 * Created by ricardo on 4/25/15.
 */
public class NotebooksFragment extends Fragment {

    private NotebooksAdapter mNotebooksAdapter;

    public NotebooksFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup contianer,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_notebooks, contianer, false);

        RecyclerView notebooksRecyclerView = (RecyclerView) rootView.findViewById(R.id.notebooks_recycle_view);
        notebooksRecyclerView.setHasFixedSize(true);
        notebooksRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(),
                DividerItemDecoration.VERTICAL_LIST));

        RecyclerView.LayoutManager notebooksLayoutManager = new LinearLayoutManager(getActivity());
        notebooksRecyclerView.setLayoutManager(notebooksLayoutManager);

        View completeContainer = rootView.findViewById(R.id.complete_container);
        if(getActivity().getIntent().getBooleanExtra(Extras.SHOW_COMPLETE, false)) {
            completeContainer.setVisibility(View.VISIBLE);
        } else {
            completeContainer.setVisibility(View.GONE);
        }
        rootView.findViewById(R.id.complete_button).setOnClickListener(mCompleteButtonListener);

        DatabaseManager manager = DatabaseManager.getInstance(getActivity());

        // Load notebooks
        ConversationHolder holder = (ConversationHolder) getActivity();
        List<Notebook> notebooks = new ArrayList<>();
        try {
            notebooks = manager.getNotebooksInConversation(holder.getConversation());
        } catch (SQLException e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), R.string.error_loading_notebooks, Toast.LENGTH_SHORT).show();
        }
        mNotebooksAdapter = new NotebooksAdapter(notebooks, getActivity());
        notebooksRecyclerView.setAdapter(mNotebooksAdapter);

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        broadcastManager.registerReceiver(mOTPSyncedEventReceiver,
                new IntentFilter(Events.OTP_SYNCED));
        broadcastManager.registerReceiver(mNotebookCreatedEventReceiver,
                new IntentFilter(Events.NOTEBOOK_CREATED));
        broadcastManager.registerReceiver(mNotebookDeletedEventReceiver,
                new IntentFilter(Events.NOTEBOOK_DELETED));

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getActivity());
        broadcastManager.unregisterReceiver(mOTPSyncedEventReceiver);
        broadcastManager.unregisterReceiver(mNotebookCreatedEventReceiver);
        broadcastManager.unregisterReceiver(mNotebookDeletedEventReceiver);
    }

    private final NotebooksFragment mSelfFragment = this;

    private final View.OnClickListener mCompleteButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mSelfFragment.getActivity().finish();
        }
    };

    /**
     * Listener for OTP syncing to update status information in notebooks list
     */
    private final BroadcastReceiver mOTPSyncedEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                mNotebooksAdapter.updateNotebookWithOTPId(intent.getIntExtra(Extras.OTP_ID, 0));
            } catch (SQLException e) {
                // Drop event
                e.printStackTrace();
            }
        }
    };

    private final BroadcastReceiver mNotebookCreatedEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(mNotebooksAdapter != null) {
                try {
                    mNotebooksAdapter.addNotebook(DatabaseManager.getInstance(mSelfFragment.getActivity())
                            .getNotebookWithId(intent.getIntExtra(Extras.NOTEBOOK_ID, 0)));
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private final BroadcastReceiver mNotebookDeletedEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(mNotebooksAdapter != null) {
                mNotebooksAdapter.removeNotebookWithId(intent.getIntExtra(Extras.NOTEBOOK_ID, 0));
            }
        }
    };
}
