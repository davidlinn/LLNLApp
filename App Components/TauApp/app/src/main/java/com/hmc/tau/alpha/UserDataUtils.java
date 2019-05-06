// Tim Player and Richard Harris
// 30 March 2019
// tplayer@hmc.edu rharris@hmc.edu
// UserDataUtils.java
// SETTERS AND GETTERS FOR USERACTIVEHOURSSCORE, USERQRCODESCORE, AND USERTOTALSCORE

package com.hmc.tau.alpha;
import android.content.SharedPreferences;
import android.util.Log;

import static android.content.Context.MODE_PRIVATE;

/**
 * Helper class to handle SharedPreferences for UX continuity
 */
public class UserDataUtils {

    //State

    /**
     * The package path of the SharedPreferences object
     */
    private static String sharedPrefFile = "com.example.david.alpha";

    /**
     * The SharedPreferences object in which all of the user's information is stored
     */
    private static SharedPreferences userData = App
            .getContext()
            .getSharedPreferences(sharedPrefFile, MODE_PRIVATE);

    //Behavior

    /**
     * Put the String SensorID into userData
     * @param id
     */
    public static void setSensorID(String id){
        String key = GlobalParams.SENSOR_KEY;
        putString(key, id);
    }

    /**
     * Getter for sensorID
     * @return
     */
    public static String getSensorID(){
        String key = GlobalParams.SENSOR_KEY;
        return getString(key);
    }

    /**
     * Put the int CounterSteps into SharedPreferences.
     * This method ensures that the last known value of the Step Counter is persistent even when
     * ActiveHoursActivity is restarted.
     * @param cs
     */
    public static void setCounterSteps(int cs){
        String key = GlobalParams.COUNTER_STEPS_KEY;
        putInt(key, cs);
    }

    /**
     * Getter for CounterSteps
     * @return
     */
    public static int getCounterSteps(){
        String key = GlobalParams.COUNTER_STEPS_KEY;
        return getInt(key);
    }

    /**
     * increases ActiveHoursScore and UserTotalScore by 1 in userData
     */
    public static void incrementUserActiveHoursScore(){
        incrementUserActiveHoursScore(1);
    }

    /**
     * increases ActiveHoursScore and UserTotalScore by amount in userData
     * @param amount
     */
    public static void incrementUserActiveHoursScore(int amount){

        int currentActiveHoursScore = getUserActiveHoursScore();

        String key = GlobalParams.ACTIVEHOURS_SCORE_KEY;
        putInt(key, currentActiveHoursScore + amount);

        incrementUserTotalScore(amount);

        Log.d("scoring", "Active Hours point(s) incremented by " + Integer.toString(amount));
    }

    /**
     * Increases userQRCodeScore and UserTotalScore by 1 in userData
     */
    public static void incrementUserQRCodeScore(){
        incrementUserQRCodeScore(1);
    }

    /**
     * Increases ActiveHoursScore and UserTotalScore in userData by amount
     * @param amount
     */
    public static void incrementUserQRCodeScore(int amount){

        int currentUserQRCodeScore = getUserQRCodeScore();

        String key = GlobalParams.QRCODE_SCORE_KEY;
        putInt(key, currentUserQRCodeScore + amount);

        incrementUserTotalScore(amount);

        Log.d("scoring", "QR Code point(s) incremented");
    }

    /**
     * Convenience function to increment UserTotalScore by 1
     */
    public static void incrementUserTotalScore(){
        incrementUserTotalScore(1);
    }

    /**
     * increase the userTotalScore in userData by amount. Note: this should only be called by
     * incrementActiveHoursScore in ActiveHoursActivity to avoid redundant point assignment.
     */
    public static void incrementUserTotalScore(int amount){

        int currentUserTotalScore = getUserTotalScore();

        String key = GlobalParams.TOTAL_SCORE_KEY;
        putInt(key, currentUserTotalScore + amount);

        Log.d("scoring", "Active Hours point(s) incremented");
    }

    /**
     * Getter for Active Hours Score
     */
    public static int getUserActiveHoursScore(){
        String key = GlobalParams.ACTIVEHOURS_SCORE_KEY;
        return getInt(key);
    }

    /**
     * Getter for Total Score
     * @return
     */
    public static int getUserTotalScore(){
        String key = GlobalParams.TOTAL_SCORE_KEY;
        return getInt(key);
    }

    /**
     * Getter for QR Code Score
     * @return
     */
    public static int getUserQRCodeScore(){
        String key = GlobalParams.QRCODE_SCORE_KEY;
        return getInt(key);
    }

    /**
     * Getter for flag saying whether puzzle was previously completed
     * @param puzzleID
     * @return
     */
    public static Boolean wasPuzzleCompleted(String puzzleID){
        return userData.getBoolean(puzzleID + "_puzzle_completed", false);
    }

    /**
     * Setter for flage saying whether puzzle was previously completed
     * @param puzzleID
     */
    public static void setPuzzleCompleted(String puzzleID){
        putBoolean(puzzleID + "_puzzle_completed", true);
    }

    /**
     * Getter for flag saying whether a puzzle's bonus was collected
     * @param puzzleID
     * @return
     */
    public static Boolean wasBonusCollected(String puzzleID){
        return userData.getBoolean(puzzleID + "_bonus_collected", false);
    }

    /**
     * Setter for flag saying whether a puzzle's bonus was collected
     * @param puzzleID
     */
    public static void setBonusCollected(String puzzleID) {
        putBoolean(puzzleID + "_bonus_collected", true);
    }

    /**
     * Gets the last recorded puzzle scan number (to avoid double-scanning)
     * @return
     */
    public static String getLastScan() {
        return userData.getString("lastScan","none");
    }

    /**
     * Sets the alst recorded puzzle scan number
     * @param lastScan
     */
    public static void setLastScan(String lastScan) {
        putString("lastScan", lastScan);
    }

    /**
     * Private helper method to put a Boolean into userData
     * @param key
     * @param value
     */
    private static void putBoolean(String key, Boolean value){
        SharedPreferences.Editor editor = userData.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    /**
     * Private helper method to put an Int into userData
     * https://stackoverflow.com/questions/2614719/how-do-i-get-the-sharedpreferences-from-a-preferenceactivity-in-android
     * @param key
     * @param value
     */
    private static void putInt(String key, int value) {
        SharedPreferences.Editor editor = userData.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    /**
     *  Gets an int from userData
     */
    private static int getInt(String key) {
        return userData.getInt(key,  0);
    }

    /**
     * Gets a String from userData
     * @param key
     * @return
     */
    private static String getString(String key){
        return userData.getString(key, "");
    }

    /**
     * Puts a String in userData
     * @param key
     * @param value
     */
    private static void putString(String key, String value){
        SharedPreferences.Editor editor = userData.edit();
        editor.putString(key, value);
        editor.apply();
    }

}
