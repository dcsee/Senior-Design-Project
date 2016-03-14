package atlantis.com.atlantis.communications.nearcommunications;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import atlantis.com.atlantis.R;
import atlantis.com.atlantis.encryption.LocalEncryption;
import atlantis.com.atlantis.encryption.OTPEncryptionManager;
import atlantis.com.atlantis.encryption.PINCreationFailedException;
import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;
import atlantis.com.atlantis.utils.BytesUtils;
import atlantis.com.atlantis.utils.SharedPreferencesHelper;
import atlantis.com.atlantis.utils.SharedPreferencesHelper.CouldNotWriteToSharedPreferencesException;
import atlantis.com.atlantis.utils.TimeUtils;
import atlantis.com.db.DatabaseManager;
import atlantis.com.model.OTP;
import atlantis.com.model.impls.FileOTPManager;
import atlantis.com.model.impls.OTPFileInputStream;
import atlantis.com.model.impls.OTPFileOutputStream;

/**
 * Created by jvronsky on 5/18/15.
 * Sync manager takes care of the syncing process
 */
public class SyncManager extends Handler {

    private static final String TAG = "SyncManager";

    public static final String SYNC_SHARED_PREFERENCES_FOLDER = "com.atlantis.syncing";
    // Next chunk index is used to be able to start from failure.
    public static final String NEXT_CHUNK_INDEX_KEY = "com.atlantis.sync.next_chunk_index";
    public static final String SENDING_OTP_KEY = "com.atlantis.sync.sending_otp";
    public static final String RECEIVING_OTP_KEY = "com.atlantis.sync.receiving_otp";
    public static final String TIMESTAMP_KEY =  "com.atlantis.sync.timestamp";
    public static final String STATE_KEY = "com.atlantis.sync.state";
    public static final String BUFFER_FILE = "com.atlantis.sync.buffer";

    // Sync manager states.
    private static final int SENDING = 0x00;
    private static final int RECEIVING = 0x01;

    // Timeout is 10 minutes.
    private final static long SHARED_PREFERENCES_TIMESTAMP_TIMEOUT =
            TimeUtils.minutesToMiliseconds(10);

    private SharedPreferencesHelper mSharedPreferences;

    private NearCommunicationMethod mNearCommunicationMethod;

    // Activity variables.
    private final Activity mParentActivity;

    // Sync variables.
    private OTP mSendingOTP;
    private OTP mReceivingOTP;
    private int mNextChunkIndex;
    private int mSendingOTPLength;
    private boolean mIsHost;
    private int mState;
    private boolean mIsSyncInProgress;
    private OTPFileInputStream mSendingOTPSource;
    private OTPFileOutputStream mReceivingOTPSource;
    private FileOTPManager mFileOTPManager;
    private RetryRunnable mRetryRunnable;
    private Handler mRetryHandler;
    private boolean mProcessedInitMessage;
    // If message was not received within 5 seconds ask for it again.
    private static final long RETRY_DELAY_TIMEOUT = TimeUtils.secondsToMilliseconds(1);
    public static final String SYNC_IS_COMPLETE = "com.atlantis.sync.SYNC_IS_COMPLETE";

    // UI variables.
    private ProgressDialog mSyncManagerProgress;
    private final String[] mStateProgressDialogTitles;
    private final String mTitle;

    public SyncManager(Context context, NearCommunicationMethod nearCommunicationMethod) throws BadSyncDataException, NotAuthenticatedException, PINNotCreatedException, IOException, PINCreationFailedException {
        this.mParentActivity = (Activity) context;
        this.mNearCommunicationMethod = nearCommunicationMethod;
        mNearCommunicationMethod.setSyncManager(this);
        mSharedPreferences = new SharedPreferencesHelper(context, SYNC_SHARED_PREFERENCES_FOLDER);
        mTitle = mParentActivity.getString(R.string.sync_dialog_title);
        mStateProgressDialogTitles = new String[]{mParentActivity.getString(
                R.string.sync_receiving_message),
                mParentActivity.getString(R.string.sync_sending_message)};
        initializeVariables();
    }

