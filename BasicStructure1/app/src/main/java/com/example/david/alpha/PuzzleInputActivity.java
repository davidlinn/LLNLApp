package com.example.david.alpha;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
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


import static com.example.david.alpha.ActiveHoursActivity.userData;

/*
    Puzzle input activity:  Allows user to input puzzle answers.  Initiates request to google sheets
    database containing puzzle answers.  Compares input to correct answer and resturns if the user
    got the puzzle right.
 */

public class PuzzleInputActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private static String PuzzleID;
    //private static String Puzzle1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_puzzle_input);

        getPuzzleOptions();

        //userData = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);
        //Puzzle1 = userData.getString(GlobalParams.PUZZLEID_KEY, "Incomplete");
    }

    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        String selectedPuzzle = parent.getItemAtPosition(pos).toString();
        PuzzleID = selectedPuzzle;
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
        PuzzleID = "PUZZLE1";
    }

    public void getPuzzleOptions() {
        final TextView AnswerDisplay = (TextView) findViewById(R.id.input_serverinfo);
        //Create queue that accepts requests
        RequestQueue queue = Volley.newRequestQueue(this);
        //Build URL and query string from JSON object
        String url = getApplicationContext().getString(R.string.answer_submission_url);
        url += '?';
        url += "Sheet=" + "Sheet1" + '&';
        url += "RequestType=" + "GetPuzzleNames";
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

                            JSONArray arrJson = response.getJSONArray("puzzleNames");
                            String[] puzzleNames = new String[arrJson.length()];
                            for(int i=0;i<arrJson.length();i++)
                            {
                                puzzleNames[i] = arrJson.getString(i);
                            }
                            Spinner spinner = (Spinner) findViewById(R.id.puzzleSelect_spinner);
                            // Create an ArrayAdapter using the string array and a default spinner layout
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(PuzzleInputActivity.this, android.R.layout.simple_spinner_item, puzzleNames);
                            // Specify the layout to use when the list of choices appears
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            // Apply the adapter to the spinner
                            spinner.setAdapter(adapter);
                            spinner.setOnItemSelectedListener(PuzzleInputActivity.this); //this
                        }
                        catch (JSONException exception) {
                            AnswerDisplay.setText("Json request failed");
                        }
                            //SharedPreferences.Editor prefEditor = userData.edit();
                            //prefEditor.putString(GlobalParams.PUZZLEID_KEY, PuzzleID);
                            //prefEditor.apply();
                            //}
                            //catch (JSONException e) {
                            //    AnswerDisplay.setText("failed");
                            //}
                        }/*
                        else {
                            try {
                                AnswerDisplay.setText("failed");
                            }
                            catch (JSONException e) {
                                AnswerDisplay.setText("failed");
                            }
                        }*/

                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AnswerDisplay.setText("Error in HTTP Request");
                    }
                });

        queue.add(jsonObjectRequest);
    }

    public void submitInput(View view) {
        final TextView AnswerDisplay = (TextView) findViewById(R.id.input_serverinfo);
        String confirmation = "Answer submitted.  Checking answer...";
        AnswerDisplay.setText(confirmation);
        EditText editText = (EditText) findViewById(R.id.editText);
        String answer = editText.getText().toString();
        //Create queue that accepts requests
        RequestQueue queue = Volley.newRequestQueue(this);
        //Build URL and query string from JSON object
        String url = getApplicationContext().getString(R.string.answer_submission_url);
        url += '?';
        url += "Sheet=" + "Sheet1" + '&';
        url += "SensorID=" + QRActivity.getSensorID().substring(5,9) + '&';
        url += "RequestType=" + "AnswerSubmission" + "&";
        url += "PuzzleID=" + PuzzleID + '&';
        url += "SubmittedAnswer=" + answer;
        url = QRActivity.ensureValidURL(url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String str = response.toString();
                        Log.e("QR JSON response",str);
                        boolean result = false;
                        String correctness = "";
                        boolean alreadyCompleted = false;
                        try {
                            result = response.getString("result").equals("success");
                            correctness = response.getString(("correct?"));
                            alreadyCompleted = response.getBoolean(("alreadyCompleted?"));
                        }
                        catch (JSONException exception) {
                            AnswerDisplay.setText(correctness);
                        }
                        if (result) {
                            //try {
                                AnswerDisplay.setText(correctness);
                                if (correctness.equals("Correct!") && alreadyCompleted == false) {
                                    int pointsToAdd = 500;
                                    ActiveHoursActivity.userQRCodeScore += pointsToAdd;
                                    ActiveHoursActivity.userTotalScore += pointsToAdd;
                                    SharedPreferences.Editor prefEditor = ActiveHoursActivity.userData.edit();
                                    prefEditor.putInt(GlobalParams.QRCODE_SCORE_KEY, ActiveHoursActivity.userQRCodeScore);
                                    prefEditor.putInt(GlobalParams.TOTAL_SCORE_KEY, ActiveHoursActivity.userTotalScore);
                                    prefEditor.apply();


                                }
                                //SharedPreferences.Editor prefEditor = userData.edit();
                                //prefEditor.putString(GlobalParams.PUZZLEID_KEY, PuzzleID);
                                //prefEditor.apply();
                            //}
                            //catch (JSONException e) {
                            //    AnswerDisplay.setText("failed");
                            //}
                        }/*
                        else {
                            try {
                                AnswerDisplay.setText("failed");
                            }
                            catch (JSONException e) {
                                AnswerDisplay.setText("failed");
                            }
                        }*/
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
