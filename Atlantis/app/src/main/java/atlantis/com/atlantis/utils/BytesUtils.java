package atlantis.com.atlantis.utils;

/**
 * Created by jvronsky on 1/26/15.
 */
public class BytesUtils {

    // Used to mask a byte to an int.
    private static final int BYTE_TO_INT_CONVERSION = 0xff;

    /**
     * Converts array of bytes to an int.
     * @param n array of byte to convert
     * @return numerical equivalent
     */
    public static int byteArrayToInt(byte[] n) {
        return   n[3] & 0xFF |
                (n[2] & 0xFF) << 8 |
                (n[1] & 0xFF) << 16 |
                (n[0] & 0xFF) << 24;
    }

    /**
     * Turn int into array of bytes.
     * @param n number to transform
     * @return byte representation
     */
    public static byte[] intToByteArray(int n) {
        return new byte[] {
                (byte) ((n >> 24) & 0xFF),
                (byte) ((n >> 16) & 0xFF),
                (byte) ((n >> 8) & 0xFF),
                (byte) (n & 0xFF)
        };
    }

    public static int byteToInt(byte b) {
        return b & BYTE_TO_INT_CONVERSION;
    }
}
