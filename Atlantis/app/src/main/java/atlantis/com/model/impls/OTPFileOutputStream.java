package atlantis.com.model.impls;

import android.content.Context;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import atlantis.com.atlantis.encryption.LocalEncryption;
import atlantis.com.atlantis.encryption.OTPEncryptionManager;
import atlantis.com.atlantis.encryption.PINCreationFailedException;
import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;
import atlantis.com.model.OTP;

/**
 * Created by jvronsky on 5/2/15.
 */
public class OTPFileOutputStream {


    private FileOutputStream fileOutputStream;
    private byte[] outputBuffer;
    private int outputBufferIndex;
    private OTPEncryptionManager mOTPEncryptionManager;
    private int bytesWritten = 0;

    public OTPFileOutputStream(Context context, OTP otp, boolean append) throws FileNotFoundException, NotAuthenticatedException, PINNotCreatedException {
        fileOutputStream = new FileOutputStream(
                FileOTPManager.getOTPFileFromOTP(context, otp.getDataId()), append);
        outputBuffer = new byte[OTPEncryptionManager.MAX_VIRTUAL_OTP_BLOCK_SIZE];
        outputBufferIndex = 0;
        mOTPEncryptionManager = new OTPEncryptionManager(context);
    }

    public OTPFileOutputStream(Context context, OTP otp) throws NotAuthenticatedException, FileNotFoundException, PINNotCreatedException {
        // Default we do not append OTP.
        this(context, otp, false);
    }

    /**
     * Write to otp file. Equivalent to write(data, 0, data.length)
     * @param data buffer with the data to write
     */
    public void write(byte[] data) throws IOException,
            PINCreationFailedException, OTPFileOutputStreamException {
        write(data, 0, data.length);
    }

    /**
     * Write to otp file
     * @param data buffer with the data to write
     * @param start where in the data buffer to start reading
     * @param length how many bytes to read from the start
     */
    public void write(byte[] data, int start, int length) throws IOException, PINCreationFailedException, OTPFileOutputStreamException {
        for(int i = start; i < start + length; i++) {
            outputBuffer[outputBufferIndex++] = data[i];
            if(outputBufferIndex == OTPEncryptionManager.MAX_VIRTUAL_OTP_BLOCK_SIZE) {
                write();
            }
        }
    }

    public void close() throws IOException, PINCreationFailedException, OTPFileOutputStreamException {
        if(outputBufferIndex != 0) {
            write();
        }
        fileOutputStream.close();
    }

    public void setOutputBuffer(byte[] bytes) {
        System.arraycopy(bytes, 0, outputBuffer, 0, bytes.length);
        outputBufferIndex = bytes.length;
    }

    public byte[] getOutputBuffer() {
        if(outputBufferIndex == 0) {
            return new byte[] {};
        } else {
            return Arrays.copyOfRange(outputBuffer, 0, outputBufferIndex);
        }
    }

    private void write() throws PINCreationFailedException, IOException, OTPFileOutputStreamException {
        byte[] bytesToWrite = mOTPEncryptionManager.encryptOTP(
                Arrays.copyOfRange(outputBuffer, 0, outputBufferIndex));
        if(bytesToWrite.length == LocalEncryption.lengthOfEncryptedData(outputBufferIndex)) {
            fileOutputStream.write(bytesToWrite);
            bytesWritten += bytesToWrite.length;
        } else {
            throw new OTPFileOutputStreamException("Failed to write due to encryption");
        }
        outputBufferIndex = 0;
    }


    public class OTPFileOutputStreamException extends Throwable {
        public OTPFileOutputStreamException(String s) {
            super(s);
        }
    }
}
