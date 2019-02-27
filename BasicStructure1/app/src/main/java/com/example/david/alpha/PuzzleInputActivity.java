package com.example.david.alpha;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
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

/*
    Puzzle input activity:  Allows user to input puzzle answers.  Initiates request to google sheets
    database containing puzzle answers.  Compares input to correct answer and resturns if the user
    got the puzzle right.
 */

public class PuzzleInputActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle_input);
    }

    public void submitInput(View view) {
        final TextView AnswerDisplay = (TextView) findViewById(R.id.input_serverinfo);
        EditText editText = (EditText) findViewById(R.id.editText);
        String answer = editText.getText().toString();
        //Create queue that accepts requests
        RequestQueue queue = Volley.newRequestQueue(this);
        //Build URL and query string from JSON object
        String url = getApplicationContext().getString(R.string.user_database_url);
        url += '?';
        url += "Sheet=" + "Event2" + '&';
        url += "answer=" + answer;
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
                            AnswerDisplay.setText("failed");
                        }
                        if (result) {
                            try {
                                AnswerDisplay.setText("success");
                            }
                            catch (JSONException e) {
                                AnswerDisplay.setText("failed");
                            }
                        }
                        else {
                            try {
                                AnswerDisplay.setText("failed");
                            }
                            catch (JSONException e) {
                                AnswerDisplay.setText("failed");
                            }
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AnswerDisplay.setText("Error in HTTP Request");
                    }
                });

        queue.add(jsonObjectRequest);
    }
}
