package atlantis.com.atlantis.communications.nearcommunications.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import atlantis.com.atlantis.communications.nearcommunications.NearCommunicationMessage;
import atlantis.com.atlantis.communications.nearcommunications.SyncManager;

/**
 * Created by jvronsky on 5/29/15.
 * Handles HOST usb communications.
 */
public class UsbHostHandler extends UsbHandler{

    private static final int GENERAL_WAIT_TIMEOUT = 1000;
    private static final int USB_TIMEOUT = 300;
    private static final int ATTEMPTS_TO_GET_ACCESSORY_COUNT = 5;
    private UsbDeviceConnection mUsbDeviceConnection;
    private UsbInterface mUsbInterface;
    private UsbDevice mUsbDevice;
    private boolean mIsListening = false;
    private UsbEndpoint mEndpointIn = null;
    private UsbEndpoint mEndpointOut = null;
    private List<byte[]> mOutgoingMessages;

    public UsbHostHandler(Context context) {
        super(context);
        mOutgoingMessages = new ArrayList<>(1);
    }

    @Override
    public void close() {
        mIsListening = false;
        if(mUsbDeviceConnection != null && mUsbInterface != null) {
            mUsbDeviceConnection.releaseInterface(mUsbInterface);
            mUsbDeviceConnection.close();
            mUsbInterface = null;
            mUsbDeviceConnection = null;
        }
        this.interrupt();
    }

    @Override
    public void send(byte[] message) {
        mOutgoingMessages.add(message);
    }

    @Override
    public void run() {
        try {
            boolean foundAccessory = false;
            PendingIntent pendingIntent =
                    PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
            while (!foundAccessory) {
                HashMap<String, UsbDevice> deviceHashMap = mUsbManager.getDeviceList();
                // Wait if no devices were attached.
                while (deviceHashMap.values().size() == 0) {
                    deviceHashMap = mUsbManager.getDeviceList();
                    sleepFor(GENERAL_WAIT_TIMEOUT);
                }
                mUsbDevice = (UsbDevice) deviceHashMap.values().toArray()[0];
                mUsbManager.requestPermission(mUsbDevice, pendingIntent);
                // Wait for permission to accessory.
                while (!mUsbManager.hasPermission(mUsbDevice)) {
                    sleepFor(GENERAL_WAIT_TIMEOUT);
                }
                mUsbDeviceConnection = mUsbManager.openDevice(mUsbDevice);
                UsbCommunicator.initAccessory(mUsbDeviceConnection);
                mUsbDeviceConnection.close();
                // Wait for accessory to appear.
                int tries = 0;
                while (tries < ATTEMPTS_TO_GET_ACCESSORY_COUNT) {
                    deviceHashMap = mUsbManager.getDeviceList();
                    // Search among devices for an accessory.
                    for (UsbDevice device : deviceHashMap.values()) {
                        if (UsbCommunicator.isUsbAccessory(device)) {
                            foundAccessory = true;
                            mUsbDevice = device;
                            break;
                        }
                    }
                    if (foundAccessory) {
                        break;
                    }
                    sleepFor(GENERAL_WAIT_TIMEOUT);
                }
            }

            pendingIntent =
                    PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
            mUsbManager.requestPermission(mUsbDevice, pendingIntent);
            while (!mUsbManager.hasPermission(mUsbDevice)) {
                sleepFor(GENERAL_WAIT_TIMEOUT);
            }
            mUsbDeviceConnection = mUsbManager.openDevice(mUsbDevice);
            mUsbInterface = mUsbDevice.getInterface(0);
            boolean claimed = mUsbDeviceConnection.claimInterface(mUsbInterface, true);
            if (!claimed) {
                throw new UsbSetupFailed("Could not claim interface");
            }
            // Get endpoints
            for (int i = 0; i < mUsbInterface.getEndpointCount(); i++) {
                final UsbEndpoint endpoint = mUsbInterface.getEndpoint(i);
                if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
                    mEndpointIn = endpoint;
                }
                if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
                    mEndpointOut = endpoint;
                }
            }
            if (mEndpointIn == null || mEndpointOut == null) {
                throw new UsbSetupFailed("Could not find endpoints");
            }
            mIsListening = true;
            listen();
        } catch (UsbSetupFailed usbSetupFailed) {
            usbSetupFailed.printStackTrace();

        }
    }

    private void listen() {
        byte[] buffer = new byte[BUFFER_SIZE];
        Intent intent = new Intent();
        intent.setAction(UsbHostActivity.ACTION_HOST_IS_READY);
        mContext.sendBroadcast(intent);
        while(mIsListening) {
            int bytesTransferred = mUsbDeviceConnection.bulkTransfer(
                    mEndpointIn, buffer, buffer.length, USB_TIMEOUT);
            if(bytesTransferred > 0) {
                NearCommunicationMessage.MessageHeader messageHeader =
                        new NearCommunicationMessage().parseHeader(Arrays.copyOfRange(buffer, 0,
                                NearCommunicationMessage.HEADER_LENGTH));
                if(!messageHeader.isValidHeader()) {
                    return;
                }
                processMessage(messageHeader,
                        Arrays.copyOfRange(buffer,
                                NearCommunicationMessage.HEADER_LENGTH, bytesTransferred));
            }
            synchronized (mOutgoingMessages) {
                if(mOutgoingMessages.size() > 0) {
                    byte[] messageToSend = mOutgoingMessages.get(0);
                    mUsbDeviceConnection.bulkTransfer(
                            mEndpointOut, messageToSend, messageToSend.length, USB_TIMEOUT);
                    mOutgoingMessages.remove(0);
                }
            }
        }
    }
}
