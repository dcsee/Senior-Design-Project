package atlantis.com.server.socket;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.util.Base64;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.j256.ormlite.dao.ForeignCollection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import atlantis.com.atlantis.encryption.PINCreationFailedException;
import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;
import atlantis.com.atlantis.utils.Events;
import atlantis.com.atlantis.utils.Extras;
import atlantis.com.atlantis.utils.LogUtils;
import atlantis.com.db.DatabaseManager;
import atlantis.com.model.CipherAddress;
import atlantis.com.model.CipherMessage;
import atlantis.com.model.Conversation;
import atlantis.com.model.Message;
import atlantis.com.model.MessageContent;
import atlantis.com.model.Notebook;
import atlantis.com.model.OTP;
import atlantis.com.model.OutgoingMessage;
import atlantis.com.model.exceptions.MessageAuthenticityFailedException;
import atlantis.com.model.exceptions.NotEnoughOTPException;
import atlantis.com.model.impls.FileOTPManager;
import atlantis.com.model.impls.OTPFileInputStream;
import atlantis.com.model.interfaces.OTPManager;
import atlantis.com.server.MessageCourier;

/**
 * Created by ricardo on 4/5/15.
 */
public class SocketManager {
    private final static String SOCKET_TAG = "SOCKET";

    private final static String SOCKET_ADDRESS = "http://atlantis-app.herokuapp.com";
//    public final static String SOCKET_ADDRESS = "http://192.168.1.109:5000";

    private final static String SOCKET_MESSAGE_POST = "post";
    public final static String SOCKET_MESSAGE_POST_ERROR = "post-error";
    public final static String SOCKET_MESSAGE_POST_SUCCESS = "post-success";

    private final static String SOCKET_MESSAGE_GET = "get";
    public final static String SOCKET_MESSAGE_GET_ERROR = "get-error";
    private final static String SOCKET_MESSAGE_MESSAGES = "messages";

    private final Map<String, Pair<Conversation, OTP>> mRequestedAddresses = new HashMap<>();

    static private SocketManager instance;

    static public SocketManager getInstance(Context context) {
        if(null == instance) {
            instance = new SocketManager(context);
        }
        return instance;
    }

    private Socket mSocket;
    private Context mContext;

    /**
     * Initializes and configures socket
     * @param context
     */
    private SocketManager(Context context) {
        try {
            mContext = context;
            mSocket = IO.socket(SOCKET_ADDRESS);
            mSocket.on(Socket.EVENT_CONNECT, mConnectListener);
            mSocket.on(Socket.EVENT_CONNECT_ERROR, mErrorListener);
            mSocket.on(Socket.EVENT_ERROR, mErrorListener);
            mSocket.on(Socket.EVENT_DISCONNECT, mErrorListener);
            mSocket.on(SOCKET_MESSAGE_MESSAGES, mMessageListener);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Connects to server
     */
    private void connect() {
        if(!mSocket.connected()) {
            mSocket.connect();
        }
    }

    /**
     * Disconnects from server
     */
    public void disconnect() {
        if(mSocket.connected()) {
            mSocket.disconnect();
        }
    }

    /**
     * Listener for when socket has an error
     */
    private final Emitter.Listener mErrorListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.e(SOCKET_TAG, "ERROR");
        }
    };

