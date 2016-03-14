package atlantis.com.model;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by ricardo on 4/21/15.
 * The class holding the state of the notebook including OTPs, outgoing messages, and Acks.
 */
@DatabaseTable
public class Notebook extends Model {

    public static final int OTP_COUNT_PER_NOTEBOOK = 2;

    public static final String ID_FIELD_NAME = "id";
    @DatabaseField(generatedId = true, columnName = ID_FIELD_NAME)
    private int mId;

    public static final String CONVERSATION_FIELD_NAME = "conversation";
    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true, columnName = CONVERSATION_FIELD_NAME)
    private Conversation mConversation;

    public static final String CONTACT_FIELD_NAME = "contact";
    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true, columnName = CONTACT_FIELD_NAME)
    private Person mContact;

    public static final String RECEIVING_OTP_FIELD_NAME = "receiving";
    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true, columnName = RECEIVING_OTP_FIELD_NAME)
    private OTP mReceivingOTP;

    public static final String SENDING_OTP_FIELD_NAME = "sending";
    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true, columnName = SENDING_OTP_FIELD_NAME)
    private OTP mSendingOTP;

    public static final String OUTGOING_MESSAGES = "outgoing_messages";
    @ForeignCollectionField(orderColumnName = OutgoingMessage.ID_FIELD_NAME, orderAscending = true, columnName = OUTGOING_MESSAGES)
    private ForeignCollection<OutgoingMessage> mOutgoingMessages;

    public static final String LAST_ACK_FIELD_NAME = "last_ack";
    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true, columnName = LAST_ACK_FIELD_NAME)
    private CipherMessage mLastAck;

    @DatabaseField(canBeNull = true, dataType = DataType.BYTE_ARRAY)
    private byte[] mLastAckAddress;

    public ForeignCollection<OutgoingMessage> getOutgoingMessages() {
        return mOutgoingMessages;
    }

    public CipherMessage getLastAck() {
        return mLastAck;
    }

    public void setLastAck(CipherMessage lastAck) {
        this.mLastAck = lastAck;
    }

    public byte[] getLastAckAddressBytes() {
        return mLastAckAddress;
    }

    public void setLastAckAddress(byte[] lastAckAddress) {
        this.mLastAckAddress = lastAckAddress;
    }

    public Conversation getConversation() {
        return mConversation;
    }

    public void setConversation(Conversation conversation) {
        this.mConversation = conversation;
    }

    public Person getContact() {
        return mContact;
    }

    public void setContact(Person contact) {
        this.mContact = contact;
    }

    public OTP getReceivingOTP() {
        return mReceivingOTP;
    }

    public void setReceivingOTP(OTP receivingOTP) {
        this.mReceivingOTP = receivingOTP;
    }

    public OTP getSendingOTP() {
        return mSendingOTP;
    }

    public void setSendingOTP(OTP sendingOTP) {
        this.mSendingOTP = sendingOTP;
    }

    public boolean getIsSynced() {
        return this.mReceivingOTP.getLength() > 0;
    }

    public int getId() {
        return mId;
    }
}
