package atlantis.com.atlantis.encryption;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;
import atlantis.com.model.Notebook;
import atlantis.com.model.impls.FileOTPManager;

/**
 * Created by jvronsky on 5/31/15.
 * Pool manager takes care of changing the pin.
 */
public class ChangePINPoolManager {

    private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final int WORKERS_PER_POOL = 2;
    static ChangePINPoolManager mInstance;
    private ThreadPoolExecutor mThreadPoolExecutor;
    private String mPIN;

    private ChangePINPoolManager() {
        mThreadPoolExecutor = new ThreadPoolExecutor(NUMBER_OF_CORES * WORKERS_PER_POOL,
                NUMBER_OF_CORES * WORKERS_PER_POOL,
                60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    }

    public static ChangePINPoolManager getInstance() {
        if(mInstance == null) {
            mInstance = new ChangePINPoolManager();
            return mInstance;
        } else {
            return mInstance;
        }
    }

    public void setNewPin(String PIN) {
        this.mPIN = PIN;
    }

    /**
     * Adds all the OTP files and run the changing pin.
     * @param context
     * @param notebookList
     * @return
     * @throws InterruptedException
     */
    public List<Future<Boolean>> addAllOTPFilesAndRun(Context context, List<Notebook> notebookList) throws InterruptedException {
        List<ChangeFileEncryptionRunnable> tasks = new ArrayList<>(notebookList.size() * Notebook.OTP_COUNT_PER_NOTEBOOK);
        for(Notebook notebook : notebookList) {
            File sendingOTP = FileOTPManager.getOTPFileFromOTP(context, notebook.getSendingOTP());
            File receivingOTP = FileOTPManager.getOTPFileFromOTP(context, notebook.getReceivingOTP());
            tasks.add(new ChangeFileEncryptionRunnable(context, sendingOTP));
            tasks.add(new ChangeFileEncryptionRunnable(context, receivingOTP));
        }
        return runAll(tasks);
    }

    private List runAll(List tasks) throws InterruptedException {
        return mThreadPoolExecutor.invokeAll(tasks);
    }

    private class ChangeFileEncryptionRunnable implements Callable<Boolean> {

        private static final String TAG = "ChangeOTP";
        private static final String MODE = "rwd";
        private File mFile;
        private Context mContext;

        public ChangeFileEncryptionRunnable(Context context, File file) {
            this.mFile = file;
            this.mContext = context;
        }

        @Override
        public Boolean call() throws Exception {
            changeOTPEncryptionKey();
            return true;
        }


        private void changeOTPEncryptionKey() throws IOException, NotAuthenticatedException, PINNotCreatedException, PINCreationFailedException {
            RandomAccessFile otpFile = new RandomAccessFile(mFile, MODE);
            byte[] inBuffer = new byte[OTPEncryptionManager.PHYSICAL_OTP_BLOCK_SIZE];
            long previousPosition = 0;
            long currentPosition = 0;
            long bytesInFile = otpFile.length();
            long bytesRemaining = bytesInFile;
            LocalEncryption localEncryption = LocalEncryption.getInstance(mContext);
            otpFile.seek(0L);
            while(bytesRemaining > 0) {
                byte[] decryptedBytes;
                byte[] reencryptedBytes;
                int bytesIn = otpFile.read(inBuffer, 0, OTPEncryptionManager.PHYSICAL_OTP_BLOCK_SIZE);
                inBuffer = Arrays.copyOfRange(inBuffer, 0, bytesIn);
                bytesRemaining -= bytesIn;
                currentPosition = otpFile.getFilePointer();
                otpFile.seek(previousPosition);
                decryptedBytes = localEncryption.decrypt(inBuffer);
                reencryptedBytes = localEncryption.encryptWithPIN(decryptedBytes, mPIN);
                otpFile.write(reencryptedBytes);
                previousPosition = currentPosition;
            }
            otpFile.close();
        }
    }
}
