package com.example.david.alpha;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.os.Bundle;
import android.os.SystemClock;

import com.google.android.gms.maps.model.LatLng;

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
    private static String sensorID;
    private static int userScore;
    public static boolean active;

    private static long startRestTime;
    private static long elapsedRestTime;
    private static long startActiveTime;
    private static long elapsedActiveTime;

    private static SharedPreferences userData;
    private String sharedPrefFile = "com.example.david.alpha";

    public void onAccuracyChanged(Sensor sensor, int num) {
        Log.d("accelerometer ", "accuracy changed");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scoring);

        userData = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);

        userScore = userData.getInt(GlobalParams.SCORE_KEY, 0);

        sensorMan = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        sensorMan.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL);
        checkAttached();
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

            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float)Math.sqrt(x * x + y * y + z * z);
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
                    Log.d("Accelerometer: ", "Walking");
                    initiateActivity();
                    walking = true;
                } else {
                    Log.d("Accelerometer: ", "Not Walking");
                    initiateRest();
                    walking = false;
                }
            }

            Log.d("UserScore", Integer.toString(userScore));
            hitCount = 0;
            hitSum = 0;
            hitResult = 0;
        }
    }

    //David Edit 11/21: Simplified logic and ensured sensors that don't start with
    //  but contain "SGM" are included
    public static void checkAttached() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        for (BluetoothDevice bt : pairedDevices) {
            String bluetoothDevice = bt.getName();
            int startingIndex = bluetoothDevice.indexOf("SGM");
            if (startingIndex != -1) {

                sensorRegistered = true;

                SharedPreferences.Editor preferencesEditor = userData.edit();
                preferencesEditor.putString(GlobalParams.SENSOR_KEY, bluetoothDevice);
                preferencesEditor.apply();
            }
        }
        sensorRegistered = false;
    }


    public static void initiateRest() {
        if (!walking) {
            elapsedRestTime = SystemClock.elapsedRealtime() - startRestTime;
            if (elapsedRestTime == GlobalParams.ACTIVE_CUTOFF) {
                active = false;
                Log.d("scoring", "set inactive");
            }
        } else {
            startRestTime = SystemClock.elapsedRealtime();
            elapsedRestTime = 0;
        }
    }

    public static void initiateActivity() {
        if (walking) {
            elapsedActiveTime = SystemClock.elapsedRealtime() - startActiveTime;
            if (elapsedActiveTime >= GlobalParams.POINT_TIME) {
                startRestTime = SystemClock.elapsedRealtime();

                SharedPreferences.Editor prefEditor = userData.edit();
                prefEditor.putInt("USER_SCORE", userScore + (sensorRegistered ? 1 : 0) * (active ? 1 : 0));
                prefEditor.apply();

                userScore++;
                Log.d("scoring", "Point assigned");
                SharedPreferences.Editor preferencesEditor = userData.edit();
                preferencesEditor.putInt(GlobalParams.SCORE_KEY, userScore);
                preferencesEditor.apply();

                elapsedActiveTime = 0;
                startActiveTime = SystemClock.elapsedRealtime();
            }
        } else {
            active = true;
            Log.d("scoring", "set Active");
            startActiveTime = SystemClock.elapsedRealtime();
        }
    }

}

