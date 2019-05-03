package com.example.david.alpha;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import android.webkit.WebView;

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

/**
 *    QR Activity:
 *     Scans a QR Code using classes in barcode and camera directories
 *     Pushes ground truth to Ground Truth database, sheet specified by 4-character code (first 4 characters
 *     of 6-character QR code string). Final 2 digits of QR code specify stop number. Also includes
 *     SensorID, Serial number, Location (if MapsActivity opened since app startup), and local time.
 *     Add points according to QR Code type ('D','P', or 'T' - Daily, Puzzle, or Test)
 *     David Linn - dlinn@hmc.edu - 12/7/18
 *     Richie Harris, Tim Player - rkharris@hmc.edu, tplayer@hmc.edu - 4/6/19
 */

public class QRActivity extends AppCompatActivity {

    private static final String LOG_TAG = QRActivity.class.getSimpleName();
    private static final int BARCODE_READER_REQUEST_CODE = 1;
    private TextView mResultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //userData = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);

        setContentView(R.layout.activity_qr);

        mResultTextView = findViewById(R.id.result_textview);

        Button scanBarcodeButton = findViewById(R.id.scan_barcode_button);
        // Start barcode scanner activity when button is pressed.  Have the activity return the
        // result of the barcode scan back to this activity
        scanBarcodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), BarcodeCaptureActivity.class);
                startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);
            }
        });
    }

    /**
     * Called when the barcode scanner activity finishes and returns.  Uses the result of the
     * barcode scan to set the text display.  Calls the groundTruth method.
     *
     * @param requestCode The request code passed to startActivityForResult()
     * @param resultCode The result code specified by the barcode reader, either RESULT_OK or RESULT_CANCELLED
     * @param data carries the barcode data from the barcode scanner activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BARCODE_READER_REQUEST_CODE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    Point[] p = barcode.cornerPoints;
                    mResultTextView.setText(barcode.displayValue);
                    Log.d("qr substring", barcode.displayValue.substring(0,4));
                    if (barcode.displayValue.length() == 6) {//QR codes should return String of len 6
                        groundTruth(barcode.displayValue);
                    }
                    else if (barcode.displayValue.substring(0,4).equals("http")) {// puzzle website
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(barcode.displayValue));
                        intent.setPackage("com.android.chrome");
                        startActivity(intent);
                    }
                    else
                        mResultTextView.setText(barcode.displayValue+", Invalid QR code");
                } else mResultTextView.setText(R.string.no_barcode_captured);
            } else Log.e(LOG_TAG, String.format(getString(R.string.barcode_error_format),
                    CommonStatusCodes.getStatusCodeString(resultCode)));
        } else super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Sends a JSON request to the Ground Truth spreadsheet.  The sheet returns the character
     * (letter) representing the scan type, the puzzle answer prerequisite associated with the
     * QR code if the QR scan was a bonus scan, and the stop number (last 2 digits of the QR scan).
     * Points are awarded to the user according to the scan type, and whether the scan was a puzzle
     * scan or a bonus scan.
     *
     * @param qrResult the 6 character result obtained from the QR scan.  First 4 characters are
     *                 the sheet name.  Last 2 characters are the stop number.
     */
    protected void groundTruth(String qrResult) {
        try {
            //Create queue that accepts requests
            RequestQueue queue = Volley.newRequestQueue(this);
            //Build URL and query string from JSON object
            String url = getApplicationContext().getString(R.string.ground_truth_script_url);
            url += '?';
            url += "Sheet=" + qrResult.substring(0, 4) + '&';
            String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
            url += "LocalTime=" + currentDateTimeString + '&';
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

                                //Check the JSON response for a "Prerequisite" field. If there is
                                //a prerequisite, that means the user must have completed a puzzle
                                // ("prereq") in order to qualify for ten additional points for that
                                // scan.  Also get the scanType and currentScan stop number
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

                                if (!prereq.isEmpty()){ //if there is a prerequisite to scan this QR code, require it.
                                    if(UserDataUtils.wasPuzzleCompleted(prereq)){

                                        if(UserDataUtils.wasBonusCollected(prereq)){
                                            mResultTextView.setText("Successfully updated Google Sheet." + "\nYou have already used your bonus.");
                                        }else{
                                            UserDataUtils.incrementUserQRCodeScore(20);
                                            mResultTextView.setText("Successfully updated Google Sheet. You got 20 bonus points!" + "\nYou have used your bonus.");
                                            UserDataUtils.setBonusCollected(prereq);
                                        }

                                    } else {
                                        mResultTextView.setText("Successfully updated Google Sheet." + "\nYou have not completed the required puzzle yet.");
                                    }
                                }
                                else { // no prerequisite
                                    String lastScan = UserDataUtils.getLastScan();
                                    int pointsToAdd = 0;
                                    if (!currentScan.equals(lastScan)) { // user cannot scan the same QR code multiple times for points
                                        switch (scanType) {
                                            case "T": // Test
                                                pointsToAdd = 4;
                                                Log.d("QR type", "T");
                                                break;
                                            case "D": // Daily
                                                pointsToAdd = 12;
                                                Log.d("QR type", "D");
                                                break;
                                            case "M": // Puzzle.  Changed sheet name to start with MSTR
                                                pointsToAdd = 5;
                                                Log.d("QR type", "M");
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                    UserDataUtils.incrementUserQRCodeScore(pointsToAdd);
                                    mResultTextView.setText("Successfully updated Google Sheet." + "\n" + Integer.toString(pointsToAdd) + " points added.");

                                }
                                UserDataUtils.setLastScan(currentScan);
                            }
                            else{
                                mResultTextView.setText("Connected to server but failed to update Google Sheet");
                            }

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

    /**
     * Obtains the sensor ID of the paired radiation detector for identification in the Ground
     * Truth Google sheet.
     *
     * @return the ID of the paired detector
     */
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

    /**
     * Ensures that the JSON request URL is valid
     * @param url URL for JSON request
     * @return the corrected URL if there were any spaces that needed to be changed to + characters
     */
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
