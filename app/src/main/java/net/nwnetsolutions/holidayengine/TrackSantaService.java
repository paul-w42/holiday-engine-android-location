package net.nwnetsolutions.holidayengine;

/*
 * NOTE: 
 * - Services do not run in separate threads, or separate processes.  Can spawn own thread though.
 * - IntentService class is available as standard implementation that has its own thread where it schedules work to be done
 * - Must have corresponding entry in manifest.xml
 * - START_STICKY .... ???
 * - WAKE_LOCK ....... ???
 * - AlarmManager - some seem to use this to schedule events in the future (i.e. 15 seconds).  Service wakes, aquires temporary wake lock, retrieves 
 * 			GPS Location, checks, transmits if required, schedules next alarm, release wake_lock.  Need to use wake_lock w/ this?
 * -
 * 	
 *  LINKS
 *  
 *  http://stackoverflow.com/questions/7765152/android-gps-tracking-and-wakelock
 *  
 *  http://stackoverflow.com/questions/3647446/how-to-prevent-sleeping-in-android
 *  
 *  http://stackoverflow.com/questions/7566026/updating-gps-location-from-background-android-doesnt-work-on-most-of-the-phones
 *  	Details a near-identical use case, and mentions possibility of not all phones behaving the same re location updates and sleep mode.
 *  
 *  http://stackoverflow.com/questions/5854921/android-service-running-in-background-without-sleep
 *  
 *  http://stackoverflow.com/questions/8202875/calculate-speed-between-two-location-points-in-android
 *  	Talks about using getSpeed() - states must use hasSpeed() 1st to determin if available.
 *  
 *  
 *  
 *  11/30/12 -----------------------------------------
 *  
 *  http://code.google.com/p/android/issues/detail?id=10931
 *  	Refers to no cell location updates w/ display off - refers to requestLocationUpdates as a workaround
 *  
 *  https://groups.google.com/forum/?fromgroups=#!topic/android-developers/-s8XtrOSZXo
 *  	Comment from Mark Murphy among others, talking about whether a global wake lock is required or not (ie. preventing device from sleeping)
 *  	NEXT STEP - GLOBAL WAKELOCK
 *  
 *  IDEAS
 *  	DONE - SEEMS TO WORK FOR 4.1 DEVICES ... Service -> startForeground & stopForeground --> system shouldn't kill service
 *  	AlarmManager or PartialWakeLock (on global level)
 *
 *   06/13/18 ----------------------------------------
 *
 *   https://developer.android.com/training/scheduling/wakelock
 *   WakeLock can be used to keep the CPU only on
 *
 *   Suggests using JobScheduler or FirebaseCloudMessaging as alternative
 *
 *   PowerManager - https://developer.android.com/reference/android/os/PowerManager
 *
 *
 *  7/24/18 - "On Android 8.0 (API level 26) and higher, if an app is running in the background when it requests the current
 *  location, then the device calculates the location only a few times each hour. To learn how to adapt your app to these
 *  calculation limits, see Background Location Limits."  https://developer.android.com/guide/topics/location/strategies
 *
 *
 *  11/22/19 - Updating for usage of new GoLang server.  Contains different API calls (start-tracking, send-location, and
 *  stop-tracking).
 *
 */

import android.Manifest.permission;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

//import net.nwnetsolutions.R;

import net.nwnetsolutions.holidayengine.constants.Constants;

import org.json.JSONObject;

/**
 * This is the Service that transmits the engines location on a regular basis.
 *
 * (2023)Currently works with 3 of 4 phones reliably.  4th phone, an old pixel, stops transmitting
 * location after about an hour.
 *
 * @author Paul Woods
 */
public class TrackSantaService extends Service implements LocationListener {

	private static final String TAG = "TrackSantaService";
	private static final int NOTIFICATION_ID = 4242;
	private static final String CHANNEL_ID = "engine-channel";
	public static String engineID = "";
	private static String tickerText = "";
    private static String contentText = "";
	public static final String ACTION_TRACK = "ACTION_TRACK";
    public static final String ACTION_RESET = "ACTION_RESET";
	static Boolean trackingSanta = false;
	LocationManager locationManager;
	private NotificationManager notificationManager;
	RequestQueue requestQueue;
	private NotificationCompat.Builder builder;
	private Notification notification;

