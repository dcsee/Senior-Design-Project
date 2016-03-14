package atlantis.com.atlantis.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import atlantis.com.atlantis.R;


/**
 * A simple {@link Fragment} subclass.
 * to handle interaction events.
 */
public class FakeSyncingFragment extends Fragment {

    public FakeSyncingFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_fake_syncing, container, false);
    }

}
