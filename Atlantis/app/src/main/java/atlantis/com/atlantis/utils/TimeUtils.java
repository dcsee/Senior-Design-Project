package atlantis.com.atlantis.utils;

/**
 * Created by jvronsky on 5/18/15.
 * Utils to do time conversions
 */
public class TimeUtils {

    public static final int SECONDS_IN_A_MINUTE = 60;
    public static final int MILLI_SECONDS_IN_A_SECOND = 1000;

    public static int minutesToSeconds(int minutes) {
        return minutes * SECONDS_IN_A_MINUTE;
    }

    public static long secondsToMilliseconds(int seconds) {
        return seconds * MILLI_SECONDS_IN_A_SECOND;
    }

    public static long minutesToMiliseconds(int minutes) {
        return secondsToMilliseconds(minutesToSeconds(minutes));
    }

    public static int timeDifferenceSeconds(int t1, int t2) {
        return t2 - t1;
    }

    public static long timeDifferenceMiliSeconds(long t1, long t2) {
        return t2 - t1;
    }
}
