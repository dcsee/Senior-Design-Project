package atlantis.com.atlantis.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.ex.chips.BaseRecipientAdapter;
import com.android.ex.chips.RecipientEditTextView;
import com.android.ex.chips.recipientchip.DrawableRecipientChip;

import java.util.ArrayList;
import java.util.List;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.utils.Events;
import atlantis.com.atlantis.utils.NotebookSizeManager;
import atlantis.com.model.Person;

/**
 * Fragment that shows options for creating conversations, users must implement the
 * {@link atlantis.com.atlantis.fragments.NewConversationFragment.OnCreateConversationListener}
 * Created by ricardo on 2/21/15.
 */
public class NewConversationFragment extends Fragment {

    private RecipientEditTextView mRecipientEditTextView;

    /**
     * Updates the new conversation screen to reflect available OTP and estimates text messages
     * and pictures that someone can send with the amount selected.
     */
    private NotebookSizeManager mNotebookSizeManager;

    public interface OnCreateConversationListener {
        void onCreateConversation(String name, List<Person> selectedPeople, int length);
    }

    public interface OTPAmountHolder {
        long getOTPAmount();
    }

    private OnCreateConversationListener mListener;

    public NewConversationFragment() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnCreateConversationListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + "must implement OnCreatePressedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_new_conversation, container, false);
        rootView.findViewById(R.id.conversation_recipient_edit_text_view).requestFocus();
        Button createButton = (Button) rootView.findViewById(R.id.create_conversation_button);
        createButton.setOnClickListener(mCreateButtonListener);

        mNotebookSizeManager = new NotebookSizeManager(
                (SeekBar) rootView.findViewById(R.id.notebook_size_seek_bar),
                (ProgressBar) rootView.findViewById(R.id.notebook_size_progress_bar),
                (TextView) rootView.findViewById(R.id.text_estimate_view),
                (TextView) rootView.findViewById(R.id.image_estimate_text),
                getActivity());

        mRecipientEditTextView = (RecipientEditTextView) rootView.findViewById(R.id.conversation_recipient_edit_text_view);
        mRecipientEditTextView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        // Accept all strings
        mRecipientEditTextView.setValidator(new AutoCompleteTextView.Validator() {
            @Override
            public boolean isValid(CharSequence text) {
                return true;
            }

            @Override
            public CharSequence fixText(CharSequence invalidText) {
                return invalidText;
            }
        });
        mRecipientEditTextView.setAdapter(new BaseRecipientAdapter(BaseRecipientAdapter.QUERY_TYPE_EMAIL, getActivity()));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mServiceBoundBroadcastReceiver,
                new IntentFilter(Events.SERVICE_BOUND));

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mServiceBoundBroadcastReceiver);
    }

    private final NewConversationFragment mSelfFragment = this;

    private final BroadcastReceiver mServiceBoundBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mNotebookSizeManager.updateAmount();
        }
    };

    /**
     * Listener for when create button is pressed
     */
    private final View.OnClickListener mCreateButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mRecipientEditTextView.commitDefault();
            List<Person> people = getSelectedPeople();
            if(people.size() > 0) {
                mListener.onCreateConversation(getDefaultConversationName(), people, mNotebookSizeManager.getSelectedLength());
            } else {
                Toast.makeText(mSelfFragment.getActivity(), R.string.need_one_contact_text, Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * Gets the people from the RecipientsEditTextView
     * @return List of people entered
     */
    private List<Person> getSelectedPeople() {
        ArrayList<Person> people = new ArrayList<>(mRecipientEditTextView.getRecipients().length);
        for(DrawableRecipientChip recipientChip : mRecipientEditTextView.getRecipients()) {
            Person person = new Person();
            person.setLookupKey(recipientChip.getLookupKey());
            person.setDisplay(recipientChip.getDisplay().toString());
            people.add(person);
        }
        return people;
    }

    /**
     * Generates the default conversation name from the people in the RecipientEditTextView as a
     * comma and ampersand separated list
     * @return String with the names of the recipients
     */
    private String getDefaultConversationName() {
        String conversationName = "";
        String nameSeparator = getResources().getString(R.string.name_separator);
        String nameLastSeparator = getResources().getString(R.string.name_last_separator);
        DrawableRecipientChip[] recipientChips = mRecipientEditTextView.getSortedRecipients();
        for(int i = 0; i < recipientChips.length; i++) {
            if(i == recipientChips.length - 1 && recipientChips.length > 1) {
                conversationName += nameLastSeparator;
            } else if(i > 0) {
                conversationName += nameSeparator;
            }
            conversationName += recipientChips[i].getDisplay();
        }
        return conversationName;
    }
}
