package atlantis.com.model.impls;

import java.security.SecureRandom;

import atlantis.com.atlantis.utils.DebugFlags;
import atlantis.com.atlantis.utils.LogUtils;

/**
 * Created by Daniel on 2/25/2015.
 */
public class SecureRandomGenerator {

    //used to access file directory to write OTP
    private final SecureRandom sr;

    private static final byte DEBUG_START_VALUE = 119;
    private static byte mDebugValue = DEBUG_START_VALUE;

    public SecureRandomGenerator(){
        sr = new SecureRandom();
    }

    public byte[] getBytes(int size){
        byte[] output = new byte[size];
        sr.nextBytes(output);

        if(DebugFlags.USE_SIMPLE_RANDOM) {
            for(int i = 0; i < size; i++) {
                output[i] = mDebugValue++;
            }
        }

        LogUtils.logBytes("Secure Random", output, 100);
        return output;
    }

    /**
     *  gets a single random integer using SecureRandom's built in function.
     *
     * @return a single random integer
     */

    public Integer getInteger(){
        Integer output = sr.nextInt();
        if(output < 0){
            output = output * -1;
        }
        return output;
    }
}
