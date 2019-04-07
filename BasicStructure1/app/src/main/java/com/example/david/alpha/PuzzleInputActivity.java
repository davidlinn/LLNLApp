package com.example.david.alpha;

import android.arch.lifecycle.LifecycleObserver;
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

import com.android.volley.DefaultRetryPolicy;
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
    Puzzle input activity:
    Allows user to input puzzle answers.  Initiates JSON request to Answer Submission Google sheet
    database containing puzzle answers.  Compares input to correct answer and returns if the user
    got the puzzle answer correct.  Awards points for correct answers, checking if the correct
    answer was already submitted and points were already awarded.
    Richie Harris, Tim Player - rkharris@hmc.edu, tplayer@hmc.edu - 4/6/19
 */

public class PuzzleInputActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    /*
    I removed these. -Tim, 3/7/19
    //these values are taken from the userPreferences object that is set in
    //ActiveHoursActivity
    private static int userActiveHoursScore;
    public static int userQRCodeScore;
    public static int userTotalScore;
    */
    public static SharedPreferences userData;
    public String sharedPrefFile = "com.example.david.alpha";
    private static String PuzzleID;
    private final int MY_SOCKET_TIMEOUT_MS = 10000; // Sets the JSON request wait time to 10 seconds
    // so that it does not time out and repeat the request

    //private static String Puzzle1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userData = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);

        setContentView(R.layout.activity_puzzle_input);

        getPuzzleOptions(); // create the spinner for the different puzzles which can be answered
    }

    @Override
    public void onResume() {
        super.onResume();

        //read in Active Hours points from userData

    }

    /**
     * Called when the user selects a puzzle to submit an answer for.  Sets the variable puzzleID
     * to the puzzle which the user selected from the spinner dropdown menu.
     *
     * @param parent
     * @param view
     * @param pos
     * @param id
     */
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        String selectedPuzzle = parent.getItemAtPosition(pos).toString();
        PuzzleID = selectedPuzzle;
    }

    /**
     * If nothing has been selected, assume as default that the user is submitting an answer for
     * the first puzzle.
     *
     * @param parent
     */
    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
        PuzzleID = "PUZZLE1";
    }

    /**
     * Sends a JSON request to the Answer Submission Google sheet, and populates the spinner with
     * the results of the JSON response, which are the different puzzles that the user can
     * submit an answer for.
     */
    public void getPuzzleOptions() {
        final TextView AnswerDisplay = (TextView) findViewById(R.id.input_serverinfo);
        //Create queue that accepts requests
        RequestQueue queue = Volley.newRequestQueue(this);
        //Build URL and query string from JSON object
        String url = getApplicationContext().getString(R.string.answer_submission_url);
        url += '?';
        url += "Sheet=" + "Sheet1" + '&'; // May need to change sheet name for a new deployment
        url += "RequestType=" + "GetPuzzleNames";
        url = ensureValidURL(url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String str = response.toString();
                        Log.e("QR JSON response", str);
                        boolean result = false;
                        try {
                            result = response.getString("result").equals("success");
                            // convert JSON array to String[]
                            JSONArray arrJson = response.getJSONArray("puzzleNames");
                            String[] puzzleNames = new String[arrJson.length()];
                            for (int i = 0; i < arrJson.length(); i++) {
                                puzzleNames[i] = arrJson.getString(i);
                            }
                            Spinner spinner = (Spinner) findViewById(R.id.puzzleSelect_spinner);

                            // Create an ArrayAdapter using the string array and a default spinner layout
                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(PuzzleInputActivity.this, android.R.layout.simple_spinner_item, puzzleNames);

                            // Specify the layout to use when the list of choices appears
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                            // Apply the adapter to the spinner
                            spinner.setAdapter(adapter);
                            spinner.setOnItemSelectedListener(PuzzleInputActivity.this);

                        } catch (JSONException exception) {
                            AnswerDisplay.setText("Json request failed");
                            Log.e("JSON request failed.", exception.toString());
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

    /**
     * Called when the user presses the submit button.  Sends a JSON request to the Answer
     * Submission Google sheet.  The JSON response returns if the submitted answer was correct
     * or not.  If correct, award the user points and unlock the bonus QR code.  If the user
     * was incorrect or if the user has already submitted a correct answer, do not award points.
     *
     * @param view The submit button in the activity UI
     */
    public void submitInput(View view) {
        final TextView AnswerDisplay = (TextView) findViewById(R.id.input_serverinfo);
        String confirmation = "Answer submitted.  Checking answer...";
        AnswerDisplay.setText(confirmation);
        // Get the answer that the user submitted
        EditText editText = (EditText) findViewById(R.id.editText);
        String answer = editText.getText().toString().toLowerCase();
        //Create queue that accepts requests
        RequestQueue queue = Volley.newRequestQueue(this);
        //Build URL and query string from JSON object
        String url = getApplicationContext().getString(R.string.answer_submission_url);
        url += '?';
        url += "Sheet=" + "Sheet1" + '&'; // may need to change for a new deployment
        url += "SensorID=" + QRActivity.getSensorID().substring(5, 9) + '&';
        url += "RequestType=" + "AnswerSubmission" + "&";
        url += "PuzzleID=" + PuzzleID + '&';
        url += "SubmittedAnswer=" + answer;
        url = ensureValidURL(url);
        Log.e("JSON request", url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String str = response.toString();
                        Log.e("QR JSON response", str);
                        boolean result = false;
                        String correctness = ""; // whether the answer was correct or incorrect
                        boolean alreadyCompleted = false; // whether the user has already submitted a correct answer
                        try {
                            result = response.getString("result").equals("success");
                            correctness = response.getString(("correct?"));
                            alreadyCompleted = response.getString(("alreadyCompleted?")).equals("true");
                        } catch (JSONException exception) {
                            AnswerDisplay.setText("JSON String returned by server has no field 'result'.");
                        }
                        if (result) {
                            AnswerDisplay.setText(correctness);
                            if (correctness.equals("Correct!")) {
                                if (alreadyCompleted) {
                                    AnswerDisplay.setText(correctness + "\nYou already completed this puzzle.");
                                } else {
                                    int pointsToAdd = 20;
                                    UserDataUtils.setPuzzleCompleted(PuzzleID);
                                    UserDataUtils.incrementUserQRCodeScore(pointsToAdd);
                                    AnswerDisplay.setText(correctness + "\nYou got " + Integer.toString(pointsToAdd) + " points!" + "\nYou have unlocked the bonus QR code.");
                                }
                            }
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        AnswerDisplay.setText("Error in HTTP Request");
                    }
                });
        // Make the timeout longer so that the JSON request process doesn't get killed and has to restart
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(jsonObjectRequest);
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
                s = s + '+';
            else
                s = s + c;
        }
        return s;
    }
}
