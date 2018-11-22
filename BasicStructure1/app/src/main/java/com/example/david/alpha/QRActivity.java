package com.example.david.alpha;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Point;
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

public class QRActivity extends AppCompatActivity {
    private static final String LOG_TAG = QRActivity.class.getSimpleName();
    private static final int BARCODE_READER_REQUEST_CODE = 1;

    private TextView mResultTextView;

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
            if (MapsActivity.myPos != null) {
                url += "Lat=" + MapsActivity.myPos.latitude + '&'; //TO DO: More accurate GPS Position
                url += "Long=" + MapsActivity.myPos.longitude + '&';
            }
            url += "SensorID=" + getSensorID();
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
