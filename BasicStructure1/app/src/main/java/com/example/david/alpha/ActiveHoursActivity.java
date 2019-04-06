package com.example.david.alpha;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
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

/*
    Active Hours Activity:
    Continuously determine whether user is earning active detector minutes
    Keep track of points
    Pushes points to UserDatabase Google Sheet each time activity is opened
    Details of active hours algorithm:
        Uses SensorManager class to listen to updates from a sensor (calls onSensorChanged() at each
            update)
        In onSensorChanged(): Accumulates difference in magnitude of accelerometer readings until
            we've gathered SAMPLE_SIZE readings, at which point we find the average difference in
            accelerometer magnitude over the sampling period and determine if greater than threshold.
            Implementation based off [link]
        Checks whether a sensor is currently connected to the phone with BTReciever class (check is
            currently disabled by setting outputs of all sensor checks to true)
    2nd semester to-do:
        Fine-tune active hours algorithm, potentially employ machine learning methods discussed in
            ___ article at [link]
        Secure score- ensure user is unable to edit locally or find a way to intercept push to Google
            Sheets
    Josh Morgan, David Linn - jmorgan@hmc.edu, dlinn@hmc.edu - 12/7/18
 */
public class ActiveHoursActivity extends AppCompatActivity implements SensorEventListener {

    private static boolean sensorRegistered;
    private static String sensorID;
    private static int counterSteps; //latest value of the cumulative step sensor
    private static int newSteps = 0;  //steps since last point update
    private static boolean active = false;

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

        // Update all UI elements
        setDisplay();
    }

    @Override
    public void onResume() {
        super.onResume();

        active = false;
        setDisplay();

        pushPointsToServer();
    }

    public void onAccuracyChanged(Sensor sensor, int num) {
        Log.d("accelerometer ", "accuracy changed");
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {

            //Determine number of new steps
            //the step counter gives the total number of steps
            int newCounterSteps = (int) event.values[0];
            newSteps += newCounterSteps - counterSteps;
            counterSteps = newCounterSteps;

            // if a minimum number of new steps has been reached, assign a new point
            // and reset the number of new steps.
            int stp = GlobalParams.STEPS_PER_POINT;
            if (newSteps > stp) {

                int pointsToAssign = newSteps / stp;
                UserDataUtils.incrementUserActiveHoursScore(pointsToAssign);
                newSteps %= stp;

                Log.d("ActiveHoursPoints", pointsToAssign + " active hours points assigned");

            }

            Log.d("Steps", "detected: " + newSteps);
            active = true;
            setDisplay();
        }
    }


    public void checkAttached() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        for (BluetoothDevice bt : pairedDevices) {
            String bluetoothDevice = bt.getName();
            int startingIndex = bluetoothDevice.indexOf("SGM");
            if (startingIndex != -1) {
                Log.d("Sensor", "Sensor paired");
                BluetoothSocket tmp;

                sensorID = bluetoothDevice;

                if (!sensorID.equals(UserDataUtils.getSensorID())) {
                    UserDataUtils.setSensorID(sensorID);
                }

                sensorRegistered = true;
            } else {
                sensorRegistered = false;
            }
        }

    }

    //Attempts to push current userActiveHoursScore and userQRCodeScore to UserDatabase Google Sheet
    //Displays response
    public void pushPointsToServer() {
        //Populate the server info view depending on request result
        final TextView InfoDisplay = (TextView) findViewById(R.id.score_serverInfo);
        //Create queue that accepts requests
        RequestQueue queue = Volley.newRequestQueue(this);
        //Build URL and query string from JSON object
        String url = getApplicationContext().getString(R.string.user_database_url);
        url += '?';
        url += "Sheet=" + "Event2" + '&';
        url += "RequestType=DataPush&";
        url += "SensorID=" + UserDataUtils.getSensorID().substring(5, 9) + '&';
        url += "ActiveMinPoints=" + UserDataUtils.getUserActiveHoursScore() + '&';
        url += "QRCodePoints=" + UserDataUtils.getUserQRCodeScore();
        url = ensureValidURL(url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String str = response.toString();
                        Log.e("QR JSON response", str);
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

    public void setDisplay() {
        updateActiveHoursDisplay();
        updateQRDisplay();
        updateRegisteredDisplay();
        updateTotalScoreDisplay();
    }

    public void updateActiveHoursDisplay() {
        //set QRDisplay to display the Active Hours score
        String activeHoursScoreString = Integer.toString(UserDataUtils.getUserActiveHoursScore());
        TextView activeDisplay = (TextView) findViewById(R.id.score_activeHoursDisplay);
        activeDisplay.setBackgroundColor(Color.LTGRAY);
        activeDisplay.setText(activeHoursScoreString);
    }

    public void updateTotalScoreDisplay() {
        //set totalScoreDisplay to display total score
        String totalScoreString = Integer.toString(UserDataUtils.getUserTotalScore());
        TextView totalScoreDisplay = (TextView) findViewById(R.id.score_totalScoreDisplay);

        int c = active ? Color.GREEN : Color.RED;
        totalScoreDisplay.setBackgroundColor(c);
        totalScoreDisplay.setText(totalScoreString);
    }

    public void updateQRDisplay() {
        //set activeDisplay to display whether the user is active
        String QRScoreString = Integer.toString(UserDataUtils.getUserQRCodeScore());
        TextView QRDisplay = (TextView) findViewById(R.id.score_QRPointsDisplay);
        QRDisplay.setBackgroundColor(Color.LTGRAY);
        QRDisplay.setText(QRScoreString);
    }

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

    public static String ensureValidURL(String url) {
        //Turn all spaces in String into '+' characters
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