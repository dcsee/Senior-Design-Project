package atlantis.com.atlantis.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import atlantis.com.atlantis.R;

/**
 * Fragment for showing creating notebook progress
 */
public class CreatingNotebookFragment extends Fragment {

    public CreatingNotebookFragment() {
        // Required empty public constructor
    }

    public interface OnCancelNotebookListener {
        void onCancelConversation();
    }

    OnCancelNotebookListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try{
            mListener = (OnCancelNotebookListener)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + "must implement OnCancelNotebookListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_creating_notebook, container, false);
        Button cancelButton = (Button) rootView.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(mCancelButtonListener);
        // Inflate the layout for this fragment
        return rootView;
    }

    private CreatingNotebookFragment mSelfFragment = this;

    private View.OnClickListener mCancelButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mListener.onCancelConversation();
            Toast.makeText(mSelfFragment.getActivity(), R.string.cancelled_text, Toast.LENGTH_SHORT).show();
        }
    };
}
