package atlantis.com.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;


/**
 * Created by Andrew on 2/13/2015.
 */
@DatabaseTable
public class Person extends Model {

    @DatabaseField(generatedId = true)
    private int mId;

    @DatabaseField
    private String mLookupKey;

    @DatabaseField
    private String mDisplay;

    @DatabaseField
    private boolean mIsGoat;

    public int getId() {
        return mId;
    }

    public String getLookupKey() {
        return mLookupKey;
    }

    public void setLookupKey(String lookupKey) {
        this.mLookupKey = lookupKey;
    }

    public String getDisplay() {
        return mDisplay;
    }

    public void setDisplay(String display) {
        this.mDisplay = display;
    }

    public void setIsGoat(boolean isGoat) {
        this.mIsGoat = isGoat;
    }

    public boolean getIsGoat() {
        return mIsGoat;
    }
}
