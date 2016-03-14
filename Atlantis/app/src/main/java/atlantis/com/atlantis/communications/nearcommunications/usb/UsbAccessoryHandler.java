package atlantis.com.atlantis.communications.nearcommunications.usb;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.os.ParcelFileDescriptor;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import atlantis.com.atlantis.communications.nearcommunications.NearCommunicationMessage;

/**
 * Created by jvronsky on 5/29/15.
 * USB accessory handler to read data.
 */
public class UsbAccessoryHandler extends UsbHandler{

    private static final int GENERAL_WAIT_TIMEOUT = 1000;
    private static final int RESTART_TIMEOUT = 8000;
    private static final int NEW_MESSAGE_ARRIVE_TIMEOUT = 8000;
    private static final int POOL_COUNT = 1;

    private boolean mIsListening = false;
    FileOutputStream mOutput;

    private ExecutorService mExecutor = Executors.newFixedThreadPool(POOL_COUNT);

    public UsbAccessoryHandler(Context context) {
        super(context);
    }

    @Override
    public void run() {
        UsbAccessory[] accessories;
        ParcelFileDescriptor temp = null;
        try {
            while (true) {
                // Wait for accessory to be detected.
                while (true) {
                    accessories = mUsbManager.getAccessoryList();
                    if (accessories != null) {
                        if (accessories.length > 0) {
                            // If found an accessory quit looking for it.
                            break;
                        }
                    }
                    sleepFor(GENERAL_WAIT_TIMEOUT);
                }
                // Assign found accessory.
                UsbAccessory usbAccessory = accessories[0];
                sleepFor(GENERAL_WAIT_TIMEOUT);
                PendingIntent pendingIntent =
                        PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
                mUsbManager.requestPermission(usbAccessory, pendingIntent);
                while (!mUsbManager.hasPermission(usbAccessory)) {
                    sleepFor(GENERAL_WAIT_TIMEOUT);
                }
                ParcelFileDescriptor parcelFileDescriptor = mUsbManager.openAccessory(usbAccessory);
                if(parcelFileDescriptor == null) {
                    parcelFileDescriptor = temp;
                } else {
                    temp = parcelFileDescriptor;
                }
                if(parcelFileDescriptor != null) {
                    FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                    FileInputStream fileInputStream = new FileInputStream(fileDescriptor);
                    FileOutputStream fileOutputStream = new FileOutputStream(fileDescriptor);
                    mIsListening = true;
                    mOutput = fileOutputStream;
                    listen(fileInputStream);
                    fileOutputStream.flush();
                    fileInputStream.close();
                    fileOutputStream.close();
                    mOutput = null;
                    parcelFileDescriptor.close();
                } else {
                    throw new UsbSetupFailed("Could not get file descriptor");
                }
                sleepFor(RESTART_TIMEOUT);
            }
        } catch (IOException | UsbSetupFailed e) {
            e.printStackTrace();
        }
    }

    private void listen(FileInputStream inChannel) {
        byte[] incomingMessage;
        // Listening tp new data
        while(mIsListening) {
            try {
                ReadTask readTask = new ReadTask(inChannel);
                Future<AccessoryReadResult> future = mExecutor.submit(readTask);
                AccessoryReadResult readResult =
                        future.get(NEW_MESSAGE_ARRIVE_TIMEOUT, TimeUnit.MILLISECONDS);
                inChannel = readTask.getFileInputStream();
                incomingMessage = readResult.getData();
                if (readResult.getLength() > 0) {
                    NearCommunicationMessage.MessageHeader messageHeader =
                            new NearCommunicationMessage().parseHeader(
                                    Arrays.copyOfRange(
                                            incomingMessage, 0, NearCommunicationMessage.HEADER_LENGTH));
                    processMessage(messageHeader, Arrays.copyOfRange(incomingMessage,
                            NearCommunicationMessage.HEADER_LENGTH, readResult.getLength()));
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
                stopListening();
            }
        }
    }

    private void stopListening() {
        mIsListening = false;
        mOutput = null;
    }

    @Override
    public void close() {
        this.interrupt();
    }

    @Override
    public void send(byte[] message) {
        if(mIsListening) {
            try {
                mOutput.write(message);
            } catch (IOException e) {
                e.printStackTrace();
                mIsListening = false;
            }
        }
    }

    private class AccessoryReadResult {

        private int mLength;
        private byte[] mData;

        public AccessoryReadResult(int length, byte[] buffer) {
            this.mLength = length;
            this.mData = buffer;
        }

        public int getLength() {
            return mLength;
        }

        public byte[] getData() {
            return mData;
        }
    }

    private class ReadTask implements Callable<AccessoryReadResult> {

        FileInputStream mFileInputStream;

        public ReadTask(FileInputStream fileInputStream) {
            this.mFileInputStream = fileInputStream;
        }

        @Override
        public AccessoryReadResult call() throws Exception {
            byte[] buffer = new byte[BUFFER_SIZE];
            int length = mFileInputStream.read(buffer);
            return new AccessoryReadResult(length, Arrays.copyOfRange(buffer, 0, length));
        }

        public FileInputStream getFileInputStream() {
            return mFileInputStream;
        }
    }
}
