package atlantis.com.atlantis.communications.nearcommunications;

import android.app.Activity;
import android.content.Context;
import android.widget.ArrayAdapter;

/**
 * Created by jvronsky on 3/3/15.
 */
public abstract class NearCommunicationMethod {

    protected SyncManager mSyncManager;
    protected ArrayAdapter<Device> mArrayAdapter;
    protected final Activity mParentActivity;

    protected NearCommunicationMethod(Context context, ArrayAdapter<Device> arrayAdapter) {
        this.mArrayAdapter = arrayAdapter;
        this.mParentActivity = (Activity) context;
      }

    /**
     * Close connection.
     */
    public abstract void close();

    /**
     * Scan for other devices.
     */
    public abstract void scan();

    /**
     * Send message through the medium.
     * @param message to send over
     */
    public abstract void send(byte[] message);

    /**
     * Connect to other device.
     * @param device to connect to
     */
    public abstract void connect(Device device);

    /**
     * Return the type of the method ex. Bluetooth
     * @return
     */
    public abstract String getType();

    /**
     * Use to restore setting to what they were before method was init.
     */
    public abstract void restoreSettings();

    /**
     * @return the largest number of bytes in a data piece message.
     */
    protected abstract int maxDataPieceSize();

    public void setSyncManager(SyncManager syncManager) {
        this.mSyncManager = syncManager;
    }
}
