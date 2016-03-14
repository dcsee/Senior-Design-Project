package atlantis.com.atlantis.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.fragments.interfaces.MessageContentHolder;

/**
 * Fragment for showing the text in a message
 */
public class TextMessageFragment extends Fragment {

    public TextMessageFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_text_message, container, false);

        MessageContentHolder messageContentHolder = (MessageContentHolder) getActivity();

        TextView messageText = ((TextView)rootView.findViewById(R.id.message_content_text));
        try {
            messageText.setText(messageContentHolder.getMessageContent()
                    .getStringContent());
        } catch (IOException e) {
            e.printStackTrace();

            messageText.setText(rootView.getResources().getString(R.string.error_loading_text));
        }

        return rootView;
    }

}