    /**
     * Initialize variables required for sync manager.
     * @throws BadSyncDataException
     */
    private void initializeVariables() throws BadSyncDataException, NotAuthenticatedException, PINNotCreatedException, IOException, PINCreationFailedException {
        SyncManagerStats syncManagerStats = new SyncManagerStats().load();
        int sendingOTPId = syncManagerStats.getSendingOTPId();
        int receivingOTPId = syncManagerStats.getReceivingOTPId();
        int nextChunkIndex = syncManagerStats.getNextChunkIndex();
        mState = syncManagerStats.getState();
        long timestamp = mSharedPreferences.get(TIMESTAMP_KEY, -1L);
        mProcessedInitMessage = false;
        if(!isAllDataRetrievedCorrectly(sendingOTPId, receivingOTPId, timestamp, nextChunkIndex)) {
            throw new BadSyncDataException("Data passed to sync manager was bad");
        }
        long currentTime = System.currentTimeMillis();
        if(TimeUtils.timeDifferenceMiliSeconds(timestamp, currentTime)
                > SHARED_PREFERENCES_TIMESTAMP_TIMEOUT) {
            throw new BadSyncDataException("Too much time between sync setup and sync start");
        }
        DatabaseManager databaseManager = DatabaseManager.getInstance(mParentActivity);
        mSendingOTP = databaseManager.getOTPWithId(sendingOTPId);
        mReceivingOTP = databaseManager.getOTPWithId(receivingOTPId);
        // If nextChunkIndex it means the process quit and we now need to append data to the file
        if(nextChunkIndex > 0) {
            mReceivingOTPSource = new OTPFileOutputStream(mParentActivity, mReceivingOTP, true);
            mReceivingOTPSource.setOutputBuffer(syncManagerStats.getBuffer());
        }
        mSendingOTPLength = mSendingOTP.getLength();
        mSendingOTPSource = new OTPFileInputStream(mParentActivity, mSendingOTP);
        mFileOTPManager = new FileOTPManager(mParentActivity);
        mRetryHandler = new Handler();
        mRetryRunnable = new RetryRunnable();
        mIsSyncInProgress = false;
        mNextChunkIndex = nextChunkIndex;
        setupProgressDialog();
    }

    public void terminateAndSave() throws CouldNotWriteToSharedPreferencesException, NotAuthenticatedException, PINNotCreatedException, PINCreationFailedException, IOException {
        if(mIsSyncInProgress) {
            byte[] outputBuffer;
            if(mReceivingOTPSource == null) {
                outputBuffer = new byte[] {};
            } else {
                outputBuffer = mReceivingOTPSource.getOutputBuffer();
            }
            new SyncManagerStats(mState, mSendingOTP.getId(),
                    mReceivingOTP.getId(), mNextChunkIndex, outputBuffer).save();
        }
    }

    public void terminateAndClear() throws CouldNotWriteToSharedPreferencesException {
        new SyncManagerStats().clear();
    }

    public void startCommunication() {
        mIsHost = true;
        mIsSyncInProgress = true;
        if(mNextChunkIndex > 0) {
            changeState(RECEIVING);
            sendReadyMessage(mNextChunkIndex);
        } else if(mNextChunkIndex == 0) {
            changeState(SENDING);
            sendInitCommunicationMessage();
        }
    }

    /**
     * Send message to handler to handle.
     * @param code
     * @param message
     */
    public void sendMessageToTarget(int code, byte[] message) {
        this.obtainMessage(code, message).sendToTarget();
    }

    public boolean isSyncInProgress() {
        return mIsSyncInProgress;
    }

