package com.example.david.alpha;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class LeaderboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);
        leaderboardRequest();
    }

    public void leaderboardRequest() {
        //Populate the server info view depending on request result
        final TextView InfoDisplay = (TextView) findViewById(R.id.leaderboardView);
        //Create queue that accepts requests
        RequestQueue queue = Volley.newRequestQueue(this);
        //Build URL and query string from JSON object
        String url = getApplicationContext().getString(R.string.user_database_url);
        url += '?';
        url += "Sheet=" + "Event1" + '&';
        url += "RequestType=Leaderboard";
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
                                InfoDisplay.setText(response.getJSONArray("leaderboardNames").toString()+
                                        response.getJSONArray("leaderboardScores").toString());
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
