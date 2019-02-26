package com.example.david.alpha;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;
import android.net.Uri;
import android.widget.Toast;
import android.util.Log;

import java.net.URL;

/*
    Navigation Activity:
    Initializes crash handler (catches exceptions not caught by try/catch blocks)
    Checks for remote updates
    Provides user with navigation bar
    Note: Upon app startup, user must open ActiveHoursActivity to initiate scoring. User must open
        MapsActivity to initiate location tracking before scanning QR Codes.
    2nd semester to-do:
        User does not need to open other activities to initiate scoring/location tracking. Upon phone
        startup, user should be earning active detector hours and be able to go directly to the QR
        Code tab to scan a QR code.
        Better UI: Potentially have large blocks to jump to various activities rather than a bottom
            navigation bar that disappears when moving to other activities
    David Linn, Joshua Morgan - dlinn@hmc.edu, jmorgan@hmc.edu - 12/7/18
 */
public class Navigation extends AppCompatActivity {

    private TextView mTextMessage;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {

                case R.id.navigation_scoring:
                    Intent homeIntent = new Intent(getApplicationContext(), ActiveHoursActivity.class);
                    startActivity(homeIntent);
                    //mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.navigation_puzzles:
                    Intent puzzleIntent = new Intent(getApplicationContext(), PuzzleInputActivity.class);
                    startActivity(puzzleIntent);
                    //mTextMessage.setText("Puzzles");
                    return true;
                case R.id.navigation_qrScanner:
                    Intent qrIntent = new Intent(getApplicationContext(), QRActivity.class);
                    startActivity(qrIntent);
                    return true;
                case R.id.navigation_navigate:
                    Intent mapsIntent = new Intent(getApplicationContext(), MapsActivity.class);
                    startActivity(mapsIntent);
                    return true;
                case R.id.navigation_leaderboard:
                    Intent leadIntent = new Intent(getApplicationContext(), LeaderboardActivity.class);
                    startActivity(leadIntent);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final String TAG = "MainActivity";
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        // Install the application crash handler
        ApplicationCrashHandler.installHandler();

        // Record the start-up time
        //long currentTime = System.currentTimeMillis();
        //long elapsedTime = currentTime - ApplicationWrapper.startTime;
        //if (elapsedTime > 3000) {
          //  Log.e(TAG, "LONG STARTUP TIME");
        //}
        //Log.d(TAG, String.format("Elapsed Time = %d ms", elapsedTime));


        Log.d("Startup","Starting up navigation");
        //TODO: CHECK REMOTE UPDATE FUNCTIONALITY

        try {
            Intent mServiceIntent = new Intent(this, RemoteUpdateService.class);
            mServiceIntent.setData(Uri.parse(GlobalParams.REMOTE_UPDATE_HOSTURL));
            startService(mServiceIntent);
            Log.d("update","attempting update");
        }
        catch(Exception e) {
            Toast.makeText(this,"Could not update", Toast.LENGTH_SHORT);
            Log.d("update", "Update failed");
        }


        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

}
