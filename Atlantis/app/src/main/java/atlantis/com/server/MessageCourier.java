package atlantis.com.server;

import android.content.Context;
import android.util.Base64;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import atlantis.com.atlantis.encryption.PINCreationFailedException;
import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;
import atlantis.com.model.Conversation;
import atlantis.com.model.Message;
import atlantis.com.model.exceptions.NotEnoughOTPException;
import atlantis.com.model.impls.OTPFileInputStream;
import atlantis.com.server.socket.SocketManager;

/**
 * Created by Daniel on 2/27/2015.
 */
public class MessageCourier {

    static private MessageCourier instance;

    static public MessageCourier getInstance() {
        if(null == instance) {
            instance = new MessageCourier();
        }
        return instance;
    }

    public static final int BASE64_OPTIONS = Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP;

    public void requestMessagesInConversation(Conversation conversation, Context context) throws SQLException, NotEnoughOTPException, PINCreationFailedException, OTPFileInputStream.InvalidBufferException, PINNotCreatedException, NotAuthenticatedException, IOException {
        SocketManager.getInstance(context).requestMessagesInConversation(conversation);
    }

    public void sendMessageInConversation(Message message, Conversation conversation, Context context) throws SQLException, NotEnoughOTPException, NotAuthenticatedException, PINNotCreatedException, PINCreationFailedException, OTPFileInputStream.InvalidBufferException, IOException, NoSuchAlgorithmException, InvalidKeyException {
        SocketManager.getInstance(context).sendMessageInConversation(message, conversation);
    }

    public void sendQueuedMessagesInConversation(Conversation conversation, Context context) throws SQLException, PINCreationFailedException, InvalidKeyException, OTPFileInputStream.InvalidBufferException, IOException, PINNotCreatedException, NoSuchAlgorithmException, NotEnoughOTPException, NotAuthenticatedException {
        SocketManager.getInstance(context).sendQueuedMessagesInConversation(conversation);
    }
}
