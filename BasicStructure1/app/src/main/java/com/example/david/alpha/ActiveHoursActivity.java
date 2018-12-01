package com.example.david.alpha;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.text.DateFormat;
import java.util.Date;
import java.util.Set;

public class ActiveHoursActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorMan;
    private Sensor accelerometer;

    private float[] mGravity;
    private double mAccel;
    private double mAccelCurrent;
    private double mAccelLast;

    private static LatLng myPos;
    private static boolean walking;
    private static boolean sensorRegistered;
    private static boolean atSpeed;
    private static String sensorID;
    private static int userActiveHoursScore;
    public static int userQRCodeScore;
    public static int userTotalScore;
    public static boolean active = false;

    private static long startRestTime;
    private static long elapsedRestTime;
    private static long startActiveTime;
    private static long elapsedActiveTime;

    public static SharedPreferences userData;
    public String sharedPrefFile = "com.example.david.alpha";

    public void onAccuracyChanged(Sensor sensor, int num) {
        Log.d("accelerometer ", "accuracy changed");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scoring);

        userData = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);

        userTotalScore = userData.getInt(GlobalParams.TOTAL_SCORE_KEY,userTotalScore); //TODO: FIGURE OUT DEF VALUE
        userActiveHoursScore = userData.getInt(GlobalParams.ACTIVEHOURS_SCORE_KEY, userActiveHoursScore);
        userQRCodeScore = userData.getInt(GlobalParams.QRCODE_SCORE_KEY,userActiveHoursScore);

        sensorMan = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        IntentFilter disconnectFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        IntentFilter connectFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        this.registerReceiver(BTReceiver,disconnectFilter);
        this.registerReceiver(BTReceiver,connectFilter);

        sensorMan.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        checkAttached();

        String totalScoreString = Integer.toString(userTotalScore);
        TextView totalScoreDisplay = (TextView) findViewById(R.id.score_totalScoreDisplay);
        totalScoreDisplay.setBackgroundColor(Color.RED);
        totalScoreDisplay.setText(totalScoreString);

        String QRScoreString = Integer.toString(userQRCodeScore);
        TextView activeDisplay = (TextView) findViewById(R.id.score_QRPointsDisplay);
        activeDisplay.setBackgroundColor(Color.LTGRAY);
        activeDisplay.setText(QRScoreString);

        String activeHoursScoreString = Integer.toString(userActiveHoursScore);
        TextView QRDisplay = (TextView) findViewById(R.id.score_activeHoursDisplay);
        QRDisplay.setBackgroundColor(Color.LTGRAY);
        QRDisplay.setText(activeHoursScoreString);
    }

    @Override
    public void onResume() {
        super.onResume();
        TextView activeDisplay = (TextView) findViewById(R.id.score_QRPointsDisplay);
        String QRScoreString = Integer.toString(userQRCodeScore);
        Log.d("QRCodeScore",QRScoreString);
        activeDisplay.setText(QRScoreString);
        pushPointsToServer();
    }

    private int hitCount = 0;
    private double hitSum = 0;
    private double hitResult = 0;

    private int SAMPLE_SIZE = GlobalParams.ACC_SAMPLE_SIZE;
    private double THRESHOLD = GlobalParams.ACC_THRESHOLD;


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values.clone();
            double x = mGravity[0];
            double y = mGravity[1];
            double z = mGravity[2];

            //Log.d("acceleration", "x = " + Double.toString(x) + ", " + "y = " + Double.toString(y) + ", " + "z = " + Double.toString(x) + ", ");
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt(x * x + y * y + z * z);
            double delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;

            if (hitCount <= SAMPLE_SIZE) {
                hitCount++;
                hitSum += Math.abs(mAccel);
            } else {
                myPos = MapsActivity.myPos;
                hitResult = hitSum / SAMPLE_SIZE;

                Log.d("Sensor", String.valueOf(hitResult));
                if (hitResult > THRESHOLD) {
                    initiateActivity();
                    walking = true;
                    Log.d("Accelerometer: ", "Walking");
                } else {
                    initiateRest();
                    walking = false;
                    Log.d("Accelerometer: ", "Not Walking");
                }

                //Log.d("UserScore", Integer.toString(userTotalScore));
                hitCount = 0;
                hitSum = 0;
                hitResult = 0;
            }
        }
    }

    //David Edit 11/21: Simplified logic and ensured sensors that don't start with
    //  but contain "SGM" are included
    public void checkAttached() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        for (BluetoothDevice bt : pairedDevices) {
            String bluetoothDevice = bt.getName();
            int startingIndex = bluetoothDevice.indexOf("SGM");
            if (startingIndex != -1) {
                Log.d("Sensor","Sensor paired");
                BluetoothSocket tmp;

                sensorID = bluetoothDevice;
                SharedPreferences.Editor prefEditor = userData.edit();
                prefEditor.putString(GlobalParams.SENSOR_KEY, sensorID);
                prefEditor.apply();

                try {
                    tmp = bt.createRfcommSocketToServiceRecord(bt.getUuids()[0].getUuid());
                    tmp.connect();
                    Log.d("BluetoothSocket","Established");
                    Log.d("bluetoothdevice",bluetoothDevice);
                    if (tmp.isConnected()) {
                        Log.d("BluetoothSocket","connected");
                        sensorRegistered = true;
                        TextView sensorReg = findViewById(R.id.score_sensorRegistered);
                        sensorReg.setText("Sensor Registered: true");
                        sensorReg.setBackgroundColor(Color.LTGRAY);
                    }
                    else {
                        Log.d("BluetoothSocket","not connected");
                        sensorRegistered = true; //TODO: RETURN TO FALSE
                        TextView sensorReg = findViewById(R.id.score_sensorRegistered);
                        sensorReg.setText("Sensor Registered: false");
                        sensorReg.setBackgroundColor(Color.RED);
                    }
                }
                catch(java.io.IOException e) {
                    e.getStackTrace();
                    Log.d("BluetoothSocket","could not be established");
                    sensorRegistered = true; //TODO: RETURN TO FALSE
                    TextView sensorReg = findViewById(R.id.score_sensorRegistered);
                    sensorReg.setText("Sensor Registered: false");
                    sensorReg.setBackgroundColor(Color.RED);
                }


            }
        }
        TextView sensorReg = findViewById(R.id.score_sensorRegistered);
        sensorReg.setText("Sensor Registered: true");
        sensorReg.setBackgroundColor(Color.LTGRAY);
        sensorRegistered = true; //TODO: RETURN TO FALSE
        //TextView sensorReg = findViewById(R.id.score_sensorRegistered);
        //sensorReg.setText("Sensor Registered: false");
        //sensorReg.setBackgroundColor(Color.RED);

    }

    private final BroadcastReceiver BTReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("Bluetooth", "connection received");
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            String bluetoothDevice = device.getName();
            int startingIndex = bluetoothDevice.indexOf("SGM");

            if (startingIndex != -1) {
                Log.d("Sensor","Sensor paired");
                sensorID = bluetoothDevice;
                SharedPreferences.Editor prefEditor = userData.edit();
                prefEditor.putString(GlobalParams.SENSOR_KEY, sensorID);
                prefEditor.apply();

                if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    sensorRegistered = true;
                    TextView sensorReg = findViewById(R.id.score_sensorRegistered);
                    sensorReg.setText("Sensor Registered: true");
                    sensorReg.setBackgroundColor(Color.LTGRAY);
                    Toast.makeText(getApplicationContext(), "BT Connected", Toast.LENGTH_SHORT).show();
                }

                else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    sensorRegistered = true; //TODO: RETURN TO FALSE
                    TextView sensorReg = findViewById(R.id.score_sensorRegistered);
                    sensorReg.setText("Sensor Registered: false");
                    sensorReg.setBackgroundColor(Color.RED);
                    Toast.makeText(getApplicationContext(), "BT Disconnected", Toast.LENGTH_SHORT).show();
                }
            }
            TextView sensorReg = findViewById(R.id.score_sensorRegistered); //TODO: RETURN TO FALSE
            sensorReg.setText("Sensor Registered: true");
            sensorReg.setBackgroundColor(Color.LTGRAY);

        }
    };

    public void checkAtSpeed() {
        if (mAccelCurrent >= GlobalParams.ACC_CUTOFF) {
            atSpeed = false;
            Log.d("speed","too fast");
        }
        else {
            atSpeed = true;
        }
    }


    public void initiateRest() {
        if (!walking) {
            elapsedRestTime = SystemClock.elapsedRealtime() - startRestTime;
            elapsedActiveTime = SystemClock.elapsedRealtime() - startActiveTime;
            assignPoint();
            Log.d("active time", Double.toString(elapsedActiveTime));
            if (elapsedRestTime >= GlobalParams.ACTIVE_CUTOFF) {
                //userScore += GlobalParams.ACTIVE_CUTOFF/GlobalParams.MILLIS_TO_MINUTES; //TODO: FIX REST SCORE ISSUE
                active = false;
                TextView scoreDisp = findViewById(R.id.score_totalScoreDisplay);
                scoreDisp.setBackgroundColor(Color.RED);
                Log.d("scoring", "set inactive");
            }
        } else {
            startRestTime = SystemClock.elapsedRealtime();
        }
    }

    public void initiateActivity() {
        if (walking) {
            elapsedActiveTime = SystemClock.elapsedRealtime() - startActiveTime;
            Log.d("active time", Double.toString(elapsedActiveTime));
            assignPoint();
        } else {
            active = true;
            //Log.d("scoring", "set Active");
            TextView scoreDisp = findViewById(R.id.score_totalScoreDisplay);
            scoreDisp.setBackgroundColor(Color.GREEN);
            startActiveTime = SystemClock.elapsedRealtime();
        }
    }

    public void assignPoint() {
        if (elapsedActiveTime >= GlobalParams.POINT_TIME) {
            //checkAttached();
            checkAtSpeed();
            int point = (sensorRegistered ? 1 : 0) * (active ? 1 : 0) * (atSpeed ? 1 : 0);
            userActiveHoursScore += point;
            userTotalScore += point;

            SharedPreferences.Editor prefEditor = userData.edit();
            prefEditor.putInt(GlobalParams.ACTIVEHOURS_SCORE_KEY, userActiveHoursScore);
            prefEditor.putInt(GlobalParams.QRCODE_SCORE_KEY, userQRCodeScore);
            prefEditor.putInt(GlobalParams.TOTAL_SCORE_KEY, userTotalScore);
            prefEditor.apply();
            Log.d("scoring", "Point assigned");


            String scoreDisplay = Integer.toString(userTotalScore);
            TextView totalScoreDisplay = (TextView) findViewById(R.id.score_totalScoreDisplay);
            totalScoreDisplay.setText(scoreDisplay);

            scoreDisplay = Integer.toString(userActiveHoursScore);
            TextView activeHoursScoreDisplay = (TextView) findViewById(R.id.score_activeHoursDisplay);
            activeHoursScoreDisplay.setText(scoreDisplay);

            elapsedActiveTime = 0;
            startActiveTime = SystemClock.elapsedRealtime();
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
        url += "Sheet=" + "Event1" + '&';
        url += "RequestType=DataPush&";
        url += "SensorID=" + QRActivity.getSensorID().substring(5,9) + '&';
        url += "ActiveMinPoints=" + userActiveHoursScore + '&';
        url += "QRCodePoints=" + userQRCodeScore;
        url = QRActivity.ensureValidURL(url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String str = response.toString();
                        Log.e("QR JSON response",str);
                        boolean result = false;
                        try {
                            result = response.getString("result").equals("success");
                        }
                        catch (JSONException exception) {
                            InfoDisplay.setText("JSON String returned by server has no field 'result'.");
                        }
                        if (result) {
                            try {
                                InfoDisplay.setText("Successfully updated server, with our manual offset you have "
                                        + response.getInt("remotePoints") + " points on our servers.");
                            }
                            catch (JSONException e) {
                                InfoDisplay.setText("This should never appear: Email us if you see this USERDATABASEERROR");
                            }
                        }
                        else {
                            try {
                                InfoDisplay.setText("Connected to server but failed to push local score: " +
                                        response.getString("error"));
                            }
                            catch (JSONException e) {
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

}