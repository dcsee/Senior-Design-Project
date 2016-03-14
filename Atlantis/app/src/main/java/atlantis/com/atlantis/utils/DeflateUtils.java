package atlantis.com.atlantis.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;

/**
 * Created by jvronsky on 5/24/15.
 * DeflateUtils help compressing data.
 */
public class DeflateUtils {

    /**
     * Deflate the data passed in.
     * @param data data to compress
     * @return deflated data
     * @throws IOException
     */
    public static byte[] deflateData(byte[] data, int length) throws IOException {
        ByteArrayOutputStream compressedData = new ByteArrayOutputStream();
        DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(compressedData);
        deflaterOutputStream.write(data, 0, length);
        deflaterOutputStream.close();
        return compressedData.toByteArray();
    }
}
