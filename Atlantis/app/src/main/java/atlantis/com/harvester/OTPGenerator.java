package atlantis.com.harvester;

import android.content.Context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import atlantis.com.atlantis.utils.DebugFlags;
import atlantis.com.atlantis.utils.DeflateUtils;
import atlantis.com.atlantis.utils.HashingUtils;
import atlantis.com.harvester.analysis.ShannonEntropy;

/**
 * Created by jvronsky on 5/23/15.
 * OTP generator turn raw data to OTP.
 */
public class OTPGenerator {

    private static final String TAG = "OTPGenerator";

    // 64 bytes = 512 bits.
    private static final int SIZE_TO_HASH_IN_BYTES = 32;
    private static final int IV_LENGTH = 16;
    // AES mKey of 256 bits = 32 bytes
    private static final int KEY_LENGTH = 32;
    private static final int MIN_BUFFER_SIZE_TO_WRITE = SIZE_TO_HASH_IN_BYTES + IV_LENGTH + 1024;
    // Encryption algorithm used to create encryption mKey.
    private static final String KEY_SPEC_ALGORITHM = "AES";
    // Encrpytion algorithm used.
    private static final String ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5PADDING";
    // Collected Entropy file.
    private static final String ENTROPY_DIRECTORY = "entropy";
    private static final String ENTROPY_FILE_PATH = "entropyFile";
    private static final String TEMP_ENTROPY_FILE_PATH = "tempEntropyFile";

    private final File mEntropyFile;
    private final File mTempEntropyFile;
    private final ByteArrayOutputStream mPreMixingBuffer;
    private int mBufferSize;
    private long mAvailableOTP;
    private final byte[] mKey = new byte[KEY_LENGTH];
    private final byte[] mIV = new byte[IV_LENGTH];

    public OTPGenerator(Context context) throws FileNotFoundException {
        File entropyDirectory = context.getDir(ENTROPY_DIRECTORY, Context.MODE_PRIVATE);
        mEntropyFile = new File(entropyDirectory, ENTROPY_FILE_PATH);
        mTempEntropyFile = new File(entropyDirectory, TEMP_ENTROPY_FILE_PATH);
        mPreMixingBuffer = new ByteArrayOutputStream();
        mBufferSize = 0;
        mAvailableOTP = mEntropyFile.length();
    }

    /**
     * Add raw data to generate OTP from.
     * @param bytes to generate OTP from
     * @param length of the amount of bytes passed in.
     * @throws IOException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidAlgorithmParameterException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     */
    public void addRawData(byte[] bytes, int length) throws IOException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeyException, InvalidKeySpecException {
        byte[] buffer = hash(DeflateUtils.deflateData(bytes, length));
        mBufferSize += buffer.length;
        mPreMixingBuffer.write(buffer);
        if(mBufferSize > MIN_BUFFER_SIZE_TO_WRITE) {
            writeToFile();
        }
    }

    /**
     * Hash the data in 256bits blocks using SHA2
     * @param bytes to hash
     * @return the hashed version of the bytes.
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    private byte[] hash(byte[] bytes) throws NoSuchAlgorithmException, IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[SIZE_TO_HASH_IN_BYTES];
        int bytesRead = 0;
        while(bytesRead != -1) {
            bytesRead = byteArrayInputStream.read(buffer, 0, SIZE_TO_HASH_IN_BYTES);
            if(bytesRead == SIZE_TO_HASH_IN_BYTES) {
                byteArrayOutputStream.write(HashingUtils.sha2(buffer));
            } else if(bytesRead < SIZE_TO_HASH_IN_BYTES) {
                break;
            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Write the OTP to the file.
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeySpecException
     * @throws IOException
     */
    private void writeToFile() throws NoSuchPaddingException, InvalidKeyException,
            NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException,
            InvalidAlgorithmParameterException, InvalidKeySpecException, IOException {
        ShannonEntropy shannonEntropy = new ShannonEntropy();
        FileOutputStream entropyFileOutputStream = new FileOutputStream(mEntropyFile, true);
        ByteArrayInputStream byteArrayInputStream =
                new ByteArrayInputStream(mPreMixingBuffer.toByteArray());
        int lengthOfData = mBufferSize - KEY_LENGTH - IV_LENGTH;
        byte[] data = new byte[lengthOfData];
        // Set values for mixing.
        byteArrayInputStream.read(mKey, 0, KEY_LENGTH);
        byteArrayInputStream.read(mIV, 0, IV_LENGTH);
        byteArrayInputStream.read(data, 0, lengthOfData);
        byte[] finalData = mixWithAES(data, mIV, mKey);
        // Compute bytes of entropy.
        int bytesOfEntropy = (int) shannonEntropy.bytesOfEntropy(finalData);
        if(DebugFlags.FAKE_ENTROPY) {
            bytesOfEntropy = finalData.length;
        }
        mAvailableOTP += bytesOfEntropy;
        entropyFileOutputStream.write(finalData, 0, bytesOfEntropy);
        entropyFileOutputStream.flush();
        entropyFileOutputStream.close();
        reset();
    }

    private void reset() {
        mBufferSize = 0;
        mPreMixingBuffer.reset();
    }

    /**
     * Mix using AES/CBC
     * @param data to mix
     * @param iv to use to initiate mixing
     * @param key to use for mixing
     * @return the mixed data
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     * @throws BadPaddingException
     * @throws IllegalBlockSizeException
     */
    private byte[] mixWithAES(byte[] data, byte[] iv, byte[] key)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {
        byte[] encrypted = null;
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, KEY_SPEC_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
        encrypted = cipher.doFinal(data);
        return encrypted;
    }

    public long getAvailableOTP() {
        return mAvailableOTP;
    }

    /**
     * Get OTP from entropy file.
     * @param length of OTP requested.
     * @param buffer to store OTP in
     * @return the actual length returned
     * @throws IOException
     */
    public int getOTP(int length, byte[] buffer) throws IOException {
        FileInputStream entropyFile = new FileInputStream(mEntropyFile);
        int bytesRead = entropyFile.read(buffer, 0, length);
        rewriteEntropyFile(entropyFile);
        mAvailableOTP -= bytesRead;
        return bytesRead;
    }

    /**
     * After user gets OTP rewrite the entropy file to not have the old OTP.
     * @param entropyFile InputFileStream after OTP was read
     * @throws IOException
     */
    private void rewriteEntropyFile(FileInputStream entropyFile) throws IOException {
        FileOutputStream tempFile = new FileOutputStream(mTempEntropyFile);
        int bytesIn = 0;
        byte[] buffer = new byte[MIN_BUFFER_SIZE_TO_WRITE];
        while(bytesIn != -1) {
            bytesIn = entropyFile.read(buffer, 0, MIN_BUFFER_SIZE_TO_WRITE);
            if(bytesIn > 0) {
                tempFile.write(buffer, 0, bytesIn);
            }
        }
        tempFile.close();
        FileInputStream fileInputStream = new FileInputStream(mTempEntropyFile);
        FileOutputStream originalFileOutputStream = new FileOutputStream(mEntropyFile);
        bytesIn = 0;
        while(bytesIn != -1) {
            bytesIn = fileInputStream.read(buffer, 0, MIN_BUFFER_SIZE_TO_WRITE);
            if(bytesIn > 0) {
                originalFileOutputStream.write(buffer, 0, bytesIn);
            }
        }
        originalFileOutputStream.close();
    }
}
