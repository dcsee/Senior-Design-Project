package atlantis.com.model.impls;

import android.content.Context;

import java.io.File;
import java.io.IOException;

import atlantis.com.atlantis.encryption.OTPEncryptionManager;
import atlantis.com.atlantis.encryption.PINCreationFailedException;
import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;
import atlantis.com.model.OTP;

/**
 * Created by jvronsky on 5/2/15.
 */
public class OTPFileInputStream {

    private final File otpFile;
    private int index;
    private OTPEncryptionManager mOTPEncryptionManager;
    private final int fileSize;

    public OTPFileInputStream(Context context, OTP otp) throws NotAuthenticatedException, PINNotCreatedException {
        otpFile = FileOTPManager.getOTPFileFromOTP(context, otp.getDataId());
        index = 0;
        fileSize = otp.getLength();
        mOTPEncryptionManager = new OTPEncryptionManager(context);
    }

    /**
     * Read OTP into the buffer.
     * @param buffer OTP will be read into
     * @return number of bytes read
     * @throws InvalidBufferException
     * @throws IOException
     * @throws PINCreationFailedException
     */
    public int read(byte[] buffer) throws InvalidBufferException, IOException, PINCreationFailedException {
        if(buffer == null || buffer.length == 0) {
            throw new InvalidBufferException("Buffer was not initialized or size 0");
        }
        int length = buffer.length;
        if(index >= fileSize) {
            return -1;
        }
        if((fileSize - index) < buffer.length) {
            length = fileSize - index;
        }
        byte[] input = mOTPEncryptionManager.decryptedOTP(otpFile, index, length);
        System.arraycopy(input, 0, buffer, 0, input.length);
        index += length;
        return input.length;
    }

    /**
     * Skips bytes in the file.
     * @param bytesToSkip
     */
    public void skip(int bytesToSkip) {
        index += bytesToSkip;
    }

    /**
     * Reset the input stream to read from the beginning.
     */
    public void reset() {
        this.index = 0;
    }

    public class InvalidBufferException extends Exception {

        public InvalidBufferException(String s) {
            super(s);
        }
    }

}