    /**
     * Route message to continue syncing.
     *@param msg
     */
    @Override
    public void handleMessage(Message msg) {
        int code = msg.what;
        byte[] data = (byte[]) msg.obj;
        try{
            switch (code) {
                case NearCommunicationMessage.INIT_CONNECTION: processInitConnectionMessage(data); break;
                case NearCommunicationMessage.READY: processReadyMessage(data); break;
                case NearCommunicationMessage.DATA_PIECE: processDataPiece(data); break;
                case NearCommunicationMessage.DONE_SENDING: processDoneSending(); break;
            }
        } catch (IOException | NotAuthenticatedException | PINCreationFailedException |
                PINNotCreatedException | OTPFileOutputStream.OTPFileOutputStreamException
                | OTPFileInputStream.InvalidBufferException e) {
            e.printStackTrace();
        }
    }

    private void processDoneSending() throws PINCreationFailedException, OTPFileOutputStream.OTPFileOutputStreamException, IOException {
        changeState(SENDING);
        mReceivingOTPSource.close();
        mNextChunkIndex = 0;
        resetProgressDialog(mSyncManagerProgress);
        mRetryHandler.removeCallbacks(mRetryRunnable);
        if(!mIsHost) {
            sendInitCommunicationMessage();
        } else {
            notifySyncComplete();
        }
    }

    private void processDataPiece(byte[] data) throws PINCreationFailedException, OTPFileOutputStream.OTPFileOutputStreamException, IOException {
        mReceivingOTPSource.write(data);
        this.mNextChunkIndex += data.length;
        updateProgress(mSyncManagerProgress, mNextChunkIndex);
        mRetryHandler.removeCallbacks(mRetryRunnable);
        sendReadyMessage(mNextChunkIndex);
    }

    private void processReadyMessage(byte[] data) throws PINCreationFailedException, OTPFileInputStream.InvalidBufferException, IOException {
        int chunkNumber = BytesUtils.byteArrayToInt(data);
        mRetryHandler.removeCallbacks(mRetryRunnable);
        if(chunkNumber >= mSendingOTPLength) {
            sendDoneSending();
        } else {
            sendDataPiece(chunkNumber);
        }
    }

    private void processInitConnectionMessage(byte[] data) throws FileNotFoundException, NotAuthenticatedException, PINNotCreatedException {
        if(!mProcessedInitMessage) {
            int receivingOTPLength = BytesUtils.byteArrayToInt(data);
            changeState(RECEIVING);
            mFileOTPManager.setOTPDataId(mReceivingOTP, mFileOTPManager.createOTPFile());
            mFileOTPManager.setOTPLength(mReceivingOTP, receivingOTPLength);
            mReceivingOTPSource = new OTPFileOutputStream(mParentActivity, mReceivingOTP);
            updateProgressMax(mSyncManagerProgress, receivingOTPLength);
            showProgress(mSyncManagerProgress);
            sendReadyMessage(0);
            mProcessedInitMessage = true;
        }
    }

    private void sendReadyMessage(int chunkRequestNumber) {
        send(NearCommunicationMessage.buildReadyMessage(chunkRequestNumber));
        mRetryRunnable.setChunkRequestNumber(chunkRequestNumber);
        mRetryHandler.postDelayed(mRetryRunnable, RETRY_DELAY_TIMEOUT);
    }

    private void sendInitCommunicationMessage() {
        changeState(SENDING);
        resetProgressDialog(mSyncManagerProgress);
        updateProgressMax(mSyncManagerProgress, mSendingOTPLength);
        showProgress(mSyncManagerProgress);
        send(NearCommunicationMessage.buildInitConnectionMessage(mSendingOTPLength));
        mRetryHandler.postDelayed(mRetryRunnable, RETRY_DELAY_TIMEOUT);
    }

    private void sendDoneSending() {
        changeState(RECEIVING);
        send(NearCommunicationMessage.buildDoneSendingMessage());
        if(!mIsHost) {
            notifySyncComplete();
        }
    }

