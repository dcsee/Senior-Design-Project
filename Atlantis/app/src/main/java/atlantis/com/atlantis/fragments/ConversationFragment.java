package atlantis.com.atlantis.fragments;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.activities.NotebooksActivity;
import atlantis.com.atlantis.utils.DrawableUtils;
import atlantis.com.atlantis.utils.Events;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.db.DatabaseManager;
import atlantis.com.model.Message;
import atlantis.com.model.MessageContent;
import atlantis.com.model.OTP;

/**
 * A simple {@link Fragment} subclass.
 * to handle interaction events.
 */
public class ConversationFragment extends Fragment {

    private static final int PICK_CONTENT_REQUEST_CODE = 1;

    private static final String ATTACHMENT_MIME_TYPE = "image/*";

    private OnSendMessageListener mListener;
    private MessagesFragment.ConversationHolder mConversationHolder;
    private EditText mMessageEditText;
    private ImageButton mSendButton;

    private static final int WARNING_PERCENTAGE = 10;

    public ConversationFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_conversation, container, false);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.messages_fragment, new MessagesFragment())
                .commit();

        mMessageEditText = (EditText) rootView.findViewById(R.id.message_edit_text);
        mMessageEditText.addTextChangedListener(mMessageTextWatcher);

        mSendButton = (ImageButton) rootView.findViewById(R.id.send_message_button);
        mSendButton.setOnClickListener(mSendButtonClickListener);

        ImageButton attachButton = (ImageButton) rootView.findViewById(R.id.attach_button);
        attachButton.setOnClickListener(mAttachButtonClickListener);

        DrawableUtils.tintDrawablesIfNotLollipop(new Drawable[] {
                getResources().getDrawable(R.drawable.send_icon)
        }, getResources().getColor(R.color.material_deep_teal_500));

        mConversationHolder = (MessagesFragment.ConversationHolder) getActivity();

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mNewMessageEventReceiver,
                new IntentFilter(Events.MESSAGE_CREATED));
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        updateOTPPercentage();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnSendMessageListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnSendMessageListener");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mNewMessageEventReceiver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    // Updates the percentage of OTP available
    private void updateOTPPercentage(){
        DatabaseManager manager = DatabaseManager.getInstance(getActivity());
        try {
            // Find OTP with the least amount available
            List<OTP> otps = manager.getOTPsInConversation(mConversationHolder.getConversation());
            int percentageLeft = 0;
            if (otps.size() > 0) {
                OTP leastFullOTP = otps.get(0);
                for(OTP otp : otps) {
                    if(otp.getPercentageLeft() < leastFullOTP.getPercentageLeft()) {
                        leastFullOTP = otp;
                    }
                }
                percentageLeft = (int) (leastFullOTP.getPercentageLeft() * 100);
            }
            // Update percentage with least amount available
            String formattedPercentage = String.format(getActivity().getResources().getString(R.string.notebook_percentage_left_format),
                    percentageLeft);
            View rootView = getView();
            if(rootView != null) {
                ((TextView) getView().findViewById(R.id.notebook_percentage_left_text))
                        .setText(formattedPercentage);
                ((ProgressBar) getView().findViewById(R.id.notebook_percentage_left_progress_bar))
                        .setProgress(percentageLeft);
                View syncWarning = getView().findViewById(R.id.sync_warning);
                getView().findViewById(R.id.sync_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), NotebooksActivity.class);
                        intent.putExtra(Extras.CONVERSATION_ID,
                                mConversationHolder.getConversation().getId());
                        startActivity(intent);
                    }
                });
                if(percentageLeft == 0) {
                    syncWarning.setBackgroundColor(getResources().getColor(R.color.sync_error));
                    syncWarning.setVisibility(View.VISIBLE);
                } else if(percentageLeft < WARNING_PERCENTAGE) {
                    syncWarning.setBackgroundColor(getResources().getColor(R.color.sync_warning));
                    syncWarning.setVisibility(View.VISIBLE);
                } else {
                    syncWarning.setVisibility(View.GONE);
                }
            }
        } catch (SQLException e) {
            // Eat error
            e.printStackTrace();
        }
    }

    private final ConversationFragment mSelfFragment = this;
    private final BroadcastReceiver mNewMessageEventReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mSelfFragment.updateOTPPercentage();
        }
    };

    private final View.OnClickListener mSendButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(mListener != null) {
                View rootView = getView();
                if(rootView != null) {
                    try {
                        EditText editText = (EditText) getView().findViewById(R.id.message_edit_text);
                        Message message = new Message();
                        MessageContent messageContent = new MessageContent(editText.getText().toString());
                        message.setSerializedContent(messageContent);
                        mListener.onSendMessage(message);
                        editText.setText("");
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(mSelfFragment.getActivity(), R.string.error_sending_message, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };

    private final View.OnClickListener mAttachButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType(ATTACHMENT_MIME_TYPE);

            Intent chooser = Intent.createChooser(intent, "Test");
            getActivity().startActivityForResult(chooser, PICK_CONTENT_REQUEST_CODE);
        }
    };

    private final TextWatcher mMessageTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            updateSendButtonEnabled();
        }
    };

    private void updateSendButtonEnabled() {
        mSendButton.setEnabled(mMessageEditText.getText().length() > 0);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnSendMessageListener {
        void onSendMessage(Message message);
    }

}
