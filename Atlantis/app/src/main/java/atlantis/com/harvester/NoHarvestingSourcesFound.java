package atlantis.com.harvester;

/**
 * Created by jvronsky on 5/23/15.
 * Exception for when no harvesting sources were found.
 */
public class NoHarvestingSourcesFound extends Exception {
    public NoHarvestingSourcesFound(String s) {
        super(s);
    }
}
