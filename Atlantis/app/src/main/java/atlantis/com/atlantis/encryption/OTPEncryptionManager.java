package atlantis.com.atlantis.encryption;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

import atlantis.com.atlantis.encryption.exceptions.NotAuthenticatedException;
import atlantis.com.atlantis.encryption.exceptions.PINNotCreatedException;

/**
 * Created by jvronsky on 4/26/15.
 */
public class OTPEncryptionManager {

    // The bytes of actual OTP held in a block.
    public static final int MAX_VIRTUAL_OTP_BLOCK_SIZE = 1024;
    // Due to encryption the block will have different physical size.
    public static final int PHYSICAL_OTP_BLOCK_SIZE =
            LocalEncryption.lengthOfEncryptedData(MAX_VIRTUAL_OTP_BLOCK_SIZE);

    private LocalEncryption mLocalEncryption;

    public OTPEncryptionManager(Context context) throws NotAuthenticatedException, PINNotCreatedException {
        mLocalEncryption = LocalEncryption.getInstance(context);
    }

    public byte[] encryptOTP(byte[] otp) throws PINCreationFailedException {
        return mLocalEncryption.encrypt(otp);
    }

    /**
     * Decrypts OTP from file.
     * @param file to decrypt the OTP from
     * @param index in the file to start the access
     * @param length of the OTP wanted
     * @return
     * @throws PINCreationFailedException
     * @throws IOException
     */
    public byte[] decryptedOTP(File file, int index, int length) throws PINCreationFailedException, IOException {
        byte[] decryptedOTP = new byte[length];
        int decryptedOTPIndex = 0;

        int totalBytesRemaining = length;
        byte[] buffer = new byte[PHYSICAL_OTP_BLOCK_SIZE];
        FileInputStream inputStream = new FileInputStream(file);

        int startingBlock = index / MAX_VIRTUAL_OTP_BLOCK_SIZE;
        inputStream.skip(startingBlock * PHYSICAL_OTP_BLOCK_SIZE);

        while(totalBytesRemaining > 0) {
            int inBlockIndex = index % MAX_VIRTUAL_OTP_BLOCK_SIZE;
            int blockIndex = index / MAX_VIRTUAL_OTP_BLOCK_SIZE;
            int bytesRemainingInBlock = MAX_VIRTUAL_OTP_BLOCK_SIZE - inBlockIndex;
            int bytesToReadFromBlock = Math.min(totalBytesRemaining, bytesRemainingInBlock);
            int bytesToEndOfFile = (int) (file.length() - (blockIndex * PHYSICAL_OTP_BLOCK_SIZE));
            byte[] decryptedBlock;

            inputStream.read(buffer);
            index += bytesToReadFromBlock;

            if(bytesToEndOfFile < PHYSICAL_OTP_BLOCK_SIZE) {
                decryptedBlock = mLocalEncryption.decrypt(
                        Arrays.copyOfRange(buffer, 0, bytesToEndOfFile));
            } else  {
                decryptedBlock = mLocalEncryption.decrypt(buffer);
            }

            System.arraycopy(decryptedBlock, inBlockIndex, decryptedOTP,
                    decryptedOTPIndex, bytesToReadFromBlock);
            decryptedOTPIndex += bytesToReadFromBlock;

            totalBytesRemaining -= bytesToReadFromBlock;
        }
        inputStream.close();

        return decryptedOTP;
    }
}
