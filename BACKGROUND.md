# Android Holiday Engine Location Updates

**Note:** While primarily successful, especially in the last few years, a common issue across differing phones used to transmit the Holiday Engines location has been keeping the service running in the background without being put to sleep or killed by the scheduler. Quite a bit of this is working through permission issues on specific phones, and some of it might be phone (or android build) specific as well.  So far, we have had pretty reliable success with Pixels, and at least one Samsung Galaxy.

Below are some notes on this topic, as well as additional areas or strategies we may look into as time allows.  **Advice, pointers, and suggestions appreciated!**

## Notes & Strategies

* partial wake-lock (no screen, cpu kept alive)
  * https://developer.android.com/training/scheduling/wakelock
  * "If your app relies on background services, consider using JobScheduler"
  * Old, but is what I am thinking. Aquire the wakelock inside the onStart() method, and release in the destroy() method. https://stackoverflow.com/questions/32085565/android-background-service-and-wake-lock
  * https://medium.com/googleplaydev/https-medium-com-googleplaydev-how-to-fix-app-quality-issues-with-android-vitals-part-2-7ba6ebe646ab Takes of services using wakelocks, and lists some alternatives.
  * "Use JobScheduler ... API when a job needs to run in the background. Using these features will hold a wake lock for the job while it is running."
  * "Schedule an AlarmManager broadcast when you want to run a background task at a fixed time or regular intervals. AlarmManager holds a wake lock for as long as your BroadcastReceiver.onReceive() method is running."
  * https://stackoverflow.com/questions/52425222/foreground-service-getting-killed-from-oreo  "Try call startForeground(id, notification) in onCreate() method before you acquire wake lock. And if you start service with alarm broadcast, acquire wakelock with timeout (some seconds) in BroadcastReceiver's onReceive method, to give service time to start, and then start your service"
  * ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
  * Job Scheduler - "...the preferred way of performing background work..."
  * Double Check service - notification channel importance set, persistent notification priority set.
  * GeoFencing - limited to 100 per phone, but can we create, use, remove, create .... scenario of notifying when we leave a geofenced area, not when we arrive
  * Others having same issue, working through, etc. - https://github.com/google/ExoPlayer/issues/5117
    * NOTE - Example of notification construction, double check against this.  Uses         notificationManagerCompat.notify(1,notification) after startForground()
  * https://developer.android.com/reference/android/R.attr.html#foregroundServiceType
    * Specify service type starting in Android Q
  * DONE - <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
  * DONE - ... intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
  * DONE - <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
        relevant for Android Q / 10 only


## General Notes on Background Services

* Android 6/API-23 - introduced doze mode
* 7/API-24 - doze-on-the-go
* 8/API-26 - additional limitations, esp w/ location
* 9/API-28 - app standby buckets, resouces prioritized based on usage patterns
* "Since Android 5.0, JobScheduler has been the preferred way of performing background work in a way that's good for users. Apps can schedule jobs while letting the system optimize based on memory, power, and connectivity conditions. JobScheduler offers control and simplicity, and we want all apps to use it."
* Re data-saver - "Users can whitelist specific apps to allow background metered data usage even when Data Saver is turned on."
* Lists foreground services as one item to use (already use)
* Alarm Manager to run a job at a specific time
* Work manager if the job does not need to run at a specific time (not appropriate for us)


## Notes on Android 9 & later

* Background Location Limits:
  * https://developer.android.com/about/versions/oreo/background-location-limits.html
  * An app with a foreground service is not considered to be in the background.

* Location Strategies
  * https://developer.android.com/guide/topics/location/strategies

* Android 9 Power Management ( & standby buckets)
  * https://developer.android.com/about/versions/pie/power
  * Best Practices to promote bucket level - have a launcher activity, make notifications actionable (user can touch/click)
  * Additional restrictions in 9
    * puts apps in app standby mode more aggressively
    * Location services may be disabled when the screen is off.
    * Background apps do not have network access
* Doze-Standby
  * https://developer.android.com/training/monitoring-device-state/doze-standby#support_for_other_use_cases
  * YES!!! - An app can check whether it is currently on the exemption whitelist by calling isIgnoringBatteryOptimizations().  Call, put results in a toast ... 
  * https://developer.android.com/reference/android/provider/Settings.html#ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
  * Also provides notes on testing and forcing system into doze mode and app standby.
* Permissions
  * https://developer.android.com/reference/android/Manifest.permission.html#ACCESS_BACKGROUND_LOCATION
  * ACCESS_BACKGROUND_LOCATION - new in API 29, permission level dangerous
  * https://stackoverflow.com/questions/55705238/how-does-access-background-location-introduced-in-android-q-affect-geofence-apis
  * https://medium.com/google-developer-experts/exploring-android-q-location-permissions-64d312b0e2e1
* Save Android Service from Doze Mode
  * https://hashedin.com/blog/save-your-android-service-from-doze-mode/
  * Create foreground service with Different Process Name
  * Create an alarm which runs every 30 seconds, by checking android version
  * Create broadcast receiver for catching the alarm, should be in the same process where the foreground service is running.
  * ARTICLE NOTES that device behavior while plugged in (portable battery charger?) is different.
* Android 9 Changes
  * https://developer.android.com/about/versions/pie/android-9.0-changes-all
  * For our purposes, essentially points to https://developer.android.com/about/versions/pie/power (listed above)
  
## Also Found - Relevant?

https://dontkillmyapp.com/






















