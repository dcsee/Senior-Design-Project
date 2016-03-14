package atlantis.com.atlantis.communications.nearcommunications.bluetooth;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.util.Pair;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.UUID;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.communications.nearcommunications.Device;
import atlantis.com.atlantis.communications.nearcommunications.NearCommunicationMessage;
import atlantis.com.atlantis.communications.nearcommunications.NearCommunicationMethod;

/**
 * Created by jvronsky on 3/4/15.
 */
public class BluetoothCommunicator extends NearCommunicationMethod {

    private static final String TAG = "BLUETOOTH_COMMUNICATOR";

    private static final String DIALOG_TITLE = "Establishing Bluetooth Connection";

    public static final String CONNECTED_TO_DEVICE = "atlantis.connected_to_device";

    private static final String TYPE = "Bluetooth";

    public static final int REQUEST_ENABLE_BT = 1;

    // Maximum size of a chunk over bluetooth.
    private static final int MAX_SIZE_OF_MESSAGE = 1024*50;

    // UUID used for bluetooth communication.
    private static final String CUSTOM_UUID = "6022a9c0-4f26-fce3-b5f7-0621ffc462a5";

    // How long will a device be discoverable.
    private static final int DISCOVERABLE_TIME = 0;

    private final BluetoothAdapter bluetoothAdapter;
    private final BroadcastReceiver deviceDetector;
    private boolean wasConnectedToBluetoothBefore;
    private boolean alreadyConnectedToDevice;
    private boolean mDiscoverReceiverRegistered;
    private HashMap<String, BluetoothDevice> foundDevices;
    // Thread to wait for hosting.
    private AcceptThread hostThread;
    // Thread to connect to device.
    private ConnectThread connectThread;
    // Thread that runs while connected.
    private ConnectedThread connectedThread;
    // Establish connection dialog is UI to show the progress of connecting.
    private final ProgressDialog establishingConnectionDialog;

