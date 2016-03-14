package atlantis.com.atlantis.communications.nearcommunications.usb;

import android.content.Context;
import android.hardware.usb.UsbManager;

import atlantis.com.atlantis.communications.nearcommunications.NearCommunicationMessage;

/**
 * Created by jvronsky on 5/29/15.
 * Usb Handler super class
 */
public abstract class UsbHandler extends Thread {

    protected static final String ACTION_USB_PERMISSION = "USB_PERMISSION";
    protected static final int BUFFER_SIZE = 16 * 1024;

    protected final Context mContext;
    protected UsbManager mUsbManager;
    private UsbCommunicator mUsbCommunicator;

    public UsbHandler(Context context) {
        this.mContext = context;
    }

    /**
     * Close uses to close the USB activity.
     */
    public abstract void close();

    public abstract void send(byte[] message);

    public void setUsbManager(UsbManager usbManager) {
        this.mUsbManager = usbManager;
    }

    public void setUsbCommunicator(UsbCommunicator usbCommunicator) {
        this.mUsbCommunicator = usbCommunicator;
    }

    protected void processMessage(NearCommunicationMessage.MessageHeader messageHeader, byte[] body) {
        if(messageHeader.isValidHeader()) {
            if(NearCommunicationMessage.isEmptyMessageCode(messageHeader.getStatusCode())) {
                routeMessage(messageHeader.getStatusCode(), null);
                return;
            } else if(NearCommunicationMessage.checkDataIntegrity(body,
                    messageHeader.getDataChecksum())) {
                routeMessage(messageHeader.getStatusCode(), body);
                return;
            }
        }
    }

    private void routeMessage(int statusCode, byte[] body) {
        mUsbCommunicator.routeMessage(statusCode, body);
    }

    protected void sleepFor(int timeout) {
        try {
            Thread.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
