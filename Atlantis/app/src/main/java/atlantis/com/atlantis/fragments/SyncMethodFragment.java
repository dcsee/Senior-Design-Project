package atlantis.com.atlantis.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.adapters.SyncMethodsAdapter;
import atlantis.com.atlantis.decorations.DividerItemDecoration;

/**
 * A simple {@link Fragment} subclass.
 * to handle interaction events.
 */
public class SyncMethodFragment extends Fragment {

    public SyncMethodFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_sync_method, container, false);

        RecyclerView recyclerView = (RecyclerView) rootView.findViewById(R.id.sync_methods_recycle_view);
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST));
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(new SyncMethodsAdapter((SyncMethodsAdapter.NotebookHolder)getActivity(), getActivity()));

        return rootView;
    }

}
