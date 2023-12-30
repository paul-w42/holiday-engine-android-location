package net.nwnetsolutions.holidayengine.constants;

/**
 * This class contains String constants that would change depending on user/organization.  This
 * allows for easy customization in just one place to enable usage (or testing) with alternative
 * parameters.
 *
 * This class also contains a helper function to compare a passed in String w/ a given Hash
 *
 * @author Paul Woods
 */
public class Constants {

    // used by LoginActivity.onClick(login_button), hard-coded password
    private static String password = "SHA-256 hash / 64-characters";

    // used by HolidayEngineUpdateActivity.onCreate(), returns number of engines
    private static String enginesUrl = "";              // REST api path

    // used by TrackSantaService.onStartCommand().serverStartTracking()
    private static String startTrackingUrl = "";        // REST api path

    // used by TrackSantaService.onDestroy(), i.e. user has stopped tracking
    private static String stopTrackingUrl = "";         // REST api path

    // used by TrackSantaService.onLocationChanged()
    private static String updateLocationUrl = "";       // REST api path

    public static String getUpdateLocationUrl() {
        return updateLocationUrl;
    }

    public static String getStopTrackingUrl() {
        return stopTrackingUrl;
    }

    public static String getStartTrackingUrl() {
        return startTrackingUrl;
    }

//    public static String getUpdateSiteUrl() {
//        return updateSiteUrl;
//    }

    public static String getEnginesUrl() {
        return enginesUrl;
    }

    public static String getPassword() {
        return password;
    }

}
