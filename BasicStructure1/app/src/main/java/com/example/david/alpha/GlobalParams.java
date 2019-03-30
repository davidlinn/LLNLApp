package com.example.david.alpha;


public enum GlobalParams {;


    public static final long ACTIVE_CUTOFF = 5000; // The time after which the player state goes from active to inactive (50sec). Time in milliseconds.
    public static final long POINT_TIME = 15000; // The time in millis after which an active player gains a point (15sec).
    public static final long MILLIS_TO_MINUTES = 1000; // Converts ACTIVE_CUTOFF from milliseconds to minutes.

    public static final int STEPS_PER_POINT = 15;
    public static final int ACC_SAMPLE_SIZE = 50;

    public static final double ACC_THRESHOLD = .9;
    public static final double ACC_CUTOFF = 20;

    public static final String ACTIVEHOURS_SCORE_KEY = "User_ActiveHoursScore";
    public static final String QRCODE_SCORE_KEY = "User_QRCodeScore";
    public static final String TOTAL_SCORE_KEY = "User_TotalScore";
    public static final String SENSOR_KEY = "Sensor_ID";

    public static final String REMOTE_UPDATE_INFOURL = "https://raw.githubusercontent.com/SnekCharmer/App-deploy/master/versionInfo.txt"; //TODO: FIGURE OUT DOWNLOAD NOTIFICATIONS.
    public static final String REMOTE_UPDATE_HOSTURL = "https://github.com/SnekCharmer/App-deploy/blob/master/app-deploy.apk?raw=true";
}
