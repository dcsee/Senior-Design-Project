package atlantis.com.harvester.harvestSource;

/**
 * Created by jvronsky on 5/23/15.
 * General interface for harvesting source.
 */
public interface HarvestSource {

    /**
     * Return bytes collected by the source.
     * @return bytes collected
     */
    byte[] getBytesFromSource();

    /**
     * Start collecting entropy.
     */
    void startCollecting();

    /**
     * Stop collecting entropy.
     */
    void stopCollectingData();

}