	PowerManager powerManager;
	PowerManager.WakeLock wakeLock;


	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}


	@Override
	public void onCreate() {
		super.onCreate();

		// Request Queue for Volley Networking/JSON
		requestQueue = Volley.newRequestQueue(this);

		// Initialize WakeLock
		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EngineUpdateActivity:TrackSantaService");

		// Initialize Location Services
		locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
		Log.d(TAG, "onCreate(): Location Manager initialized");

		startInForeground();

		wakeLock.acquire();
	}

	/*
	 * startInForeground() prepares for and builds notification required to call startService & startForeground
	 *
	 * Called by onCreate()
	 */
	private void startInForeground() {

        Log.d(TAG, "startInForeground() called");

		if (notificationManager == null) {
			notificationManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
		}

		Intent intent;
		PendingIntent pendingIntent; // = PendingIntent.getActivity(this,0, intent,0);
		//NotificationCompat.Builder builder;				// Make global, call same builder for each notification
														// https://stackoverflow.com/questions/14885368/update-text-of-notification-not-entire-notification

		// If Android 8.0 or above, use notification channel (required)
		if (Build.VERSION.SDK_INT >= 26) {
			NotificationChannel notificationChannel = notificationManager.getNotificationChannel(CHANNEL_ID);

			if (notificationChannel == null) {
				notificationChannel = new NotificationChannel(CHANNEL_ID,
						CHANNEL_ID,
						NotificationManager.IMPORTANCE_HIGH);
				notificationChannel.enableVibration(false);
				//notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
				notificationManager.createNotificationChannel(notificationChannel);
			}
		}

		builder = new NotificationCompat.Builder(this, CHANNEL_ID);

		intent = new Intent(this, TrackSantaActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

		pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

		builder.setContentTitle(contentText)                            // required
				.setSmallIcon(android.R.drawable.ic_popup_reminder)   // required
				.setContentText(this.getString(R.string.app_name)) // required
				.setDefaults(Notification.DEFAULT_ALL)
				.setAutoCancel(true)
				.setContentIntent(pendingIntent)
				.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)	// view on lock screen
				.setTicker(contentText);

		notification = builder.build();

		Log.d(TAG, "startInForeground(): calling startForeground()");

		startService(intent);	// 11-24-19 - Docs direct to call this prior to startForeground - https://developer.android.com/reference/android/app/Service.html#startForeground(int,%20android.app.Notification)
		startForeground(NOTIFICATION_ID, notification);
		//Log.d(TAG, "Build Version Code: " + Integer.toString(Build.VERSION.SDK_INT));	// Returned 26

        Log.d(TAG, "startInForeground(): startForeground called");
    }

    /*
	 * Send start-tracking signal to OpalStack server.  Provides quick house-keeping to
	 * accommodate tracking for this particular engine.
	 * called by onStartCommand()
     */
    private void serverStartTracking() {

		JSONObject json = new JSONObject();
		try {
			json.put("engine", engineID);
		}
		catch(Exception e) {
		}

		JsonObjectRequest putRequest = new JsonObjectRequest(
				Request.Method.POST,
				Constants.getStartTrackingUrl(),
				json,
				null,
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
					}
				}
		);

		requestQueue.add(putRequest);
	}

	/**
	 * onStartCommand() requests location updates, and sends comamand to server to start tracking for this engine.
	 * @param intent
	 * @param flags
	 * @param startId
	 * @return
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

        engineID = intent.getStringExtra("EngineID");
        Log.d(TAG, "onStartCommand(): engineID: " + engineID);
        Log.d(TAG, "onStartCommand(): " + intent.getAction());

		if (intent.getAction() ==  ACTION_TRACK) {
		    Log.d(TAG, "onStartCommand(): ACTION_TRACK, testing ACCESS_FINE_LOCATION permission");

            if (checkSelfPermission(permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                // Subscribe to updates every 15 seconds, and after 45 meters (2 and 20 for testing)
                // was ... 15000, 45, this (15 seconds & 45 meters), now 5000, 45 (5 seconds, 45 meters)
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 45, this);    // Use GPS, check every 5 seconds, do not call unless moved 45 Meters
                Log.d(TAG, "onStartCommand(): location updates requested");

                serverStartTracking();
                Log.d(TAG, "Called Server (OpalStack) startTracking");

                trackingSanta = true;

                // Manually call onLocationChanged with a location object to force at least one server update.
                // Otherwise, if GPS does not change location when tracker started, will not update location to server.
                // Have to force the update to get it to upload the current locale to the server.
                // onLocationChanged(locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
                // locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);

                contentText = "Updating Engine Location";
            }
            else {
                Log.d(TAG, "onStartCommand(): ACCESS_FINE_LOCATION permission not granted");
            }
        }
        // ELSE - RESET ENGINE LOCATION TO STATION
        else if (intent.getAction() == ACTION_RESET) {
		    Log.d(TAG, "onStartCommand(): Stopping Service - resetting in onDestroy() method");

            //End Service
            this.stopSelf();
        }

		// This service must keep running
		return Service.START_STICKY;
	}


	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy() called");
		
		locationManager.removeUpdates(this);

		// Stop tracking, which also resets engine location on the server side
		// Must send a PUT request parameter, i.e.  {"engine": 1}

		JSONObject json = new JSONObject();
		try {
			json.put("engine", engineID);
		}
		catch(Exception e) {

		}

		JsonObjectRequest putRequest = new JsonObjectRequest(
				Request.Method.PUT,
				Constants.getStopTrackingUrl(),
				json,
				null,
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
					}
				}
        );

		requestQueue.add(putRequest);

		trackingSanta = false;

		stopForeground(true);

		wakeLock.release();
		
		super.onDestroy();
	}

	// LocationListener interface
	@Override
	public void onLocationChanged(Location location) {

		Log.d(TAG, "onLocationChanged(): called");

		// Want to keep three places after decimal
		double lat = location.getLatitude();
		double lon = location.getLongitude();
		long lTime = System.currentTimeMillis() / 1000;

		Log.d(TAG, "onLocationChanged: " + lat + ", " + lon);

		JSONObject json = new JSONObject();
		try {
			json.put("engine", engineID);
			json.put("lat", lat);
			json.put("lon", lon);
			json.put("time", lTime);
		}
		catch(Exception e) {

		}

		JsonObjectRequest putRequest = new JsonObjectRequest(
				Request.Method.POST,
				Constants.getUpdateLocationUrl(),
				json,
				null,
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError error) {
					}
				}
		);

		requestQueue.add(putRequest);

		// ------------------------------------------------------------------------------
		//String elapsedTime = timeSinceLastUpdate();
		// TODO: SEND THIS TO ACTIVITY

		// public String getTimeSinceLastUpdate() {	RETURNS TIME SINCE LAST UPDATE
		// public String getUpdates() {		RETURNS TOTAL UPDATES

		// UPDATE TIMES ON SINGLETON
		ActivityButtonServiceProxy.setLastUpdateTime();

		// Update notification w/ original notification and notification id
		// private Notification notification;
		// private static final int NOTIFICATION_ID = 4242;
		//NotificationManagerCompat.notify();

		// Total Updates: nnn
		builder.setContentText("Total Updates: " + ActivityButtonServiceProxy.getTotalUpdates());
		notificationManager.notify(NOTIFICATION_ID, builder.build());
	}

	// LocationListener interface
	public void onProviderDisabled(String provider) {
		Log.d(TAG, "Provider Disabled");
	}

	// LocationListener interface
	public void onProviderEnabled(String provider) {
		Log.d(TAG, "Provider Enabled");
	}

	// LocationListener interface
	public void onStatusChanged(String provider, int status, Bundle extras) {
		Log.d(TAG, "Provider Status Changed");
	}
	
}
