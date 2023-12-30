package net.nwnetsolutions.holidayengine;

/*
 * 1.4667 * speed (mph) * time (seconds) = feet traveled
 * 15 seconds @ 60mph = 1/4 mile	- 400M
 * 30 seconds @ 60mph = 1/2 mile	- 800M
 * 15 seconds @ 40mph = 880ft		- 293M
 * 30 seconds @ 40mph = 1760ft (1/3 mile) 	- 586M
 * 15 seconds @ 25mph = 550ft (1/10 mile) 	- 170M
 * 30 seconds @ 25mph = 1100ft (2/10 mile) 	- 366M
 * 15 seconds @ 4mph  = 88 ft		- 30M
 * 30 seconds @ 4mph  = 176 ft		- 59M
 * 45 seconds @ 4mph  = 264 ft		- 88M
 * 45 seconds @ 3mph  = 198 ft		- 66M
 * 
 * To travel 100' at 2mph, takes 34 seconds
 * 
 * If we are travelling down the road, 30 second intervals are fine ... we are not going to be where people can get to us.
 * If we are at a stop light, it is too much.
 * If we are walking, 30 seconds would work as well.  
 * If we are standing, it's too much.
 * 
 * 
 * Start location updating service
 * 		- check location
 * 			IF
 * 				> than 100' delta from prior update
 * 				AND 
 * 				> 15 seconds since last update
 * 			THEN
 * 				UPDATE location
 * 				Save 'lastLocation' for future distance checks
 */


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

import net.nwnetsolutions.holidayengine.constants.Constants;
import net.nwnetsolutions.holidayengine.util.Security;

/**
 * This Activity class allows the user to log into the app.  Currently this contains only the most
 * basic of validation, using a hard-coded password.  This has to be improved.
 *
 * @author Paul Woods
 */
public class LoginActivity extends Activity implements OnClickListener {

	private EditText password;
	private static Boolean loggedIn;
	public static Boolean quit;
	
	private static final String TAG = "LoginActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG,"onCreate()");
		
		if (quit != null && quit == true) {
			quit = false;
			Log.d(TAG,"onCreate() - quiting");
			finish();
		}
		else {
			//super.onCreate(savedInstanceState);
	        setContentView(R.layout.login);
	        
	        Log.d(TAG,"onCreate() - presenting login page");
	        
	        loggedIn = false;
	        
	        password = (EditText)findViewById(R.id.login_password);
	        
	        // Setup click listeners for all buttons
	        View loginButton = findViewById(R.id.login_button);
	        loginButton.setOnClickListener(this);
		}
	}
	
	
	/*
	 * Returning to main screen.  In case a service is still running, sharing our location, kill now.
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		Log.d(TAG, "onResume() called, killing outstanding services");
		super.onResume();
		
		// Log.d(TAG,"onResume()");
		
		if (quit != null && quit == true) {
			quit = false;
			// Log.d(TAG,"onResume() - quit true");
			finish();
		}
		else if (loggedIn) {
			// Log.d(TAG,"onResume() - loading HolidayEngineUpdateActivity");
			Intent i = new Intent(this, HolidayEngineUpdateActivity.class);
			startActivity(i);
		}
	}

	public void onClick(View v) {

		int id = v.getId();
		if (id == R.id.login_button) {
			String pwd = password.getText().toString();
			Security security = new Security();

			// TODO - Use a different Password validator - i.e. retrieve off network vs constants

			if (security.stringMatchesHash(pwd, Constants.getPassword())) {
				
				loggedIn = true;
				
				Intent i = new Intent(this, HolidayEngineUpdateActivity.class);
				startActivity(i);
			}
			else {
				// Incorrect Password - exit
				finish();
			}
		}
	}

}
