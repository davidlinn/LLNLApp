package com.example.david.alpha;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

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
                    mTextMessage.setText("Puzzles");
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
                    mTextMessage.setText("In development");
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

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

}
