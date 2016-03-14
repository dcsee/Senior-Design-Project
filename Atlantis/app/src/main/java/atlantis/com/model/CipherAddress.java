package atlantis.com.model;

/**
 * Created by ricardo on 5/24/15.
 * Helper class for holding an address as a type
 */
public class CipherAddress {

    private byte[] mAddress;

    public CipherAddress(byte[] address) {
        this.mAddress = address;
    }

    public byte[] getBytes() {
        return mAddress;
    }
}
