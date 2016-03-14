package atlantis.com.model;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by Andrew on 2/13/2015.
 */
@DatabaseTable
public class Conversation extends Model {

    public static final String ID_FIELD_NAME = "id";
    @DatabaseField(generatedId = true, columnName = ID_FIELD_NAME)
    private int mId;

    @DatabaseField
    private String mName;

    @ForeignCollectionField(eager = true)
    private ForeignCollection<Message> mMessages;

    @ForeignCollectionField(eager = true)
    private ForeignCollection<Notebook> mNotebooks;

    @DatabaseField(canBeNull = false, foreign = true, foreignAutoRefresh = true)
    private Person mSelf;

    @DatabaseField
    private String mDescription;

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String mDescription) {
        this.mDescription = mDescription;
    }

    public void setId(int id){
        this.mId = id;
    }

    public int getId(){
        return mId;
    }

    public void setName(String name){
        this.mName = name;
    }

    public String getName(){
        return mName;
    }

    public void setMessages(ForeignCollection<Message> messages){
        this.mMessages = messages;
    }

    public ForeignCollection<Message> getMessages() {
        return mMessages;
    }

    public ForeignCollection<Notebook> getNotebooks() {
        return mNotebooks;
    }

    public void setNotebooks(ForeignCollection<Notebook> notebooks) {
        this.mNotebooks = notebooks;
    }

    public Person getSelf() {
        return mSelf;
    }

    public void setSelf(Person self) {
        this.mSelf = self;
    }
}