    private void sendDataPiece(int chunkIndex) throws PINCreationFailedException, OTPFileInputStream.InvalidBufferException, IOException {
        int messageSize = mNearCommunicationMethod.maxDataPieceSize() - NearCommunicationMessage.HEADER_LENGTH;
        byte[] buffer = new byte[messageSize];
        mSendingOTPSource.reset();
        mSendingOTPSource.skip(chunkIndex);
        int bytesRead = mSendingOTPSource.read(buffer);
        if(bytesRead == (messageSize)) {
            send(NearCommunicationMessage.buildDataPieceMessage(buffer));
        } else if(bytesRead != -1){
            send(NearCommunicationMessage.buildDataPieceMessage(
                    Arrays.copyOfRange(buffer, 0, bytesRead)));
        } else {
            bytesRead = 0;
        }
        updateProgress(mSyncManagerProgress, chunkIndex + bytesRead);
    }

    private void changeState(int newState) {
        mState = newState;
        updateProgressMessage(mSyncManagerProgress, mStateProgressDialogTitles[newState]);
    }

    private void send(byte[] message) {
        mNearCommunicationMethod.send(message);
    }

    private void notifySyncComplete() {
        mIsSyncInProgress = false;
        cancelProgress(mSyncManagerProgress);
        Intent intent = new Intent();
        intent.setAction(SYNC_IS_COMPLETE);
        mParentActivity.sendBroadcast(intent);
        mRetryHandler.removeCallbacks(mRetryRunnable);
    }

    /**
     * Checks all data that needs to be read from shared-preferences was read in with valid values.
     * @param sendingOTPId
     * @param receivingOTPId
     * @param timestamp
     * @param nextChunkIndex
     * @return
     */
    private boolean isAllDataRetrievedCorrectly(int sendingOTPId, int receivingOTPId, long timestamp, int nextChunkIndex) {
        return (sendingOTPId != 0) && (receivingOTPId != 0) && (timestamp > 0) && (nextChunkIndex >= 0);
    }

    /**
     * Sets up the progress dialog to desired default.
     */
    private void setupProgressDialog() {
        mSyncManagerProgress = new ProgressDialog(mParentActivity);
        mSyncManagerProgress.setTitle(mTitle);
        mSyncManagerProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mSyncManagerProgress.setIndeterminate(false);
        mSyncManagerProgress.setCancelable(true);
        mSyncManagerProgress.setProgressNumberFormat(null);
        mSyncManagerProgress.setMax(mReceivingOTP.getLength());
    }

    private void resetProgressDialog(ProgressDialog progressDialog) {
        progressDialog.setProgress(0);
    }

