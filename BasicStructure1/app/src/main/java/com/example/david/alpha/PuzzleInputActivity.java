package com.example.david.alpha;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
        AnswerDisplay.setText("success");
    }
}
