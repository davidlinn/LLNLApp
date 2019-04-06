package com.example.david.alpha;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import static android.content.ContentValues.TAG;

public class ActiveHoursService extends Service implements SensorEventListener {
    private int hitCount = 0;
    private double hitSum = 0;
    private double hitResult = 0;

    private int SAMPLE_SIZE = GlobalParams.ACC_SAMPLE_SIZE;
    private double THRESHOLD = GlobalParams.ACC_THRESHOLD;
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

    public static boolean active = false;

    private static long startRestTime;
    private static long elapsedRestTime;
    private static long startActiveTime;
    private static long elapsedActiveTime;

    public static SharedPreferences userData;
    public String sharedPrefFile = "com.example.david.alpha";

    @Override // This service is not intended to be bound.
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values.clone();
            double x = mGravity[0];
            double y = mGravity[1];
            double z = mGravity[2];

            //Log.d("acceleration", "x = " + Double.toString(x) + ", " + "y = " + Double.toString(y) + ", " + "z = " + Double.toString(x) + ", ");
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt(x * x + y * y + z * z); // is there a more efficient way to norm?
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

    public void checkAtSpeed() {
        if (mAccelCurrent >= GlobalParams.ACC_CUTOFF) { //has no correlation to actual speed
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
            startActiveTime = SystemClock.elapsedRealtime();
        }
    }

    public void assignPoint() {
        if (elapsedActiveTime >= GlobalParams.POINT_TIME) {

            checkAtSpeed();
            int point = (sensorRegistered ? 1 : 0) * (active ? 1 : 0) * (atSpeed ? 1 : 0);

            UserDataUtils.incrementUserActiveHoursScore();

            elapsedActiveTime = 0;
            startActiveTime = SystemClock.elapsedRealtime();
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // deal with sensor accuracy change
    }

}
