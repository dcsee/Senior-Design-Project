package atlantis.com.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

/**
 * The model for a message
 */
@DatabaseTable
public class Message extends Model {

    @DatabaseField(generatedId = true)
    private int mId;

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    private byte[] mContent;

    @DatabaseField
    private boolean mDelivered;

    @DatabaseField
    private Date mTimestamp;

    @DatabaseField(foreign = true, foreignAutoRefresh = true)
    private Conversation mConversation;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    private Person mSender;

    public Person getSender() {
        return mSender;
    }

    public void setSender(Person sender) {
        this.mSender = sender;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        this.mId = id;
    }

    public void setSerializedContent(MessageContent messageContent) {
        this.mContent = messageContent.toByteArray();
    }

    public boolean isDelivered() {
        return mDelivered;
    }

    public void setDelivered(boolean delivered) {
        this.mDelivered = delivered;
    }

    public MessageContent getSerializedContent() {
        return new MessageContent(mContent);
    }

    public byte[] getContent() {
        return mContent;
    }

    public void setContent(byte[] content) {
        this.mContent = content;
    }

    public void setConversation(Conversation conversation) {
        this.mConversation = conversation;
    }

    public Conversation getConversation() {
        return mConversation;
    }

    public void setTimestamp(){
        mTimestamp = new Date();
    }

    public Date getTimestamp(){
        return mTimestamp;
    }

}
