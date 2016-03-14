package atlantis.com.atlantis.communications.nearcommunications;

/**
 * Created by jvronsky on 5/18/15.
 * BadSync meta data exception thrown when sync started with illegal arguments.
 */
public class BadSyncDataException extends Throwable {
    public BadSyncDataException(String s) {
        super(s);
    }
}
