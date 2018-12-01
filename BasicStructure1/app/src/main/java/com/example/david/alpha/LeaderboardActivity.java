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

import org.json.JSONArray;
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
        final TextView[] name = {(TextView) findViewById(R.id.name0),(TextView) findViewById(R.id.name1),
                (TextView) findViewById(R.id.name2),(TextView) findViewById(R.id.name3),(TextView) findViewById(R.id.name4)};
        final TextView[] score = {(TextView) findViewById(R.id.score0),(TextView) findViewById(R.id.score1),
                (TextView) findViewById(R.id.score2),(TextView) findViewById(R.id.score3),(TextView) findViewById(R.id.score4)};
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
                            name[0].setText("JSON String returned by server has no field 'result'.");
                        }
                        if (result) {
                            try {
                                JSONArray names = response.getJSONArray("leaderboardNames");
                                for (int i = 0; i < 5; i++)
                                    name[i].setText(names.getJSONArray(i).getString(0));
                                JSONArray scores = response.getJSONArray("leaderboardScores");
                                for (int i = 0; i < 5; i++)
                                    score[i].setText(scores.getJSONArray(i).getString(0));
                            }
                            catch (JSONException e) {
                                name[0].setText("This should never appear: Email us if you see this USERDATABASEERROR");
                            }
                        }
                        else {
                            try {
                                name[0].setText("Connected to server but failed to push local score: " +
                                        response.getString("error"));
                            }
                            catch (JSONException e) {
                                name[0].setText("This should never appear: Email us if you see this USERDATABASEERROR");
                            }
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        name[0].setText("Error in HTTP Request");
                    }
                });

        queue.add(jsonObjectRequest);
    }
}
