package atlantis.com.atlantis.communications.nearcommunications.usb;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import atlantis.com.atlantis.communications.nearcommunications.Device;
import atlantis.com.atlantis.communications.nearcommunications.NearCommunicationMethod;

/**
 * Created by jvronsky on 5/19/15.
 * Communicator to handle USB communications.
 */
public class UsbCommunicator extends NearCommunicationMethod {

    private static final String TAG = "UsbCommunicator";
    private static final String TYPE = "USB";
    private static final int MAX_PIECE_SIZE = 16 * 1024;

    /*
    This values are from the android usb protocol documentation
    for reference:
    https://source.android.com/accessories/aoa2.html
    They are the values that include an accessory.
    */
    private static final int ACCESSORY_DEVICE_TYPE_1 = 0x2d00;
    private static final int ACCESSORY_DEVICE_TYPE_2 = 0x2d01;
    private static final int ACCESSORY_DEVICE_TYPE_3 = 0x2d04;
    private static final int ACCESSORY_DEVICE_TYPE_4 = 0x2d05;
    // Accessory Initialization values.
    public static final String APP_NAME = "Atlantis";
    public static final String MODEL = "UsbCommunicator";
    public static final String DESCRIPTION = "Transfer OTP over USB";
    public static final String URI = "-";
    public static final String SERIAL = "42";
    public static final String VERSION = "1.0";
    // Usb Control  Transfer values.
    private static final int USB_TYPE_VENDOR = 0x40;
    private static final int REQUEST_ID_STRING_SEND = 52;
    private static final int REQUEST_ID_INIT_ACCESSORY = 53;
    private static final int TIMEOUT = 1000;

    private UsbManager mUsbManager;
    private UsbHandler mUsbHandler;

    public UsbCommunicator(Context context, UsbHandler usbHandler) throws UsbIsNotSupportedException {
        super(context, null);
        this.mUsbHandler = usbHandler;
        mUsbManager = (UsbManager) mParentActivity.getSystemService(Context.USB_SERVICE);
        mUsbHandler.setUsbManager(mUsbManager);
        mUsbHandler.setUsbCommunicator(this);
    }

    @Override
    public void close() {
        mUsbHandler.close();
    }

    @Override
    public void scan() {

    }

    @Override
    public void send(byte[] message) {
        mUsbHandler.send(message);
    }

    @Override
    public void connect(Device device) {

    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void restoreSettings() {

    }

    @Override
    public int maxDataPieceSize() {
        return MAX_PIECE_SIZE;
    }

    public void startListening() {
        mUsbHandler.start();
    }

    public void routeMessage(int statusCode, byte[] body) {
        mSyncManager.sendMessageToTarget(statusCode, body);
    }

    public static boolean isUsbAccessory(UsbDevice device) {
        return (device.getProductId() == ACCESSORY_DEVICE_TYPE_1)
                || (device.getProductId() == ACCESSORY_DEVICE_TYPE_2)
                || (device.getProductId() == ACCESSORY_DEVICE_TYPE_3)
                || (device.getProductId() == ACCESSORY_DEVICE_TYPE_4);
    }

    public static boolean initAccessory(UsbDeviceConnection connection) {
        initStringControlTransfer(connection, 0, APP_NAME );
        initStringControlTransfer(connection, 1, MODEL);
        initStringControlTransfer(connection, 2, DESCRIPTION);
        initStringControlTransfer(connection, 3, VERSION);
        initStringControlTransfer(connection, 4, URI);
        initStringControlTransfer(connection, 5, SERIAL);
        connection.controlTransfer(USB_TYPE_VENDOR, REQUEST_ID_INIT_ACCESSORY, 0, 0, new byte[]{}, 0, TIMEOUT);
        connection.close();
        return true;
    }

    private static void initStringControlTransfer(UsbDeviceConnection connection, int index, String s) {
        connection.controlTransfer(USB_TYPE_VENDOR, REQUEST_ID_STRING_SEND, 0, index, s.getBytes(), s.length(), TIMEOUT);
    }
}
