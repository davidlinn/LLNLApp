package com.example.david.alpha;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Build;
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
import com.google.android.gms.vision.barcode.Barcode;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.Set;

/*
    QR Activity:
    Scans a QR Code using classes in barcode and camera directories
        Implementation from [link]
    Pushes ground truth to Ground Truth database, sheet specified by 4-character code (first 4 characters
        of 6-character QR code string). Final 2 digits of QR code specify stop number. Also includes
        SensorID, Serial number, Location (if MapsActivity opened since app startup), and local time.
    Add points according to QR Code type ('D','P', or 'T' - Daily, Puzzle, or Test)
    David Linn - dlinn@hmc.edu - 12/7/18
 */
public class QRActivity extends AppCompatActivity {
    private static final String LOG_TAG = QRActivity.class.getSimpleName();
    private static final int BARCODE_READER_REQUEST_CODE = 1;

    private TextView mResultTextView;

    public static SharedPreferences userData;
    public String sharedPrefFile = "com.example.david.alpha";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userData = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);

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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BARCODE_READER_REQUEST_CODE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    Point[] p = barcode.cornerPoints;
                    mResultTextView.setText(barcode.displayValue);
                    if (barcode.displayValue.length() == 6) {//QR codes should return String of len 6
                        groundTruth(barcode.displayValue);
                    }
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
            if (MapsActivity.myPos != null) {
                url += "Lat=" + MapsActivity.myPos.latitude + '&'; //TO DO: More accurate GPS Position
                url += "Long=" + MapsActivity.myPos.longitude + '&';
            }
            url += "RequestType=" + "QRScan" + '&';
            url += "ScanType=" + qrResult.substring(0,1) + '&';
            url += "SensorID=" + getSensorID() + '&';
            if (android.os.Build.VERSION.SDK_INT < 26)
                url += "PhoneSerial=" + Build.SERIAL;
            url = ensureValidURL(url);


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
                                mResultTextView.setText("JSON String returned by server has no field 'result'.");
                            }
                            if (result) {
                                mResultTextView.setText("Successfully updated Google Sheet");

                                //Check the JSON response for a "Prerequisite" field. If there is
                                //a prerequisite, that means the user must have completed a puzzle
                                // ("prereq") in order to qualify for ten additional points for that
                                // scan.
                                String prereq = "";
                                String scanType = "";
                                String currentScan = "";

                                try{
                                    prereq = response.getString("Prerequisite");
                                }catch (JSONException exception){
                                    Log.e("No Prerequisite field", str);
                                }

                                try{
                                    scanType = response.getString("ScanType");
                                }catch (JSONException exception){
                                    Log.e("No scan type field", str);
                                }

                                try{
                                    currentScan = response.getString("StopID");
                                }catch (JSONException exception){
                                    Log.e("No StopID field", str);
                                }

                                if (!prereq.isEmpty()){ //if there is a prerequisite, require it.
                                    if(UserDataUtils.wasPuzzleCompleted(prereq)){
                                        UserDataUtils.incrementUserQRCodeScore(20);
                                        mResultTextView.setText("Successfully updated Google Sheet. You got bonus points!" + "\nYou have used your bonus.");
                                        UserDataUtils.putBoolean(prereq, false); //toggle their prerequisite off so they cannot scan for
                                        //extra points again.

                                        //ten points for Gryffindor! (also adds ten for being P class code)
                                    }
                                    else {
                                        mResultTextView.setText("Successfully updated Google Sheet." + "\nYou have already used your bonus.");
                                    }
                                }
                                else {
                                    String lastScan = UserDataUtils.getLastScan();
                                    int pointsToAdd = 0;
                                    if (currentScan.equals(lastScan) == false) {
                                        switch (scanType) {
                                            case "T":
                                                pointsToAdd = 4;
                                                Log.d("QR type", "T");
                                                break;
                                            case "D":
                                                pointsToAdd = 12;
                                                Log.d("QR type", "D");
                                                break;
                                            case "P":
                                                pointsToAdd = 5; // make 0 because we add the points already in the JSONRequest.  But no prereqs for daily?
                                                Log.d("QR type", "P");
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                    UserDataUtils.setLastScan(currentScan);
                                    try {
                                        UserDataUtils.incrementUserQRCodeScore(pointsToAdd);
                                    }
                                    catch (Exception e) {
                                        mResultTextView.setText("Couldn't add any QR points");
                                    }
                                }
                            }
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
            int startingIndex = bluetoothDevice.indexOf("SGM");
            if (startingIndex != -1) {
                return bluetoothDevice.substring(startingIndex);
            }
        }
        return "NoSensorConnected";
    }

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
