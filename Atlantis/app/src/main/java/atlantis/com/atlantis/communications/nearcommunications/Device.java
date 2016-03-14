package atlantis.com.atlantis.communications.nearcommunications;

/**
 * Created by jvronsky on 3/3/15.
 */
public class Device {

    private final String deviceName;
    private final String deviceAddress;

    public Device(String deviceName, String deviceAddress) {
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    @Override
    public String toString() {
        return deviceName + "\n" + deviceAddress;
    }
}
