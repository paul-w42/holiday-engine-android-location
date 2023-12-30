package net.nwnetsolutions.holidayengine;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import net.nwnetsolutions.holidayengine.constants.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * This class loads the available engines, and presents a choice of engine to transmit location
 * for.  When an engine is selected and the appropriate button clicked, the user moves to the
 * TrackSantaActivity.
 *
 * Also on this display is a Quit button that both stops the service if running and quits the
 * app, and a TextView displaying the app version number.
 *
 * @author Paul Woods
 */
public class HolidayEngineUpdateActivity extends Activity implements OnClickListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = "...UpdateActivity";
    final private int REQUEST_CODE_ASK_FINE_ACCESS = 42;
    //final private int REQUEST_CODE_ASK_WAKELOCK = 43;
    RequestQueue requestQueue;
    String engineId;

    Button trackEngineButton;

    boolean engineSelected;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        engineSelected = false;     // default false value, no engine selected yet

        // LOAD INTENT TO REQUEST-IGNORE BATTERY OPTIMIZATION (CORRECT PLACE ??? )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = this.getPackageName();
            PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent();
                intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                intent.setData(Uri.parse("package:" + packageName));
                this.startActivity(intent);
            }
        }
        // END REQUEST-IGNORE

        setContentView(R.layout.main);
        
        // Setup click listeners for all buttons
        trackEngineButton = (Button)findViewById(R.id.track_engine_button);
        trackEngineButton.setOnClickListener(this);
        trackEngineButton.setEnabled(false);     // Disabled until we have selected an Engine to transmit

        View quitButton = findViewById(R.id.quit_button);
        quitButton.setOnClickListener(this);

        // Check to see if we have permission to use ACCESS_FINE_LOCATION
        // Required from Android 6.0+ (manifest permissions no longer applicable)
        int permissionCheck = this.checkSelfPermission(permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Requesting ACCESS_FINE_LOCATION permission");
            this.requestPermissions(new String[]{permission.ACCESS_FINE_LOCATION}, REQUEST_CODE_ASK_FINE_ACCESS);
        }

        // http://www.androiddeft.com/json-parsing-android-volley/#Making_JSONObject_Request
        requestQueue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, Constants.getEnginesUrl(), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Integer engines = response.getInt("number");
                            Log.d(TAG, "Engines from server: " + engines);

                            // @+id/engine_spinner
                            Spinner spinner = findViewById(R.id.engine_spinner);

                            // Set default display - i.e. "Select Engine to Transmit"
                            spinner.setOnItemSelectedListener(HolidayEngineUpdateActivity.this);

                            // Spinner Drop down elements
                            ArrayList<SpinnerSelectEngineEntry> engineChoices = new ArrayList<>();

                            for (int i = 0; i < engines + 1; i++) {
                                if (i == 0) {
                                    engineChoices.add(new SpinnerSelectEngineEntry(Integer.toString(i), "Engine to Transmit"));
                                }
                                else {
                                    engineChoices.add(new SpinnerSelectEngineEntry(Integer.toString(i), "Engine " + Integer.toString(i)));
                                }
                            }

                            Log.i(TAG, engineChoices.toString());

                            // Creating adapter for spinner
                            ArrayAdapter<SpinnerSelectEngineEntry> dataAdapter = new ArrayAdapter<SpinnerSelectEngineEntry>(HolidayEngineUpdateActivity.this, R.layout.spinner_item, engineChoices);

                            // attaching data adapter to spinner
                            spinner.setAdapter(dataAdapter);

                        }
                        catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                });
        //add request to queue
        requestQueue.add(jsonObjectRequest);

        // Display Software Version ...
        // @+id/versionText
        TextView versionText = findViewById(R.id.versionText);

        PackageInfo packageInfo;
        try {
            packageInfo = getApplicationContext()
                    .getPackageManager()
                    .getPackageInfo(
                            getApplicationContext().getPackageName(),
                            0
                    );
            Log.d(TAG, "Posting Version Name" + packageInfo.versionName);
            versionText.setText("Version " + packageInfo.versionName);

        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Version name not found");
            versionText.setText("[version name not found]");
        }

        checkAndSetButtonStatus();
    }

    /*
     * Check status of TrackSantaService and set button status accordingly
     */
    private void checkAndSetButtonStatus() {
        // if we are transmitting a location already, disable button for alternate engines
        if (TrackSantaService.trackingSanta) {
            trackEngineButton.setEnabled(false);
        }
        // we are not transmitting, has an engine been selected yet?
        else if (engineSelected) {
            trackEngineButton.setEnabled(true);
            //trackEngineButton.setClickable(true);
        }
        else {
            trackEngineButton.setEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume() called");
        checkAndSetButtonStatus();
        super.onResume();
    }

    /*
     * Receives our callback on requesting fine-access-location permission.  On permission-denied, displays a toast.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {

            case REQUEST_CODE_ASK_FINE_ACCESS:
                if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(HolidayEngineUpdateActivity.this
                    , "access fine location denied"
                    , Toast.LENGTH_SHORT).show();
                }

                break;
            /*
            case REQUEST_CODE_ASK_WAKELOCK:
                if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Toast.makeText(HolidayEngineUpdateActivity.this
                            , "access wake-lock denied"
                            , Toast.LENGTH_SHORT).show();
                }

                break;
            */
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /**
     * onClick handler for Quit button & Track Engine button.
     * @param v
     */
    public void onClick(View v) {

        Intent i;
        int id = v.getId();

        if (id == R.id.quit_button) {
            // Make sure not tracking any longer
            this.stopService(new Intent(this, TrackSantaService.class));

            LoginActivity.quit = true;
            ActivityButtonServiceProxy.setRecording(false);

            this.finish();
        }
        else if (id == R.id.track_engine_button) {
            i = new Intent(this, TrackSantaActivity.class);
            i.setAction(TrackSantaService.ACTION_TRACK);
            i.putExtra("EngineID", engineId);
            startActivity(i);
        }
		
	}


    /**
     * Called when the spinner indicating which engine to transmit has been selected. Also manages
     * some state re enabling/disabling buttons depending on selection.
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        //String item = parent.getItemAtPosition(position).toString();
        SpinnerSelectEngineEntry item = (SpinnerSelectEngineEntry) parent.getItemAtPosition(position);

        trackEngineButton = (Button)findViewById(R.id.track_engine_button);

        engineId = item.getId();

        Log.d(TAG, "onItemSelected(): " + item);

        if (item.getId().equalsIgnoreCase("0")) {
            trackEngineButton.setText("Select " + item.getName());
            trackEngineButton.setEnabled(false);
            engineSelected = false;
        }
        else {
            trackEngineButton.setText("Confirm Transmit " + item.getName());
            trackEngineButton.setEnabled(true);
            engineSelected = true;
        }

        checkAndSetButtonStatus();
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO populate onNothingSelected method
        Log.d(TAG, "onNothingSelected()");
    }

    
}