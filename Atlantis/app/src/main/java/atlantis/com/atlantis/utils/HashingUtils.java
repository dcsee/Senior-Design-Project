package atlantis.com.atlantis.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by jvronsky on 1/23/15.
 */
public class HashingUtils {


    public static final int CHECKSUM_LENGTH_IN_BYTES = 20;
    private static final String CHECKSUM_ALGORITHM = "SHA1";
    private static final String SHA2_ALGORITHM = "SHA-256";


    /**
     * Generates checksum for the data.
     * @param data
     * @return checksum bytes
     * @throws java.security.NoSuchAlgorithmException
     */
    public static byte[] generateCheckSum(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
        return messageDigest.digest(data);
    }

    /**
     * Compute sha2.
     * @param bytes to hash
     * @return the hashed bytes
     * @throws NoSuchAlgorithmException
     */
    public static byte[] sha2(byte[] bytes) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance(SHA2_ALGORITHM);
        return messageDigest.digest(bytes);
    }
}
