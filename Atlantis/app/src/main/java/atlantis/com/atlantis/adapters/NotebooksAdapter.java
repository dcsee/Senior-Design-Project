package atlantis.com.atlantis.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.sql.SQLException;
import java.util.List;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.activities.NotebookActivity;
import atlantis.com.atlantis.activities.SyncActivity;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.db.DatabaseManager;
import atlantis.com.model.Notebook;

/**
 * Created by ricardo on 4/25/15.
 */
public class NotebooksAdapter extends BasicAdapter {

    private final List<Notebook> mNotebookList;
    private Context mParentContext;

    public NotebooksAdapter(List<Notebook> notebookList, Context parentContext) {
        this.mNotebookList = notebookList;
        this.mParentContext = parentContext;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notebook_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Notebook notebook = mNotebookList.get(position);

        // Set up title text
        String formattedName = String.format(
                holder.getView().getResources().getString(R.string.notebook_name_format),
                notebook.getContact().getDisplay());
        ((TextView)holder.getView().findViewById(R.id.contact_display))
                .setText(formattedName);

        LinearLayout percentageContainer = (LinearLayout) holder.getView().findViewById(R.id.notebook_percentage_left_container);
        TextView notSyncedText = (TextView) holder.getView().findViewById(R.id.notebook_sync_text);

        if(notebook.getIsSynced()) {
            // Show percentage, synced icon, and how much left
            percentageContainer.setVisibility(View.VISIBLE);
            notSyncedText.setVisibility(View.GONE);

            ((ImageView)holder.getView().findViewById(R.id.notebook_sync_status_icon))
                    .setImageDrawable(holder.getView().getResources().getDrawable(R.drawable.ic_tick));

            int percentageLeft = (int) (Math.min(notebook.getSendingOTP().getPercentageLeft(),
                    notebook.getReceivingOTP().getPercentageLeft()) * 100);
            String formattedPercentage = String.format(
                    holder.getView().getResources().getString(R.string.notebook_percentage_left_format),
                    percentageLeft);
            ((TextView) percentageContainer.findViewById(R.id.notebook_percentage_left_text))
                    .setText(formattedPercentage);
            ((ProgressBar) percentageContainer.findViewById(R.id.notebook_percentage_left_progress_bar))
                    .setProgress(percentageLeft);

            // Set up listener to open notebook activity for refreshing
            holder.getView().setOnClickListener(new ViewNotebookOnClickListener(notebook));
        } else {
            // If it's not synced, show not synced text
            percentageContainer.setVisibility(View.GONE);
            notSyncedText.setVisibility(View.VISIBLE);

            ((ImageView)holder.getView().findViewById(R.id.notebook_sync_status_icon))
                    .setImageDrawable(holder.getView().getResources().getDrawable(R.drawable.ic_cached_black_24dp));

            // Set up listener to open syncing activity for notebook
            holder.getView().setOnClickListener(new SyncOnClickListener(notebook));
        }
    }

    public class ViewNotebookOnClickListener implements View.OnClickListener {
        public Notebook mNotebook;

        public ViewNotebookOnClickListener(Notebook notebook) {
            this.mNotebook = notebook;
        }

        @Override
        public void onClick(View v) {
            Intent notebookIntent = new Intent(v.getContext(), NotebookActivity.class);
            notebookIntent.putExtra(Extras.NOTEBOOK_ID,
                    mNotebook.getId());
            v.getContext().startActivity(notebookIntent);
        }
    }

    public class SyncOnClickListener implements View.OnClickListener {
        public Notebook mNotebook;

        public SyncOnClickListener(Notebook notebook) {
            this.mNotebook = notebook;
        }

        @Override
        public void onClick(View v) {
            Intent syncIntent = new Intent(v.getContext(), SyncActivity.class);
            syncIntent.putExtra(Extras.OTP_ID_RECEIVING,
                    mNotebook.getReceivingOTP().getId());
            syncIntent.putExtra(Extras.OTP_ID_SENDING,
                    mNotebook.getSendingOTP().getId());
            v.getContext().startActivity(syncIntent);
        }
    }

    @Override
    public int getItemCount() { return mNotebookList.size(); }

    /**
     * Updates a notebook with an OTP in the adapter with new status information
     * @param otpId The OTP that a notebook owns that should be updated
     * @throws SQLException A problem refreshing the notebook occurred
     */
    public void updateNotebookWithOTPId(int otpId) throws SQLException {
        for(Notebook notebook : mNotebookList) {
            // If the OTP is part of this notebook
            if(notebook.getReceivingOTP().getId() == otpId
                    || notebook.getSendingOTP().getId() == otpId) {
                // Refresh data and notify
                DatabaseManager manager = DatabaseManager.getInstance(mParentContext);
                manager.refreshNotebook(notebook);
                manager.refreshOTP(notebook.getReceivingOTP());
                manager.refreshOTP(notebook.getSendingOTP());
                int index = mNotebookList.indexOf(notebook);
                mNotebookList.set(index, notebook);
                notifyItemChanged(mNotebookList.indexOf(notebook));
            }
        }
    }

    /**
     * Adds a new notebook to the adapter
     * @param notebook The notebook to add
     */
    public void addNotebook(Notebook notebook) {
        mNotebookList.add(notebook);
        notifyDataSetChanged();
    }

    /**
     * Removes a notebook with the id in the adapter
     * @param notebookId The notebook id to remove
     */
    public void removeNotebookWithId(int notebookId) {
        for(Notebook notebook : mNotebookList) {
            if(notebook.getId() == notebookId) {
                int index = mNotebookList.indexOf(notebook);
                mNotebookList.remove(index);
                notifyItemRemoved(index);
            }
        }
    }
}
