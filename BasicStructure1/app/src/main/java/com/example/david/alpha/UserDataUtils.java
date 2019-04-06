// Tim Player and Richard Harris
// 30 March 2019
// tplayer@hmc.edu rharris@hmc.edu
// UserDataUtils.java
// SETTERS AND GETTERS FOR USERACTIVEHOURSSCORE, USERQRCODESCORE, AND USERTOTALSCORE

package com.example.david.alpha;
import android.content.SharedPreferences;
import android.util.Log;

import static android.content.Context.MODE_PRIVATE;

public class UserDataUtils {

    //State
    private static String sharedPrefFile = "com.example.david.alpha";
    private static SharedPreferences userData = App
            .getContext()
            .getSharedPreferences(sharedPrefFile, MODE_PRIVATE);

    //Behavior

    public static void setSensorID(String id){
        String key = GlobalParams.SENSOR_KEY;
        putString(key, id);
    }

    public static String getSensorID(){
        String key = GlobalParams.SENSOR_KEY;
        return getString(key);
    }

    //increases ActiveHoursScore and UserTotalScore by 1 in userData
    public static void incrementUserActiveHoursScore(){
        incrementUserActiveHoursScore(1);
    }

    //increases ActiveHoursScore and UserTotalScore by amount in userData
    public static void incrementUserActiveHoursScore(int amount){

        int currentActiveHoursScore = getUserActiveHoursScore();

        String key = GlobalParams.ACTIVEHOURS_SCORE_KEY;
        putInt(key, currentActiveHoursScore + amount);

        incrementUserTotalScore(amount);

        Log.d("scoring", "Active Hours point(s) incremented");
    }

    //increases userQRCodeScore and UserTotalScore by 1 in userData
    public static void incrementUserQRCodeScore(){
        incrementUserQRCodeScore(1);
    }

    //increases ActiveHoursScore and UserTotalScore by amount in userData
    public static void incrementUserQRCodeScore(int amount){

        int currentUserQRCodeScore = getUserQRCodeScore();

        String key = GlobalParams.QRCODE_SCORE_KEY;
        putInt(key, currentUserQRCodeScore + amount);

        incrementUserTotalScore(amount);

        Log.d("scoring", "QR Code point(s) incremented");
    }

    public static void incrementUserTotalScore(){
        incrementUserTotalScore(1);
    }

    //increase the userTotalScore in userData by amount. Note: this should only be called by
    //incrementActiveHoursScore in ActiveHoursActivity to avoid redundant point assignment.
    public static void incrementUserTotalScore(int amount){

        int currentUserTotalScore = getUserTotalScore();

        String key = GlobalParams.TOTAL_SCORE_KEY;
        putInt(key, currentUserTotalScore + amount);

        Log.d("scoring", "Active Hours point(s) incremented");
    }

    public static int getUserActiveHoursScore(){
        String key = GlobalParams.ACTIVEHOURS_SCORE_KEY;
        return getInt(key);
    }

    public static int getUserTotalScore(){
        String key = GlobalParams.TOTAL_SCORE_KEY;
        return getInt(key);
    }

    public static int getUserQRCodeScore(){
        String key = GlobalParams.QRCODE_SCORE_KEY;
        return getInt(key);
    }

    public static Boolean wasPuzzleCompleted(String puzzleID){
        return userData.getBoolean(puzzleID, false);
    }

    public static void setPuzzleCompleted(String puzzleID){
        putBoolean(puzzleID, true);
    }

    public static void putBoolean(String key, Boolean value){
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
        return userData.getInt(key,  0);
    }

    //gets a String from userData
    private static String getString(String key){
        return userData.getString(key, "");
    }

    //puts a String in userData
    private static void putString(String key, String value){
        SharedPreferences.Editor editor = userData.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String getLastScan() {
        return userData.getString("lastScan","none");
    }

    public static void setLastScan(String lastScan) {
        putString("lastScan", lastScan);
    }

}