    /**
     * Listener for when socket connects
     */
    private final Emitter.Listener mConnectListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.d(SOCKET_TAG, "connected");
        }
    };

    /**
     * Listener for when messages are received from server
     */
    private final Emitter.Listener mMessageListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONArray messages = (JSONArray) args[0];
            for(int i = 0; i < messages.length(); i++) {
                try {
                    JSONObject jsonMessage = (JSONObject) messages.get(i);
                    // Decode message
                    Pair<String, CipherMessage> decoded = decodeJSONToCipherMessage(jsonMessage);
                    String encodedAddress = decoded.first;
                    CipherMessage cipherMessage = decoded.second;
                    try {
                        // Process message
                        receiveCipherMessage(encodedAddress, cipherMessage);
                    } catch (IOException | PINNotCreatedException | PINCreationFailedException | OTPFileInputStream.InvalidBufferException | NotAuthenticatedException | SQLException | NoSuchAlgorithmException | InvalidKeyException | MessageAuthenticityFailedException e) {
                        // Drop if problem
                        e.printStackTrace();
                    }
                } catch (JSONException | NotEnoughOTPException e) {
                    // Drop if problem
                    e.printStackTrace();
                }
            }
        }
    };

    /**
     * Decodes a cipher message, processes it, and requests the next address
     * @param encodedAddress The base-64 address of the cipher message
     * @param cipherMessage The cipher message to decode
     */
    private void receiveCipherMessage(String encodedAddress, CipherMessage cipherMessage) throws NotAuthenticatedException, PINNotCreatedException, SQLException, OTPFileInputStream.InvalidBufferException, NotEnoughOTPException, PINCreationFailedException, NoSuchAlgorithmException, InvalidKeyException, MessageAuthenticityFailedException, IOException {
        // Lookup requested conversation and OTP for the address received
        Pair<Conversation, OTP> request = mRequestedAddresses.remove(encodedAddress);

        // If this message address was requested, process it
        if (request != null) {
            Conversation conversation = request.first;
            OTP foreignOTP = request.second;

            OTPManager otpManager = new FileOTPManager(mContext);
            DatabaseManager databaseManager = DatabaseManager.getInstance(mContext);

            Notebook notebook = databaseManager.getNotebookForOTP(foreignOTP);
            if(Arrays.equals(cipherMessage.getAddressBytes(), notebook.getLastAckAddressBytes())) {
                // Repeated message, resend ack
                resendLastAckForNotebook(notebook);
            } else {
                // New message, previous message was received correctly

                // Decrypt new message
                Message message = otpManager.decryptCipherMessage(cipherMessage, foreignOTP);
                MessageContent messageContent = message.getSerializedContent();

                // Message is an ack, process it
                if (messageContent.getContentType() == MessageContent.CONTENT_TYPE_ACK) {
                    processAck(foreignOTP, messageContent.getAckContent());
                    // Send next message
                    sendNextMessageForNotebook(notebook);
                } else {
                    // Regular message, send new ack
                    sendNewAckForNotebook(new CipherAddress(cipherMessage.getAddressBytes()), notebook);
                    // Process the message
                    processMessage(message, foreignOTP, conversation);
                }
            }

            // Request next message address
            requestMessagesInConversation(conversation, foreignOTP);
        }
    }

    /**
     * Add a new message arrived, add it to the database and notify
     * @param message The message that arrived
     * @param foreignOTP The otp that was used
     * @param conversation The conversation for the message
     * @throws SQLException
     * @throws IOException
     * @throws NotAuthenticatedException
     * @throws PINCreationFailedException
     * @throws NotEnoughOTPException
     * @throws OTPFileInputStream.InvalidBufferException
     * @throws PINNotCreatedException
     */
    private void processMessage(Message message, OTP foreignOTP, Conversation conversation) throws SQLException, IOException, NotAuthenticatedException, PINCreationFailedException, NotEnoughOTPException, OTPFileInputStream.InvalidBufferException, PINNotCreatedException {
        DatabaseManager databaseManager = DatabaseManager.getInstance(mContext);

        // Add new message
        message.setTimestamp();
        message.setConversation(conversation);
        message.setSender(databaseManager.getOwnerOfOTP(foreignOTP));
        databaseManager.addMessage(message);

        // Update conversation description
        conversation.setDescription(message.getSerializedContent().getStringContent());
        databaseManager.updateConversation(conversation);

        // Notify new message
        Intent intent = new Intent(Events.MESSAGE_CREATED);
        intent.putExtra(Extras.MESSAGE_ID, message.getId());
        intent.putExtra(Extras.CONVERSATION_ID, conversation.getId());
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    /**
     * An ack has arrived for a message, set the message as delivered and notify
     * @param otp The otp it arrived for
     * @param ackAddress The address that was acked
     * @throws SQLException
     * @throws MessageAuthenticityFailedException
     */
    private void processAck(OTP otp, CipherAddress ackAddress) throws SQLException, MessageAuthenticityFailedException {
        DatabaseManager databaseManager = DatabaseManager.getInstance(mContext);

        Notebook notebook = databaseManager.getNotebookForOTP(otp);
        Iterator<OutgoingMessage> outgoingMessageIterator = notebook.getOutgoingMessages().iterator();
        if(outgoingMessageIterator.hasNext()) {
            OutgoingMessage outgoingMessage = outgoingMessageIterator.next();

            Log.d(SOCKET_TAG, "Received ACK for address: ");
            LogUtils.logBytes(SOCKET_TAG, ackAddress.getBytes());

            Message message = outgoingMessage.getMessage();
            if(message == null) {
                throw new MessageAuthenticityFailedException();
            }

            message.setDelivered(true);
            databaseManager.updateMessage(message);
            databaseManager.removeCipherMessage(outgoingMessage.getCipherMessage());
            databaseManager.removeOutgoingMessage(outgoingMessage);
            databaseManager.refreshNotebook(notebook);

            // Notify delivered message
            Intent intent = new Intent(Events.MESSAGE_DELIVERED);
            intent.putExtra(Extras.MESSAGE_ID, message.getId());
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        } else {
            throw new MessageAuthenticityFailedException();
        }
    }

    /**
     * The ack needs to be resent, resend the cached ack
     * @param notebook The notebook to send the ack for
     */
    private void resendLastAckForNotebook(Notebook notebook) {
        sendCipherMessage(notebook.getLastAck());
    }

    /**
     * Create an ack for an address, cache it in the notebook, then send
     * @param addressToAck The address to create the ack for
     * @param notebook The notebook to ack for
     * @throws IOException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws PINCreationFailedException
     * @throws PINNotCreatedException
     * @throws NotEnoughOTPException
     * @throws NotAuthenticatedException
     * @throws SQLException
     * @throws OTPFileInputStream.InvalidBufferException
     */
    private void sendNewAckForNotebook(CipherAddress addressToAck, Notebook notebook)
            throws IOException, InvalidKeyException, NoSuchAlgorithmException, PINCreationFailedException,
                PINNotCreatedException, NotEnoughOTPException, NotAuthenticatedException, SQLException,
                OTPFileInputStream.InvalidBufferException {
        Log.d(SOCKET_TAG, "Sending ACK for address: ");
        LogUtils.logBytes(SOCKET_TAG, addressToAck.getBytes());

        CipherMessage ackCipherMessage = createAckCipherMessageForAddress(addressToAck, notebook);
        notebook.setLastAckAddress(addressToAck.getBytes());
        notebook.setLastAck(ackCipherMessage);
        sendCipherMessage(ackCipherMessage);
    }

    /**
     * Create an ack for the address
     * @param ackAddress The ack address
     * @param notebook The notebook for the ack
     * @return The new cipher message with the ack content
     * @throws NotAuthenticatedException
     * @throws PINNotCreatedException
     * @throws PINCreationFailedException
     * @throws InvalidKeyException
     * @throws OTPFileInputStream.InvalidBufferException
     * @throws IOException
     * @throws SQLException
     * @throws NoSuchAlgorithmException
     * @throws NotEnoughOTPException
     */
    private CipherMessage createAckCipherMessageForAddress(CipherAddress ackAddress, Notebook notebook)
            throws NotAuthenticatedException, PINNotCreatedException, PINCreationFailedException,
                InvalidKeyException, OTPFileInputStream.InvalidBufferException, IOException,
                SQLException, NoSuchAlgorithmException, NotEnoughOTPException {
        Message ackMessage = new Message();
        ackMessage.setSerializedContent(new MessageContent(ackAddress));
        OTPManager otpManager = new FileOTPManager(mContext);
        return otpManager.encryptMessage(ackMessage, notebook.getSendingOTP());
    }

    /**
     * Sends a message in a conversation by adding it to the outgoing messages
     * @param message The message to send
     * @param conversation The conversation to send in
     */
    public void sendMessageInConversation(Message message, Conversation conversation)
            throws NotAuthenticatedException, PINNotCreatedException, SQLException,
                OTPFileInputStream.InvalidBufferException, NotEnoughOTPException,
                PINCreationFailedException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        DatabaseManager databaseManager = DatabaseManager.getInstance(mContext);
        List<Notebook> notebooks = databaseManager.getNotebooksInConversation(conversation);

        for(Notebook notebook : notebooks) {
            OutgoingMessage outgoingMessage = new OutgoingMessage();
            outgoingMessage.setMessage(message);
            outgoingMessage.setNotebook(notebook);
            databaseManager.addOutgoingMessage(outgoingMessage);
            databaseManager.refreshNotebook(notebook);

            sendNextMessageForNotebook(notebook);
        }
    }

    /**
     * Converts a cipher message to a JSON object
     * @param cipherMessage The message to convert
     * @return The JSON representation
     */
    private JSONObject encodeCipherMessageToJSON(CipherMessage cipherMessage) {
        JSONObject jsonMessage = new JSONObject();
        try {
            jsonMessage.put("address",
                    Base64.encodeToString(cipherMessage.getAddressBytes(),
                            MessageCourier.BASE64_OPTIONS));
            jsonMessage.put("content",
                    Base64.encodeToString(cipherMessage.getContent(),
                            MessageCourier.BASE64_OPTIONS));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonMessage;
    }

    /**
     * Converts JSON object to a cipher message
     * @param jsonMessage The JSON representation
     * @return The encoded address and the cipher message
     */
    private Pair<String, CipherMessage> decodeJSONToCipherMessage(JSONObject jsonMessage) {
        CipherMessage cipherMessage = new CipherMessage();
        String encodedAddress = null;
        try {
            encodedAddress = jsonMessage.getString("address");
            byte[] address = Base64.decode(encodedAddress,
                    MessageCourier.BASE64_OPTIONS);
            byte[] cipherContent = Base64.decode(jsonMessage.getString("content"),
                    MessageCourier.BASE64_OPTIONS);
            cipherMessage.setAddressBytes(address);
            cipherMessage.setContent(cipherContent);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return new Pair<>(encodedAddress, cipherMessage);
    }

    /**
     * Sends the next outgoing message by creating the cipher message if needed and sending it
     * @param notebook The notebook to send the next message for
     * @throws NotAuthenticatedException
     * @throws PINNotCreatedException
     * @throws PINCreationFailedException
     * @throws InvalidKeyException
     * @throws OTPFileInputStream.InvalidBufferException
     * @throws IOException
     * @throws SQLException
     * @throws NoSuchAlgorithmException
     * @throws NotEnoughOTPException
     */
    private void sendNextMessageForNotebook(Notebook notebook) throws NotAuthenticatedException, PINNotCreatedException, PINCreationFailedException, InvalidKeyException, OTPFileInputStream.InvalidBufferException, IOException, SQLException, NoSuchAlgorithmException, NotEnoughOTPException {
        ForeignCollection<OutgoingMessage> outgoingMessages = notebook.getOutgoingMessages();

        Iterator<OutgoingMessage> outgoingMessageIterator = outgoingMessages.iterator();
        if(outgoingMessageIterator.hasNext()) {
            // Get the next message
            OutgoingMessage outgoingMessage = outgoingMessageIterator.next();
            if(outgoingMessage.getCipherMessage() == null) {
                OTPManager otpManager = new FileOTPManager(mContext);
                DatabaseManager databaseManager = DatabaseManager.getInstance(mContext);

                // Encrypt message
                OTP otp = notebook.getSendingOTP();
                CipherMessage cipherMessage = otpManager.encryptMessage(outgoingMessage.getMessage(), otp);

                // Cache cipher message
                databaseManager.addCipherMessage(cipherMessage);
                outgoingMessage.setCipherMessage(cipherMessage);
                databaseManager.updateOutgoingMessage(outgoingMessage);
            }
            // Send
            sendCipherMessage(outgoingMessage.getCipherMessage());
        }
    }

    /**
     * Sends the cipher message through the socket
     * @param cipherMessage The cipher message to send
     */
    private void sendCipherMessage(CipherMessage cipherMessage) {
        connect();

        Log.d(SOCKET_TAG, "Sending cipher message id " + cipherMessage.getId());

        JSONArray messages = new JSONArray();
        messages.put(encodeCipherMessageToJSON(cipherMessage));
        mSocket.emit(SOCKET_MESSAGE_POST, messages, mPostMessagesAck);
    }

    /**
     * Acknowledgement from server when message is posted
     */
    private final Ack mPostMessagesAck = new Ack() {
        @Override
        public void call(Object... args) {
            try {
                JSONObject debug = new JSONObject();
                debug.put("received ack", args[0]);
                LogUtils.logJSON(SOCKET_TAG, debug);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Request that next message be resent
     * @param conversation The conversation to resend any queued conversations
     * @throws SQLException
     * @throws NotEnoughOTPException
     * @throws PINCreationFailedException
     * @throws InvalidKeyException
     * @throws OTPFileInputStream.InvalidBufferException
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws PINNotCreatedException
     * @throws NotAuthenticatedException
     */
    public void sendQueuedMessagesInConversation(Conversation conversation) throws SQLException, NotEnoughOTPException, PINCreationFailedException, InvalidKeyException, OTPFileInputStream.InvalidBufferException, IOException, NoSuchAlgorithmException, PINNotCreatedException, NotAuthenticatedException {
        DatabaseManager databaseManager = DatabaseManager.getInstance(mContext);
        List<Notebook> notebooks = databaseManager.getNotebooksInConversation(conversation);
        for(Notebook notebook : notebooks) {
            sendNextMessageForNotebook(notebook);
        }
    }

    /**
     * Requests messages from server in conversation and optionally from specific OTPs
     * @param conversation The conversation to request messages from
     * @param otps Optional: Specific OTPs to request from
     */
    public void requestMessagesInConversation(Conversation conversation, OTP... otps) throws NotAuthenticatedException, PINNotCreatedException, SQLException, OTPFileInputStream.InvalidBufferException, NotEnoughOTPException, PINCreationFailedException, IOException {
        connect();

        try {
            FileOTPManager fileOTPManager = new FileOTPManager(mContext);
            DatabaseManager databaseManager = DatabaseManager.getInstance(mContext);

            List<OTP> contactOTPs;
            // If OTPs were given
            if(otps.length > 0) {
                // Request for them
                contactOTPs = Arrays.asList(otps);
            } else {
                // Otherwise request for all contacts
                contactOTPs = databaseManager.getNonSelfOTPsInConversation(conversation);
            }

            JSONArray addresses = new JSONArray();
            JSONArray debugArray = new JSONArray();
            for(OTP otp : contactOTPs){
                // Get address and register as requested address
                byte[] address = fileOTPManager.getAddressFromOTP(otp);
                String encodedAddress = Base64.encodeToString(address,
                        MessageCourier.BASE64_OPTIONS);
                addresses.put(encodedAddress);
                mRequestedAddresses.put(encodedAddress, new Pair<>(conversation, otp));

                JSONObject debug = new JSONObject();
                debug.put("action", "checking contact OTP");
                debug.put("id", otp.getId());
                debug.put("data_id", otp.getDataId());
                debug.put("address", LogUtils.bytesToJSON(address));
                debug.put("encoded_address", encodedAddress);
                debugArray.put(debug);
            }
            LogUtils.logJSON(SOCKET_TAG, debugArray);
            // Send request for addresses
            mSocket.emit(SOCKET_MESSAGE_GET, addresses, mRequestedAddressesAck);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Acknowledgement from server that request was received
     */
    private final Ack mRequestedAddressesAck = new Ack() {
        @Override
        public void call(Object... args) {
            try {
                JSONObject debug = new JSONObject();
                debug.put("received ack", args[0]);
                LogUtils.logJSON(SOCKET_TAG, debug);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };
}
