package atlantis.com.harvester.analysis;

import atlantis.com.atlantis.utils.BytesUtils;

/**
 * Created by jvronsky on 5/23/15.
 * Shannon entropy class computes shannon entropy of byte arrays.
 */
public class ShannonEntropy {

    // All possible values of a byte.
    private static final int POSSIBLE_BYTE_VALUES_COUNT = 256;
    // Frequencies used for calculating entropy.
    private float[] frequencies;

    public ShannonEntropy() {
        frequencies = new float[POSSIBLE_BYTE_VALUES_COUNT];
        for(int i = 0; i < POSSIBLE_BYTE_VALUES_COUNT; i++) {
            frequencies[i] = 0;
        }
    }

    /**
     * Compute the number of bytes of entropy from an array of bytes.
     * @param bytes to compute entropy on
     * @return number of bytes of entropy
     */
    public float bytesOfEntropy(byte[] bytes) {
        computeFrequencies(bytes);
        return bytesOfEntropy();
    }

    /**
     * Compute bytes of entropy from the frequencies.
     * @return bytes of entropy
     */
    private float bytesOfEntropy() {
        float sum = 0.0F;
        for(float p : frequencies) {
            sum += p * log2(p);
        }
        return sum * -1.0F;
    }

    private float log2(float p) {
        return (float) (Math.log10(p) / Math.log10(2));
    }

    /**
     * Compute the frequencies of bytes in the array.
     * @param bytes to compute frequencies on
     */
    private void computeFrequencies(byte[] bytes) {
        float totalSize = bytes.length;
        for(byte b : bytes) {
            // Turn byte to int.
            int index = BytesUtils.byteToInt(b);
            frequencies[index] += 1.0F / totalSize;
        }
    }
}
