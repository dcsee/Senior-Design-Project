package atlantis.com.atlantis.encryption;

/**
 * Created by jvronsky on 4/28/15.
 */
public class PINCreationFailedException extends Exception {

    public PINCreationFailedException(String detailMessage) {
        super(detailMessage);
    }
}
