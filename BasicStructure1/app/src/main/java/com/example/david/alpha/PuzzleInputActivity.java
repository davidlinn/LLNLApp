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


import static com.example.david.alpha.ActiveHoursActivity.userData;

/*
    Puzzle input activity:  Allows user to input puzzle answers.  Initiates request to google sheets
    database containing puzzle answers.  Compares input to correct answer and resturns if the user
    got the puzzle right.
 */

public class PuzzleInputActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{
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
    private final int MY_SOCKET_TIMEOUT_MS = 10000;

    //private static String Puzzle1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userData = getSharedPreferences(sharedPrefFile, MODE_PRIVATE);

        setContentView(R.layout.activity_puzzle_input);

        getPuzzleOptions();
    }

    @Override
    public void onResume(){
        super.onResume();

        //read in Active Hours points from userData

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

    public void submitInput(View view) {

        final TextView AnswerDisplay = (TextView) findViewById(R.id.input_serverinfo);
        String confirmation = "Answer submitted.  Checking answer...";
        AnswerDisplay.setText(confirmation);

        EditText editText = (EditText) findViewById(R.id.editText);
        String answer = editText.getText().toString().toLowerCase();

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
        url = ensureValidURL(url);
        Log.e("JSON request", url);

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
                            alreadyCompleted = response.getString(("alreadyCompleted?")).equals("true");
                        }
                        catch (JSONException exception) {
                            AnswerDisplay.setText(correctness);
                        }
                        if (result) {
                            AnswerDisplay.setText(correctness);
                            if (correctness.equals("Correct!")) {
                                if(alreadyCompleted){
                                    AnswerDisplay.setText(correctness + "\nYou already completed this puzzle.");
                                }else {
                                    int pointsToAdd = 20;
                                    setPuzzleCompleted(PuzzleID);
                                    incrementUserQRCodeScore(pointsToAdd);
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

                jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                        MY_SOCKET_TIMEOUT_MS,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(jsonObjectRequest);

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

    //SETTERS AND GETTERS FOR USERACTIVEHOURSSCORE, USERQRCODESCORE, AND USERTOTALSCORE

    //increases ActiveHoursScore and UserTotalScore by 1 in userData
    private void incrementUserActiveHoursScore(){
        incrementUserActiveHoursScore(1);
    }

    //increases ActiveHoursScore and UserTotalScore by amount in userData
    private void incrementUserActiveHoursScore(int amount){

        int currentActiveHoursScore = getUserActiveHoursScore();

        String key = GlobalParams.ACTIVEHOURS_SCORE_KEY;
        putInt(key, currentActiveHoursScore + amount);

        incrementUserTotalScore(amount);

        Log.d("scoring", "Active Hours point(s) incremented");
    }

    //increases userQRCodeScore and UserTotalScore by 1 in userData
    private void incrementUserQRCodeScore(){
        incrementUserQRCodeScore(1);
    }

    //increases ActiveHoursScore and UserTotalScore by amount in userData
    private void incrementUserQRCodeScore(int amount){

        int currentUserQRCodeScore = getUserQRCodeScore();

        String key = GlobalParams.QRCODE_SCORE_KEY;
        putInt(key, currentUserQRCodeScore + amount);

        incrementUserTotalScore(amount);

        Log.d("scoring", "QR Code point(s) incremented");
    }

    private void incrementUserTotalScore(){
        incrementUserTotalScore(1);
    }

    //increase the userTotalScore in userData by amount. Note: this should only be called by
    //incrementActiveHoursScore in ActiveHoursActivity to avoid redundant point assignment.
    private void incrementUserTotalScore(int amount){

        int currentUserTotalScore = getUserTotalScore();

        String key = GlobalParams.TOTAL_SCORE_KEY;
        putInt(key, currentUserTotalScore + amount);

        Log.d("scoring", "Active Hours point(s) incremented");
    }

    private int getUserActiveHoursScore(){
        String key = GlobalParams.ACTIVEHOURS_SCORE_KEY;
        return getInt(key);
    }

    private int getUserTotalScore(){
        String key = GlobalParams.TOTAL_SCORE_KEY;
        return getInt(key);
    }

    private int getUserQRCodeScore(){
        String key = GlobalParams.QRCODE_SCORE_KEY;
        return getInt(key);
    }

    private Boolean wasPuzzleCompleted(String puzzleID){
        return userData.getBoolean(puzzleID, false);
    }

    private void setPuzzleCompleted(String puzzleID){
        putBoolean(puzzleID, true);
    }

    private void putBoolean(String key, Boolean value){
        SharedPreferences.Editor editor = userData.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    //puts an int in userData
    //https://stackoverflow.com/questions/2614719/how-do-i-get-the-sharedpreferences-from-a-preferenceactivity-in-android
    private static void putInt(String key, int value) {
        SharedPreferences.Editor editor = userData.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    //gets an int from userData
    private static int getInt(String key) {
        return userData.getInt(key,  -1);
    }

}
