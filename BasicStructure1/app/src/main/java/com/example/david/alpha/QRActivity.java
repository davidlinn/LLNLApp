package com.example.david.alpha;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.david.alpha.barcode.BarcodeCaptureActivity;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class QRActivity extends AppCompatActivity {
    private static final String LOG_TAG = QRActivity.class.getSimpleName();
    private static final int BARCODE_READER_REQUEST_CODE = 1;

    private TextView mResultTextView;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    FusedLocationProviderClient mFusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        mResultTextView = findViewById(R.id.result_textview);

        Button scanBarcodeButton = findViewById(R.id.scan_barcode_button);
        scanBarcodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
                startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
            }
        });

        //Start location updating as soon as activity started
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(100); // one-second update interval.
        mLocationRequest.setFastestInterval(10); // 10ms upper limit
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
    }

    @Override
    public void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BARCODE_READER_REQUEST_CODE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    Point[] p = barcode.cornerPoints;
                    mResultTextView.setText(barcode.displayValue);
                    if (barcode.displayValue.length() == 6) //QR codes should return String of len 6
                        groundTruth(barcode.displayValue);
                    else
                        mResultTextView.setText(barcode.displayValue+", Invalid QR code");
                } else mResultTextView.setText(R.string.no_barcode_captured);
            } else Log.e(LOG_TAG, String.format(getString(R.string.barcode_error_format),
                    CommonStatusCodes.getStatusCodeString(resultCode)));
        } else super.onActivityResult(requestCode, resultCode, data);
    }

    protected void groundTruth(String qrResult) {
        try {
            //Create queue that accepts requests
            RequestQueue queue = Volley.newRequestQueue(this);
            //Build URL and query string from JSON object
            String url = getApplicationContext().getString(R.string.ground_truth_script_url);
            url += '?';
            url += "Sheet=" + qrResult.substring(0, 4) + '&';
            String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
            url += "LocalTime=" + currentDateTimeString + '&'; //TO DO: Remove spaces in Date/Time
            url += "Stop=" + qrResult.substring(4,6) + '&';
            long loopStartTime = System.currentTimeMillis();
            long loopTimeLimit = 3000; //3 secs
            while (mLastLocation == null && System.currentTimeMillis()-loopStartTime < loopTimeLimit);
            if (mLastLocation != null) {
                url += "Lat=" + mLastLocation.getLatitude() + '&'; //TO DO: More accurate GPS Position
                url += "Long=" + mLastLocation.getLongitude() + '&';
            }
            url += "SensorID=" + getSensorID();
            url = ensureValidURL(url);
            Log.e("QRActivity URL",url);
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                    (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            String str = response.toString();
                            Log.e("QR JSON response",str);
                            boolean result = false;
                            try {
                                result = (response.getString("result") == "success");
                            }
                            catch (JSONException exception) {
                                mResultTextView.setText("JSON String returned by server has no field 'result'.");
                            }
                            if (result)
                                mResultTextView.setText("Successfully updated Google Sheet");
                            else
                                mResultTextView.setText("Connected to server but failed to update Google Sheet");
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            mResultTextView.setText("Error in HTTP Request");
                        }
                    });
            queue.add(jsonObjectRequest);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static String getSensorID() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        for (BluetoothDevice bt : pairedDevices) {
            String bluetoothDevice = bt.getName();
            if (bluetoothDevice.length() >= 3) {
                if (bluetoothDevice.substring(0, 3) == "SGM") {
                    return bluetoothDevice;
                }
            }
        }
        return "NoSensorConnected";
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("QRActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;
            }
        }
    };

    public static String ensureValidURL(String url) {
        //Turn all spaces in String into '+' characters
        String s = "";
        for (char c : url.toCharArray()) {
            if (c == ' ')
                s = s+'+';
            else
                s = s+c;
        }
        return s;
    }

}
