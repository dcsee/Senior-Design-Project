package atlantis.com.atlantis.communications.nearcommunications;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;

import java.io.FileNotFoundException;

import atlantis.com.atlantis.activities.NearCommunicationActivity;
import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;
import atlantis.com.atlantis.utils.BytesUtils;

/**
 * Created by jvronsky on 3/3/15.
 * abstract class for all the communications methods to be implemented.
 */
public abstract class Communicator extends Handler{

    public static final String COMMUNICATION_COMPLETE_ACTION = "COMMUNICATION_COMPLETE";
    protected final Context context;
    protected final Activity parentActivity;
    protected final NearCommunicationMethod nearCommunicationMethod;


    public Communicator(Context context,
                        NearCommunicationMethod nearCommunicationMethod) {
        this.nearCommunicationMethod = nearCommunicationMethod;
        this.context = context;
        this.parentActivity = (Activity) context;
    }

    /**
     * Process init message, convert data to an int and pass it on to the overridden version.
     * @param dataLength the data from init connection message
     */
    public void processInitConnectionMessage(byte[] dataLength) throws NotAuthenticatedException, PINNotCreatedException, FileNotFoundException {
        int length = BytesUtils.byteArrayToInt(dataLength);
        processInitConnectionMessage(length);
    }

    /**
     * Start the communicator method.
     */
    public abstract void startCommunication();

    /**
     * Process Ready Message.
     */
    public abstract void processReadyMessage();

    /**
     * Process DataPiece message.
     * @param data to process
     */
    public abstract void processDataPieceMessage(byte[] data);

    /**
     * Process Received Successfully.
     */
    public abstract void processReceivedSuccessfully();

    /**
     * Process Story Received Message.
     */
    public abstract void processStoryReceivedMessage();

    /**
     * Process Ready Message.
     */
    protected abstract void sendReadyMessage();

    /**
     * Process DataPiece message.
     * @param data to process
     */
    protected abstract void sendDataPieceMessage(byte[] data);
    /**
     * Process Received Successfully.
     */
    protected abstract void sendReceivedSuccessfully();

    /**
     * Process Story Received Message.
     */
    protected abstract void sendStoryReceivedMessage();

    /**
     * The overriden version of process Init Connection Message.
     * @param dataLength length of the data specified in the message
     */
    protected abstract void sendInitConnectionMessage(int dataLength);
    /**
     * The overriden version of process Init Connection Message.
     * @param dataLength length of the data specified in the message
     */
    protected abstract void processInitConnectionMessage(int dataLength) throws NotAuthenticatedException, PINNotCreatedException, FileNotFoundException;

    /**
     * Send a message through the near communication medium.
     * @param message message to send.
     */
    protected void send(byte[] message) {
        nearCommunicationMethod.send(message);
    }

    /**
     * Notify activity that message is complete via broadcast reciever.
     */
    protected void notifyOfCommunicationComplete() {
        nearCommunicationMethod.restoreSettings();
        Intent intent = new Intent();
        intent.setAction(NearCommunicationActivity.ACTION_COMMUNICATION_OVER);
        context.sendBroadcast(intent);
    }
}
