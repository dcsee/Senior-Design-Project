package atlantis.com.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by ricardo on 5/31/15.
 * Class for recording which messages still need to be sent to which notebooks, including
 * storage for the Cipher Message sent
 */
@DatabaseTable
public class OutgoingMessage extends Model {

    public static final String ID_FIELD_NAME = "id";
    @DatabaseField(generatedId = true, columnName = ID_FIELD_NAME)
    private int mId;

    public static final String MESSAGE_FIELD_NAME = "message";
    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true, columnName = MESSAGE_FIELD_NAME)
    private Message mMessage;

    public static final String NOTEBOOK_FIELD_NAME = "notebook";
    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true, columnName = NOTEBOOK_FIELD_NAME)
    private Notebook mNotebook;

    public static final String CIPHER_MESSAGE_FIELD_NAME = "cipher_message";
    @DatabaseField(canBeNull = true, foreign = true, foreignAutoRefresh = true, columnName = CIPHER_MESSAGE_FIELD_NAME)
    private CipherMessage mCipherMessage;

    public CipherMessage getCipherMessage() {
        return mCipherMessage;
    }

    public void setCipherMessage(CipherMessage cipherMessage) {
        this.mCipherMessage = cipherMessage;
    }

    public Message getMessage() {
        return mMessage;
    }

    public void setMessage(Message message) {
        this.mMessage = message;
    }

    public Notebook getNotebook() {
        return mNotebook;
    }

    public void setNotebook(Notebook notebook) {
        this.mNotebook = notebook;
    }
}
