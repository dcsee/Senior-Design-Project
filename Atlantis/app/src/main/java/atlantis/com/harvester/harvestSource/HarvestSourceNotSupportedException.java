package atlantis.com.harvester.harvestSource;

/**
 * Created by jvronsky on 5/23/15.
 * Custom Exception for harvesting.
 */
public class HarvestSourceNotSupportedException extends Throwable {
    public HarvestSourceNotSupportedException(String s) {
        super(s);
    }
}
