package atlantis.com.atlantis.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.utils.Events;
import atlantis.com.atlantis.utils.NotebookSizeManager;
import atlantis.com.model.Notebook;

/**
 * Fragment for showing the text in a message
 */
public class NotebookFragment extends Fragment {

    /**
     * Updates the new conversation screen to reflect available OTP and estimates text messages
     * and pictures that someone can send with the amount selected.
     */
    private NotebookSizeManager mNotebookSizeManager;

    /**
     * Interface for holding a notebook
     */
    public interface NotebookHolder {
        Notebook getNotebook();
    }

    /**
     * Interface for requesting a re-sync
     */
    public interface OnReSyncListener {
        void onReSyncNotebook(Notebook notebook, int newLength);
    }

    /**
     * The holder of the notebook
     */
    private NotebookHolder mNotebookHolder;
    /**
     * The the listener for re-syncing
     */
    private OnReSyncListener mReSyncListener;
    /**
     * The seek bar for selecting the notebook length
     */
    private SeekBar mSeekBar;

    public NotebookFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mNotebookHolder = (NotebookHolder) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + "must implement NotebookHolder");
        }
        try {
            mReSyncListener = (OnReSyncListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + "must implement OnReSyncListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mNotebookHolder = null;
        mReSyncListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_notebook, container, false);

        Notebook notebook = mNotebookHolder.getNotebook();

        mSeekBar = (SeekBar) rootView.findViewById(R.id.notebook_size_seek_bar);

        rootView.findViewById(R.id.re_sync_notebook_button).setOnClickListener(mReSyncButtonListener);

        TextView contactNameText = ((TextView)rootView.findViewById(R.id.notebook_contact_name_text));
        contactNameText.setText(notebook.getContact().getDisplay());

        mNotebookSizeManager = new NotebookSizeManager(
                (SeekBar) rootView.findViewById(R.id.notebook_size_seek_bar),
                (ProgressBar) rootView.findViewById(R.id.notebook_size_progress_bar),
                (TextView) rootView.findViewById(R.id.text_estimate_view),
                (TextView) rootView.findViewById(R.id.image_estimate_text),
                getActivity());

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mServiceBoundBroadcastReceiver,
                new IntentFilter(Events.SERVICE_BOUND));

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mServiceBoundBroadcastReceiver);
    }

    private final BroadcastReceiver mServiceBoundBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mNotebookSizeManager.updateAmount();
        }
    };

    private final View.OnClickListener mReSyncButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mReSyncListener.onReSyncNotebook(mNotebookHolder.getNotebook(), mNotebookSizeManager.getSelectedLength());
        }
    };

}
