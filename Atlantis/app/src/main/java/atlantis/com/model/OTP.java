package atlantis.com.model;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
/**
 * Created by Andrew on 2/13/2015.
 */
@DatabaseTable
public class OTP extends Model {

    public static final String ID_FIELD_NAME = "id";
    @DatabaseField(generatedId = true, columnName = ID_FIELD_NAME)
    private int id;

    @DatabaseField
    private int dataId;

    @DatabaseField
    private int length;

    @DatabaseField
    private int position;

    public int getDataId() {
        return dataId;
    }

    public void setDataId(int dataId) {
        this.dataId = dataId;
    }

    public double getPercentageLeft() {
        if(length == 0) {
            return 0;
        } else {
            return 1 - position/(double)length;
        }
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getPosition() {
        return position;
    }
}
