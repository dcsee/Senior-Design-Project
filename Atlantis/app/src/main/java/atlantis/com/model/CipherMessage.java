package atlantis.com.model;

import android.util.Base64;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * The model for temporarily storing cipher messages
 */
@DatabaseTable
public class CipherMessage {

    public static final String ID_FIELD_NAME = "id";
    @DatabaseField(generatedId = true, columnName = ID_FIELD_NAME)
    private int mId;

    public static final String ADDRESS_BYTES_FIELD_NAME = "address_bytes";
    @DatabaseField(dataType = DataType.BYTE_ARRAY, columnName = ADDRESS_BYTES_FIELD_NAME)
    private byte[] mAddressBytes;

    public static final String ENCODED_ADDRESS_FIELD_NAME = "encoded_address";
    @DatabaseField(columnName = ENCODED_ADDRESS_FIELD_NAME)
    private String mEncodedAddress;

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    private byte[] mContent;

    public int getId() {
        return mId;
    }

    public byte[] getAddressBytes() {
        return mAddressBytes;
    }

    public void setAddressBytes(byte[] address) {
        this.mAddressBytes = address;
        this.mEncodedAddress = CipherMessage.encodeAddressBytesForLookup(address);
    }

    public static String encodeAddressBytesForLookup(byte[] address) {
        return Base64.encodeToString(address, 0);
    }

    public byte[] getContent() {
        return mContent;
    }

    public void setContent(byte[] content) {
        this.mContent = content;
    }
}
