package atlantis.com.atlantis.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.IOException;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.fragments.interfaces.MessageContentHolder;

/**
 * Fragment for showing the image in a message
 */
public class ImageMessageFragment extends Fragment {

    public ImageMessageFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_image_message, container, false);

        MessageContentHolder messageContentHolder = (MessageContentHolder) getActivity();

        try {
            ((ImageView)rootView.findViewById(R.id.message_content_image))
                    .setImageBitmap(messageContentHolder.getMessageContent()
                            .getImageContent());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return rootView;
    }

}