    /**
     * Close progress bar.
     * @param progressDialog to be closed
     */
    private void cancelProgress(final ProgressDialog progressDialog) {
        mParentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.cancel();
            }
        });
    }

    /**
     * Show progress  bar.
     * @param progressDialog to be shown
     */
    private void updateProgressMax(final ProgressDialog progressDialog, final int max) {
        mParentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.setMax(max);
            }
        });
    }

    /**
     * Show progress  bar.
     * @param progressDialog to be shown
     */
    private void showProgress(final ProgressDialog progressDialog) {
        mParentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.show();
            }
        });
    }

    /**
     * Update progress bar.
     * @param progressDialog to be updated
     * @param updateValue value to update by
     */
    private void updateProgress(final ProgressDialog progressDialog, final int updateValue) {
        mParentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.setProgress(updateValue);
            }
        });
    }

    /**
     * Update progress bar.
     * @param progressDialog to be updated
     * @param message message to update
     */
    private void updateProgressMessage(final ProgressDialog progressDialog, final String message) {
        mParentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.setMessage(message);
            }
        });
    }

    private class RetryRunnable implements Runnable {

        private int chunkRequestNumber;

        public RetryRunnable() {
            chunkRequestNumber = 0;
        }

        public void setChunkRequestNumber(int chunkRequestNumber) {
            this.chunkRequestNumber = chunkRequestNumber;
        }

        @Override
        public void run() {
            if(mState == SENDING) {
                sendInitCommunicationMessage();
            } else if(mState == RECEIVING) {
                sendReadyMessage(chunkRequestNumber);
            }
        }
    }

    public OTP getReceivingOTP() {
        return mReceivingOTP;
    }

    private class SyncManagerStats {

        private int mState;
        private int mSendingOTPId;
        private int mReceivingOTPId;
        private int mNextChunkIndex;
        private byte[] mBuffer;
        private File mOutputBufferFile;

        public SyncManagerStats() {
            mOutputBufferFile = new File(mParentActivity.getDir(
                    SYNC_SHARED_PREFERENCES_FOLDER, Context.MODE_PRIVATE), BUFFER_FILE);
        }

        public SyncManagerStats(int mState, int sendingId, int receivingId, int mNextChunkIndex,
                                byte[] outputBuffer) throws NotAuthenticatedException, PINCreationFailedException, PINNotCreatedException, IOException {
            this();
            this.mState = mState;
            this.mSendingOTPId = sendingId;
            this.mReceivingOTPId = receivingId;
            this.mNextChunkIndex = mNextChunkIndex;
            this.mBuffer = outputBuffer;
        }

        public SyncManagerStats load() throws NotAuthenticatedException, PINCreationFailedException, PINNotCreatedException, IOException {
            int sendingOTPId = mSharedPreferences.get(SENDING_OTP_KEY, 0);
            int receivingOTPId = mSharedPreferences.get(RECEIVING_OTP_KEY, 0);
            int nextChunkIndex = mSharedPreferences.get(NEXT_CHUNK_INDEX_KEY, -1);
            int state = mSharedPreferences.get(STATE_KEY, RECEIVING);
            byte[] buffer = loadBuffer();
            return new SyncManagerStats(state, sendingOTPId, receivingOTPId, nextChunkIndex, buffer);
        }

        public void save() throws CouldNotWriteToSharedPreferencesException, IOException, PINCreationFailedException, NotAuthenticatedException, PINNotCreatedException {
            mSharedPreferences.put(STATE_KEY, mState);
            mSharedPreferences.put(SENDING_OTP_KEY, mSendingOTPId);
            mSharedPreferences.put(RECEIVING_OTP_KEY, mReceivingOTPId);
            mSharedPreferences.put(NEXT_CHUNK_INDEX_KEY, mNextChunkIndex);
            saveBuffer(mBuffer);
        }

        private void saveBuffer(byte[] mBuffer) throws IOException, NotAuthenticatedException, PINNotCreatedException, PINCreationFailedException {
            FileOutputStream fileOutputStream = new FileOutputStream(mOutputBufferFile);
            fileOutputStream.write(LocalEncryption.getInstance(mParentActivity).encrypt(mBuffer));
            fileOutputStream.close();
        }

        private byte[] loadBuffer() throws NotAuthenticatedException, PINNotCreatedException, PINCreationFailedException, IOException {
            byte[] buffer = new byte[OTPEncryptionManager.PHYSICAL_OTP_BLOCK_SIZE];
            try {
                FileInputStream fileInputStream = new FileInputStream(mOutputBufferFile);
                int bytesIn = fileInputStream.read(buffer);
                if(bytesIn > 0) {
                    return LocalEncryption.getInstance(mParentActivity).decrypt(
                            Arrays.copyOfRange(buffer, 0, bytesIn));
                } else {
                    return null;
                }
            } catch(FileNotFoundException e) {
                return null;
            }
        }

        public int getState() {
            return mState;
        }

        public int getSendingOTPId() {
            return mSendingOTPId;
        }

        public int getReceivingOTPId() {
            return mReceivingOTPId;
        }

        public int getNextChunkIndex() {
            return mNextChunkIndex;
        }

        public byte[] getBuffer() {
            return mBuffer;
        }

        public void clear() throws CouldNotWriteToSharedPreferencesException {
            mSharedPreferences.clear();
            mOutputBufferFile.delete();
        }
    }
}
