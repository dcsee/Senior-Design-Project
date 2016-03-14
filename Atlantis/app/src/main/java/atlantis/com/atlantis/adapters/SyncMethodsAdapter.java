package atlantis.com.atlantis.adapters;

import android.app.Activity;
import android.content.Intent;
import android.os.Debug;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.activities.FakeSyncActivity;
import atlantis.com.atlantis.communications.nearcommunications.bluetooth.BluetoothActivity;
import atlantis.com.atlantis.communications.nearcommunications.usb.UsbActivity;
import atlantis.com.atlantis.utils.DebugFlags;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.model.OTP;

/**
 * Adapter for showing different methods of transferring the OTP
 * Created by ricardo on 2/22/15.
 */
public class SyncMethodsAdapter extends BasicAdapter {

    private final List<SyncMethod> mMethodList;
    private final NotebookHolder mNotebookHolder;
    private final Activity mParentActivity;

    public interface NotebookHolder {
        OTP getSendingOTP();
        OTP getReceivingOTP();
    }

    public SyncMethodsAdapter(NotebookHolder notebookHolder, FragmentActivity parentActivity) {
        this.mNotebookHolder = notebookHolder;
        this.mParentActivity = parentActivity;

        mMethodList = new ArrayList<>();
        if(DebugFlags.SHOW_FAKE_SYNC) {
            mMethodList.add(new SyncMethod(
                    parentActivity.getString(R.string.sync_method_dummy_title),
                    parentActivity.getString(R.string.sync_method_dummy_description),
                    R.drawable.bluetooth_icon,
                    FakeSyncActivity.class));
        }
        if(DebugFlags.SHOW_SD_SYNC) {
            mMethodList.add(new SyncMethod(
                    parentActivity.getString(R.string.sync_method_sd_card_title),
                    parentActivity.getString(R.string.sync_method_sd_card_description),
                    R.drawable.sd_card_icon,
                    FakeSyncActivity.class));
        }
        mMethodList.add(new SyncMethod(
                parentActivity.getString(R.string.sync_method_bluetooth_title),
                parentActivity.getString(R.string.sync_method_bluetooth_description),
                R.drawable.bluetooth_icon,
                BluetoothActivity.class));
        mMethodList.add(new SyncMethod(
                parentActivity.getString(R.string.sync_method_usb_title),
                parentActivity.getString(R.string.sync_method_usb_description),
                R.drawable.usb_icon,
                UsbActivity.class));
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.sync_method_view, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final SyncMethod syncMethod = mMethodList.get(position);
        ((TextView)holder.getView().findViewById(R.id.sync_method_name))
                .setText(syncMethod.getName());
        ((TextView)holder.getView().findViewById(R.id.sync_method_description))
                .setText(syncMethod.getDescription());
        ((ImageView)holder.getView().findViewById(R.id.sync_method_icon))
                .setImageDrawable(holder.getView().getResources().getDrawable(
                        syncMethod.getmIconId()));
        holder.getView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mParentActivity, syncMethod.getSyncClass());
                intent.putExtra(Extras.OTP_ID_RECEIVING,
                        mNotebookHolder.getReceivingOTP().getId());
                intent.putExtra(Extras.OTP_ID_SENDING,
                        mNotebookHolder.getSendingOTP().getId());
                mParentActivity.startActivity(intent);
                mParentActivity.finish();
            }
        });
    }

    @Override
    public int getItemCount() {
        return mMethodList.size();
    }

    private class SyncMethod {
        private final String mName;
        private final String mDescription;
        private final int mIconId;

        private final Class<? extends Activity> mSyncClass;

        private SyncMethod(String mName, String mDescription, int mIconId, Class<? extends Activity> mSyncClass) {
            this.mName = mName;
            this.mDescription = mDescription;
            this.mIconId = mIconId;
            this.mSyncClass = mSyncClass;
        }

        public Class<? extends Activity> getSyncClass() {
            return mSyncClass;
        }

        public String getName() {
            return mName;
        }

        public int getmIconId() {
            return mIconId;
        }

        public String getDescription() {
            return mDescription;
        }
    }
}
