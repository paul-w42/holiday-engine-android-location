package net.nwnetsolutions.holidayengine;

/**
 * A Singleton that saves state between TrackSantaService and TrackSantaActivity.  Allows us
 * to post regular updates on service progress.
 *
 * To initialize, call setStartTime() - sets all variables to zero
 *
 * For each update call:
 *      setLastUpdateTime()         -- Updates lastUpdateTime to current time
 *      (CALLED BY setLastUpdateTime()) - incrementTotalUpdates()     -- Increments counter for updates performed
 *
 * For each Activity/Display cycle:
 *      getTotalTime()
 *      getLastUpdateTime()
 *      getTotalUpdates()
 *      getLongestUpdate()
 */

class ActivityButtonServiceProxy {

    // @+id/textView_last_update
    // @+id/textView_total_updates

    public static Long startTime;
    public static Long lastUpdateTime;
    public static Integer totalUpdates;
    public static Long longestUpdateInterval;
    public static Boolean recording = false;


    public static void setRecording(Boolean r) {
        recording = r;
    }

    /**
     * Called by TrackSantaActivity to set service starting time and initialize additional
     * variables.  Used to calculate totalTime later.
     * @called-by TrackSantaActivity
     */
    public static void setStartTime() {
        startTime = System.currentTimeMillis();
        lastUpdateTime = startTime;
        totalUpdates = 0;
        longestUpdateInterval = (long)1;
        recording = true;
    }

    /**
     * Called by TrackSantaActivity to display the total time service has been running.
     * @return String value formatted to show total time, ex. 2h 34m 12s
     * @called-by TrackSantaActivity
     */
    public static String getTotalTime() {
        return formatTime(System.currentTimeMillis() - startTime);
    }

    /**
     *  Called by TrackSantaActivity to display last update time
     * @return String
     * @called-by TrackSantaActivity
     */
    /*public static String getLastUpdateTime() {
        return formatTime(lastUpdateTime);
    }*/


    /**
     * Called by TrackSantaActivity to display time since last update
     * @return String formatted time
     * @called-by TrackSantaActivity
     */
    public static String getTimeSinceLastUpdate() {
        return formatTime(System.currentTimeMillis() - lastUpdateTime);
    }

    /**
     * Called by TrackSantaService to set the last time location was updated
     * @called-by TrackSantaService
     */
    public static void setLastUpdateTime() {
        if (!recording) {
            setStartTime();
        }
        ActivityButtonServiceProxy.lastUpdateTime = System.currentTimeMillis();
        incrementTotalUpdates();
    }

    /**
     * Called by TrackSantaActivity to display total location updates made so far
     * @return String
     * @called-by TrackSantaActivity
     */
    public static String getTotalUpdates() {
        return Integer.toString(totalUpdates);
    }

    /**
     * Called by setLastUpdateTime() to increment the number of location updates made
     */
    private static void incrementTotalUpdates() {
        ActivityButtonServiceProxy.totalUpdates += 1;
    }

    /**
     * Called by TrackSantaActivity to display longest update interval.  First checks to see if
     * current update interval is longest, then returns the longest update interval formatted for
     * output.
     * @return String
     * @called-by TrackSantaActivity
     */
    public static String getLongestUpdate() {

        if (longestUpdateInterval < (System.currentTimeMillis() - lastUpdateTime)) {
            longestUpdateInterval = System.currentTimeMillis() - lastUpdateTime;
        }

        return formatTime(longestUpdateInterval);
    }

    /**
     * Private method called locally to check/update if this is the longest update
     * interval
     */
    private static void checkLongestUpdate() {
        if ((System.currentTimeMillis()-lastUpdateTime) > longestUpdateInterval) {
            longestUpdateInterval = System.currentTimeMillis() - lastUpdateTime;
        }
    }

    /**
     * Private method - formats long time values to string.  i.e. given long value of 9000 returns
     * "9s", and for a long value of 9999999 returns "2h 46m 39s"
     * @param ms
     * @return
     */
    private static String formatTime(long ms) {

        long s = ms/1000 % 60 % 60;
        long m = ms/60000 % 60;
        long h = ms/3600000;

        String result = "";

        if (h > 0) {
            result = Long.toString(h) + "h ";
        }

        if (h > 0 || m > 0) {
            result = result + Long.toString(m) + "m ";
        }

        result = result + Long.toString(s) + "s";

        return result;
    }

}
