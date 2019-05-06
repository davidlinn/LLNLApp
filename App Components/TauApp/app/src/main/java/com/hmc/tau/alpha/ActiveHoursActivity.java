package com.hmc.tau.alpha;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.os.Bundle;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;

/**
 * Active Hours Activity:
 *     Continuously determine whether user is earning active detector minutes
 *     Keep track of points
 *     Pushes points to UserDatabase Google Sheet each time activity is opened
 *     Details of active hours algorithm:
 *         Uses SensorManager class to listen to updates from a sensor (calls onSensorChanged() at each
 *             update)
 *         In onSensorChanged(): Accumulates difference in magnitude of accelerometer readings until
 *             we've gathered SAMPLE_SIZE readings, at which point we find the average difference in
 *             accelerometer magnitude over the sampling period and determine if greater than threshold.
 *             Implementation based off [link]
 *         Checks whether a sensor is currently connected to the phone with BTReciever class (check is
 *             currently disabled by setting outputs of all sensor checks to true)
 *     2nd semester to-do:
 *         Fine-tune active hours algorithm, potentially employ machine learning methods discussed in
 *             ___ article at [link]
 *         Secure score- ensure user is unable to edit locally or find a way to intercept push to Google
 *             Sheets
 *     Josh Morgan, David Linn - jmorgan@hmc.edu, dlinn@hmc.edu - 12/7/18
 */
public class ActiveHoursActivity extends AppCompatActivity implements SensorEventListener {

    private static boolean sensorRegistered;
    private static int newSteps = 0;  //steps since last point update
    private static boolean active = false;

    /**
     * Called when ActiveHoursActivity is created.
     * Sets the XML file, registers the step counter sensor, and verifies that the sensor has been
     * paired before updating the UI.
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set the XML file for this Activity
        setContentView(R.layout.activity_scoring);

        // Register the step counter sensor
        final SensorManager sensorMan = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor mStepCounter;

        if (sensorMan.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
            // Success! There's a step counter
            mStepCounter = sensorMan.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
            sensorMan.registerListener(this, mStepCounter, SensorManager.SENSOR_DELAY_NORMAL);

        } else {
            // Failure! No step counter
            Log.d("Step_counter", "No Step Counter available.");

        }

        // Verify that a bluetooth device has been paired
        checkAttached();

    }

    /**
     * Called when the Activity is re-opened. Assumes user is inactive (red display) and updates
     * display.
     */
    @Override
    public void onResume() {
        super.onResume();

        pushPointsToServer();

        active = false;
        setDisplay();


    }

    /**
     * Required method of SensorEventListener. Does nothing.
     * @param sensor
     * @param num
     */
    public void onAccuracyChanged(Sensor sensor, int num) {
        Log.d("accelerometer ", "accuracy changed");
    }