    public BluetoothCommunicator(Context context, final ArrayAdapter<Device> arrayAdapter) {
        super(context, arrayAdapter);
        alreadyConnectedToDevice = false;
        mDiscoverReceiverRegistered = false;
        establishingConnectionDialog = new ProgressDialog(context);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) {
           deviceDetector = null;
            Toast.makeText(context, R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
        } else {
           foundDevices = new HashMap<>();
           deviceDetector = new BroadcastReceiver() {
               @Override
               public void onReceive(Context context, Intent intent) {
                   String action = intent.getAction();
                   if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                       BluetoothDevice device =
                               intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                       foundDevices.put(device.getAddress(), device);
                       arrayAdapter.add(new Device(device.getName(), device.getAddress()));
                       arrayAdapter.notifyDataSetChanged();
                   }
               }
           };
        }
        turnOn();
    }

    @Override
    public void close() {
        if(connectedThread != null) {
            connectedThread.close();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void scan() {
        if(bluetoothAdapter.isDiscovering()) {
            //noinspection UnnecessaryReturnStatement
            return;
        } else {
            mArrayAdapter.clear();
            bluetoothAdapter.startDiscovery();
            mParentActivity.registerReceiver(deviceDetector,
                    new IntentFilter(BluetoothDevice.ACTION_FOUND));
            mDiscoverReceiverRegistered = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(byte[] message) {
        connectedThread.write(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void connect(Device device) {
        if(!alreadyConnectedToDevice) {
            BluetoothDevice bluetoothDevice = foundDevices.get(device.getDeviceAddress());
            connectThread = new ConnectThread(bluetoothDevice);
            connectThread.start();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreSettings() {
        if(!wasConnectedToBluetoothBefore) {
            turnOff();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int maxDataPieceSize() {
        return MAX_SIZE_OF_MESSAGE;
    }

    public void startHost() {
        hostThread = new AcceptThread();
        hostThread.start();
    }

    public void destroy() {
        if(connectedThread != null && connectedThread.isAlive()) {
            connectedThread.cancel();
        }
        if(connectThread != null && connectThread.isAlive()) {
            connectThread.cancel();
        }
        if(hostThread != null && hostThread.isAlive()) {
            hostThread.cancel();
        }
        if(mDiscoverReceiverRegistered) {
            mParentActivity.unregisterReceiver(deviceDetector);
        }
        restoreSettings();
    }

    public void makeDiscoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(
                BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_TIME);
        mParentActivity.startActivity(discoverableIntent);
    }

    /**
     * The thread that runs while the phone is waiting for a connection
     * request.
     */
    private class AcceptThread extends Thread{

        // Server socket used to host a connection.
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("Atlantis",
                        UUID.fromString(CUSTOM_UUID));
            } catch (IOException e) {
                e.printStackTrace();
            }
            serverSocket = tmp;
        }

        @Override
        public void run() {
            BluetoothSocket socket = null;
            while(true) {
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    //TODO: handle error
                }
                if (socket != null) {
                    manageConnection(socket);
                    try {
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        //TODO: handle error
                    }
                    break;
                }
            }
        }

        public void cancel() {
            closeConnection(serverSocket);
        }
    }

    /**
     * Thread to send the connect request from here it moves to connected thread.
     */
    private class ConnectThread extends Thread{
        private final BluetoothSocket bluetoothSocket;

        public ConnectThread(BluetoothDevice bluetoothDevice) {
            BluetoothSocket tmp = null;

            try {
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(
                        UUID.fromString(CUSTOM_UUID));
            } catch (IOException e) {
                e.printStackTrace();
                //TODO: Handle error correctly.
            }
            bluetoothSocket = tmp;
        }

        public void run() {
            startConnectionDialog();
            bluetoothAdapter.cancelDiscovery();

            try {
                bluetoothSocket.connect();
                manageConnection(bluetoothSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            closeConnection(bluetoothSocket);
            if(bluetoothSocket != null) {
                try {
                    bluetoothSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void startConnectionDialog() {
            establishingConnectionDialog.setTitle(DIALOG_TITLE);
            establishingConnectionDialog.setMessage("Connecting to: "
                    + bluetoothSocket.getRemoteDevice().getName());
            establishingConnectionDialog.setProgress(ProgressDialog.STYLE_HORIZONTAL);
            establishingConnectionDialog.setIndeterminate(true);
            establishingConnectionDialog.setCancelable(true);
            mParentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    establishingConnectionDialog.show();
                }
            });

        }
    }

    /**
     * This thread is running when connection is established,
     * It handles the receiving of messages correctly and passes the
     * work to do with them to the communicator class.
     */
    private class ConnectedThread extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final InputStream input;
        private final OutputStream output;
        // Number of bytes from header processed.
        private int headerBytesProcessed;
        // Header buffer used to read from input stream.
        private final byte[] headerBuffer;
        // Header read in.
        private final byte[] header;
        // Last message sent.
        private byte[] lastMessageSent;
        // Body buffer.
        private final byte[] bodyBuffer;
        // Keep the thread alive.
        private boolean keepAlive;
        // Input Message size.
        private int messageSize;
        // Incoming message.
        private ByteBuffer incomingMessage;

        public ConnectedThread(BluetoothSocket socket) {
            this.bluetoothSocket = socket;
            Pair<InputStream, OutputStream> inputOutputPair = null;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            headerBytesProcessed = 0;
            keepAlive = true;
            try {
                inputOutputPair = getInputOutputStream(socket);
            } catch (IOException e) {
                e.printStackTrace();
                //TODO: Handle error.
            }
            input = inputOutputPair.first;
            output = inputOutputPair.second;
            bodyBuffer = new byte[10*1024];
            headerBuffer = new byte[1];
            header = new byte[NearCommunicationMessage.HEADER_LENGTH];
        }

        /**
         * Connection thread, alive as long as the message is.
         */
        public void run() {
            Intent intent = new Intent();
            intent.setAction(CONNECTED_TO_DEVICE);
            mParentActivity.sendBroadcast(intent);
            alreadyConnectedToDevice = true;
            stopConnectionDialog();
            while(keepAlive) {
                try {
                    readMessage();
                } catch (IOException e) {
                    e.printStackTrace();
                    // TODO: Handle error.
                }
            }
            hostThread.cancel();
        }

        /**
         * Write message to output of bluetooth.
         * @param message
         */
        public void write(byte[] message) {
            this.lastMessageSent = message;
            try {
                Log.d(TAG, "Sending: " + message[1]);
                output.write(message);
            } catch (IOException e) {
                e.printStackTrace();
                // TODO: handle message.
            }
        }

        /**
         * Read message in.
         * @throws IOException
         */
        private void readMessage() throws IOException {
            int code = -1;
            if(isReadingHeader()) {
                input.read(headerBuffer);
                header[headerBytesProcessed++] = headerBuffer[0];
            } else if(isEntireHeaderRead()) {
                NearCommunicationMessage.MessageHeader messageHeader =
                        new NearCommunicationMessage().parseHeader(header);
                if(messageHeader.isValidHeader()) {
                    code = messageHeader.getStatusCode();
                    messageSize = messageHeader.getDataLength();
                }
                switch (code) {
                    case NearCommunicationMessage.RESEND: resendLastMessage(); break;
                    default: processMessage(code, messageHeader.getDataChecksum());
                }
            }
        }

        /**
         * Process the message from user.
         * @param code of the message received
         * @param checksum of the message received
         * @throws IOException
         */
        private void processMessage(int code, byte[] checksum) throws IOException {
            int bytesIn = 0;
            int totalIn = 0;
            incomingMessage = ByteBuffer.allocate(MAX_SIZE_OF_MESSAGE);
            Log.d(TAG, "Received Code: " + code);
            if(NearCommunicationMessage.isEmptyMessageCode(code)) {
                mSyncManager.sendMessageToTarget(code, null);
                messageSize = 0;
                headerBytesProcessed = 0;
                return;
            }
            //Log.d(TAG, "Availble: " + input.available());
            bytesIn = input.read(bodyBuffer, 0, 10*1024);
            while(bytesIn != -1) {
                //Log.d(TAG, "bytesIn: " + bytesIn);
                incomingMessage.put(bodyBuffer, 0, bytesIn);
                totalIn += bytesIn;
                if(messageSize == totalIn) {
                    break;
                }
                bytesIn = input.read(bodyBuffer);
            }
            byte[] finalMessage = new byte[incomingMessage.position()];
            incomingMessage.rewind();
            incomingMessage.get(finalMessage);
            if(NearCommunicationMessage.checkDataIntegrity(finalMessage, checksum)) {
                Log.d(TAG, "Sending to data");
                mSyncManager.sendMessageToTarget(code, finalMessage);
                //sendToCommunicator(code, finalMessage);
            } else {
                Log.d(TAG, "Checksum not matched");
                // If message was not received correctly ask for resend
                requestLastMessage();
            }
            messageSize = 0;
            headerBytesProcessed = 0;
            incomingMessage.clear();
        }

        private void requestLastMessage() {
            write(NearCommunicationMessage.buildResendMessage());
        }

        private void resendLastMessage() {
            write(lastMessageSent);
        }

        private boolean isEntireHeaderRead() {
            return headerBytesProcessed == NearCommunicationMessage.HEADER_LENGTH;
        }

        private boolean isReadingHeader() {
            return headerBytesProcessed < NearCommunicationMessage.HEADER_LENGTH;
        }

        private void stopConnectionDialog() {
            mParentActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    establishingConnectionDialog.cancel();
                }
            });
        }

        public void cancel() {
            keepAlive = false;
            close();
        }

        public void clearBuffer() {
            incomingMessage.clear();
        }

        public void close() {
            try {
                keepAlive = false;
                input.close();
                output.close();
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected Pair<InputStream, OutputStream> getInputOutputStream(BluetoothSocket socket)
            throws IOException {
        InputStream tmpIn;
        OutputStream tmpOut;
        if(socket != null) {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } else {
            throw new IOException("Socket was never initialized");
        }
        return new Pair<>(tmpIn, tmpOut);
    }

    private void manageConnection(BluetoothSocket socket) {
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();
    }

    private void closeConnection(BluetoothSocket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeConnection(BluetoothServerSocket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Turns off bluetooth.
     */
    private void turnOff() {
        if(bluetoothAdapter != null) {
            bluetoothAdapter.disable();
        }
    }

    /**
     * Turn on bluetooth.
     */
    private void turnOn() {
        if(!bluetoothAdapter.isEnabled()) {
            Intent turnOnBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            mParentActivity.startActivityForResult(turnOnBluetooth, REQUEST_ENABLE_BT);
        } else {
            wasConnectedToBluetoothBefore = true;
            startHost();
        }
    }

    public boolean isWasConnectedToBluetoothBefore() {
        return wasConnectedToBluetoothBefore;
    }

}
