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


/**
 * Navigation Activity:
 *     Initializes crash handler (catches exceptions not caught by try/catch blocks)
 *     Checks for remote updates
 *     Provides user with navigation bar
 *     Note: Upon app startup, user must open ActiveHoursActivity to initiate scoring. User must open
 *         MapsActivity to initiate location tracking before scanning QR Codes.
 */
public class Navigation extends AppCompatActivity {

    private TextView mTextMessage;

    /**
     * The menu on the bottom of the screen.
     */
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        /**
         * Defines the app behavior when a button from the menu is selected.
         * @param item
         * @return
         */
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {

                case R.id.navigation_scoring:
                    Intent homeIntent = new Intent(getApplicationContext(), ActiveHoursActivity.class);
                    startActivity(homeIntent);
                    return true;
                case R.id.navigation_puzzles:
                    Intent puzzleIntent = new Intent(getApplicationContext(), PuzzleInputActivity.class);
                    startActivity(puzzleIntent);
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

    /**
     * onCreate method for this Activity installs the crash handler, checks for a remote update, and
     * instantiates the bottom menu.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final String TAG = "MainActivity";
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_navigation);

        // Install the application crash handler
        ApplicationCrashHandler.installHandler();

        Log.d("Startup","Starting up navigation");


        // Do remote update
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
        // Set layout
        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

}