    /**
     * onSensorChanged responds to the broadcasted SensorEvent by increasing the user's Active
     * Hours Points by the amount defined in GlobalParams.STEPS_PER_POINT. Here, we assert that
     * there is a linear correspondence between steps taken and time spent active.
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {

            int counterSteps = UserDataUtils.getCounterSteps();

            //grab the latest step count from the sensor
            int newCounterSteps = (int) event.values[0];

            newSteps += newCounterSteps - counterSteps;

            //if the counterSteps field is zero, it should be initialized to this
            //sensor reading.
            if(counterSteps == 0 || newSteps > 3000){
                counterSteps = newCounterSteps;
                newSteps = 0;
                Log.d("ActiveHours", "counterSteps reset");
            }

            UserDataUtils.setCounterSteps(newCounterSteps);

            // if a minimum number of new steps has been reached, assign a new point
            // and reset the number of new steps.
            int spp = GlobalParams.STEPS_PER_POINT;
            if (newSteps > spp) {

                int pointsToAssign = newSteps / spp;
                UserDataUtils.incrementUserActiveHoursScore(pointsToAssign);
                newSteps %= spp;

                Log.d("ActiveHours", pointsToAssign + " active hours points assigned");

            }

            Log.d("Steps", "detected: " + newSteps);
            active = true;
            setDisplay();
        }
    }


    /**
     * Sets sensorRegistered.
     * Specifically, this method queries Android's bluetooth service to find what bluetooth devices have been previously
     * bonded with the phone. If that list includes a Kromek sensor (containing "SGM"), then
     * sensorRegistered is set to true.
     */
    public void checkAttached() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        for (BluetoothDevice bt : pairedDevices) {
            String bluetoothDevice = bt.getName();
            int startingIndex = bluetoothDevice.indexOf("SGM");
            if (startingIndex != -1) {
                Log.d("Sensor", "Sensor paired");

                //eg. blueToothDevice = "D3S SGM104787"
                //idNum = 4787
                String idNum = bluetoothDevice.substring(startingIndex + 5, startingIndex + 9);
                UserDataUtils.setSensorID(idNum);
                Log.d("SensorID: ", UserDataUtils.getSensorID());

                sensorRegistered = true;
            } else {
                sensorRegistered = false;
            }
        }

    }

    /**
     * Attempts to push current userActiveHoursScore and userQRCodeScore to UserDatabase Google Sheet
     * Displays response
     */
    public void pushPointsToServer() {
        //Populate the server info view depending on request result
        final TextView InfoDisplay = (TextView) findViewById(R.id.score_serverInfo);
        //Create queue that accepts requests
        RequestQueue queue = Volley.newRequestQueue(this);
        //Build URL and query string from JSON object
        String url = getApplicationContext().getString(R.string.user_database_url);
        url += '?';
        url += "Sheet=" + "MSTR" + '&'; //  note: may need to change each deployment
        url += "RequestType=DataPush&";
        url += "SensorID=" + UserDataUtils.getSensorID() + '&';
        url += "ActiveMinPoints=" + UserDataUtils.getUserActiveHoursScore() + '&';
        url += "QRCodePoints=" + UserDataUtils.getUserQRCodeScore();
        url = ensureValidURL(url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String str = response.toString();
                        Log.d("QR JSON response", str);
                        boolean result = false;
                        try {
                            result = response.getString("result").equals("success");
                        } catch (JSONException exception) {
                            InfoDisplay.setText("JSON String returned by server has no field 'result'.");
                        }
                        if (result) {
                            try {
                                InfoDisplay.setText("Successfully updated server, with our manual offset you have "
                                        + response.getInt("remotePoints") + " points on our servers.");
                            } catch (JSONException e) {
                                InfoDisplay.setText("This should never appear: Email us if you see this USERDATABASEERROR");
                            }
                        } else {
                            try {
                                InfoDisplay.setText("Connected to server but failed to push local score: " +
                                        response.getString("error"));
                            } catch (JSONException e) {
                                InfoDisplay.setText("This should never appear: Email us if you see this USERDATABASEERROR");
                            }
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        InfoDisplay.setText("Error in HTTP Request");
                    }
                });

        queue.add(jsonObjectRequest);
    }

    /**
     * Updates each of the four Textviews: ActiveHours, QR, SensorReg, and TotalScore
     */
    public void setDisplay() {
        updateActiveHoursDisplay();
        updateQRDisplay();
        updateRegisteredDisplay();
        updateTotalScoreDisplay();
    }

    /**
     * Updates ActiveHours Textview
     */
    public void updateActiveHoursDisplay() {
        //set activeHoursDisplay to display the Active Hours score
        String activeHoursScoreString = Integer.toString(UserDataUtils.getUserActiveHoursScore());
        TextView activeHoursDisplay = (TextView) findViewById(R.id.score_activeHoursDisplay);
        activeHoursDisplay.setBackgroundColor(Color.LTGRAY);
        activeHoursDisplay.setText(activeHoursScoreString);
    }

    /**
     * Updates Total Score Textview
     */
    public void updateTotalScoreDisplay() {
        //set totalScoreDisplay to display total score
        String totalScoreString = Integer.toString(UserDataUtils.getUserTotalScore());
        TextView totalScoreDisplay = (TextView) findViewById(R.id.score_totalScoreDisplay);

        int c = active ? Color.GREEN : Color.RED;
        totalScoreDisplay.setBackgroundColor(c);
        totalScoreDisplay.setText(totalScoreString);
    }

    /**
     * Updates QR Score Textview
     */
    public void updateQRDisplay() {
        //set activeDisplay to display whether the user is active
        String QRScoreString = Integer.toString(UserDataUtils.getUserQRCodeScore());
        TextView QRDisplay = (TextView) findViewById(R.id.score_QRPointsDisplay);
        QRDisplay.setBackgroundColor(Color.LTGRAY);
        QRDisplay.setText(QRScoreString);
    }

    /**
     * Updates SensorReg Textview
     */
    public void updateRegisteredDisplay() {
        //set sensorReg TextView according to sensorRegistered
        TextView sensorReg = findViewById(R.id.score_sensorRegistered);
        Log.d("Sensor_registered", Boolean.toString(sensorRegistered));
        if (sensorRegistered) {
            sensorReg.setText("Sensor Registered: true");
            sensorReg.setBackgroundColor(Color.LTGRAY);
        } else {
            sensorReg.setText("Sensor Registered: false");
            sensorReg.setBackgroundColor(Color.RED);
        }
    }

    /**
     * Turn all spaces in String url into '+' characters
     * @param url String to replace whitespaces of
     * @return String with whitespaces replaced with '+'
     */
    public static String ensureValidURL(String url) {
        String s = "";
        for (char c : url.toCharArray()) {
            if (c == ' ')
                s = s + '+';
            else
                s = s + c;
        }
        return s;
    }
}