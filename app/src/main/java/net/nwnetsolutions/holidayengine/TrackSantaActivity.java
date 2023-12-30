package net.nwnetsolutions.holidayengine;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * This Activity presents a toggle button that allows the user to start transmitting the selected
 * engines location.  When the button is de-selected, the service is stopped, and the engines
 * location is reset to its default starting point.
 *
 * The Reset button is present in case the app has quit or an engine was not automatically reset
 * for some reason.  This also allows for easy engine location resets from a different app
 * if needed without first transmitting a new location.
 *
 * @author Paul Woods
 */
public class TrackSantaActivity extends Activity implements OnClickListener {

	private static final String TAG = "TrackSantaActivity";
	private static String trackingAction = "";
	private ToggleButton toggleButton;

	private Button resetButton;
	public static String engineID = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.santa);

		Intent i = this.getIntent();
		if (i.getAction() == TrackSantaService.ACTION_TRACK) {
			Log.d(TAG, "onCreate: Track Engine");
		} else if (i.getAction() == TrackSantaService.ACTION_RESET) {
			Log.d(TAG, "onCreate: Reset Engine");
		}

		// Set Action for subsequent (possible) call to TrackSantaService, note which engine to track
		trackingAction = i.getAction();
		engineID = i.getStringExtra("EngineID");

		Log.d(TAG, "onCreate: EngineID received " + engineID);


		toggleButton = (ToggleButton) findViewById(R.id.track_santa_toggle);
		toggleButton.setOnClickListener(this);

		resetButton = (Button)findViewById(R.id.button_reset);
		resetButton.setOnClickListener(this);

		Log.d(TAG, "Setting button text: START-ENGINE-" + engineID);
		//toggleButton.setText();
		toggleButton.setTextOff("START TRANSMITTING ENGINE " + engineID);
		toggleButton.setTextOn("STOP TRANSMITTING ENGINE " + engineID);

		if (TrackSantaService.trackingSanta) {
			toggleButton.setChecked(true);
		} else {
			toggleButton.setChecked(false);
		}
	}


	@Override
	protected void onResume() {
		Log.d(TAG, "onResume() called.  Setting toggleButton");
		super.onResume();

		if (TrackSantaService.trackingSanta) {
			toggleButton.setChecked(true);
			Log.d(TAG, "Set button to ON");
		} else {
			toggleButton.setChecked(false);
			Log.d(TAG, "Set button to OFF");
		}
	}


	/**
	 * Click handler for both Track Santa toggle button and the Reset Location button.  Re the
	 * toggle button, toggling this button will start the TrackSantaService if it is not already
	 * running, otherwise it will stop the service and reset Santas start location.
	 *
	 * The Reset button is for cases where the service has been stopped or has failed in some manner
	 * that the engine location was not reset.  In this case, selecting the Reset button will reset
	 * the engine location.
	 * @param v
	 */
	public void onClick(View v) {
		ToggleButton trackSantaToggle = (ToggleButton) findViewById(R.id.track_santa_toggle);

		int id = v.getId();

		if (id == R.id.track_santa_toggle) {

			if (trackSantaToggle.isChecked()) {

				Log.d(TAG, "TRACK SANTA BUTTON CLICKED - STARTING SERVICE");
				Log.d(TAG, "EngineID to Transmit - " + engineID);

				// disable reset button while service running
				resetButton.setEnabled(false);

				// NOTE: Service automatically calls stop-tracking when it is killed.

				// Start santa service
				Intent intent = new Intent(TrackSantaActivity.this, TrackSantaService.class);

				// Set specific action, i.e. a data value/type to be passed to service, passed in from HolidayEngineUpdateActivity (Engine 1 vs Engine 2)
				intent.setAction(trackingAction);
				intent.putExtra("EngineID", engineID);

				startService(intent);

				// Initialize ActivityButtonServiceProxy
				ActivityButtonServiceProxy.setStartTime();

				final TextView tvLastUpdate = (TextView) findViewById(R.id.textView_last_update);
				final TextView tvTotalUpdates = (TextView) findViewById(R.id.textView_total_updates);
				final TextView tvLongestUpdate = (TextView) findViewById(R.id.longest_update);
				final TextView tvTotalRunningTime = (TextView) findViewById(R.id.total_running_time);


				// Separate Thread for UI/Service updates & interaction
				Thread thread = new Thread() {

					@Override
					public void run() {
						try {
							while (!isInterrupted()) {
								Thread.sleep(1000);
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										// update TextView here!
										tvLastUpdate.setText(ActivityButtonServiceProxy.getTimeSinceLastUpdate());
										tvTotalUpdates.setText(ActivityButtonServiceProxy.getTotalUpdates());
										tvLongestUpdate.setText(ActivityButtonServiceProxy.getLongestUpdate());
										tvTotalRunningTime.setText(ActivityButtonServiceProxy.getTotalTime());
									}
								});
							}
						} catch (InterruptedException e) {
						}
					}
				};

				thread.start();
			} else {
				Log.d(TAG, "TRACK SANTA BUTTON CLICKED - STOPPING SERVICE");

				stopService(new Intent(this, TrackSantaService.class));

				// enable reset button again, service is stopped
				resetButton.setEnabled(true);
			}
		}
		else if (id == R.id.button_reset) {
			Log.d(TAG, "RESET Button clicked");
			Intent i = new Intent(TrackSantaActivity.this, TrackSantaService.class);
			i.setAction(TrackSantaService.ACTION_RESET);	// reset location, end
			i.putExtra("EngineID", engineID);
			startService(i);
			stopService(i);
		}

	}

	// Example in "Android Programming: .... ", intent returned to call from SantaService, used to pull activity from notification
	public static Intent newIntent(Context context) {
		return new Intent(context, TrackSantaActivity.class);
	}

}
