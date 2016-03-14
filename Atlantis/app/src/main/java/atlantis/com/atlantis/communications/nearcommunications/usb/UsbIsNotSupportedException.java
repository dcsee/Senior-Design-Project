package atlantis.com.atlantis.communications.nearcommunications.usb;

/**
 * Created by jvronsky on 5/19/15.
 * Exception for when Usb is not supported.
 */
public class UsbIsNotSupportedException extends Exception {

    public UsbIsNotSupportedException(String detailMessage) {
        super(detailMessage);
    }
}
