package com.example.david.alpha;


public enum GlobalParams {;

    public static final long ACTIVE_CUTOFF = 10000; // The time after which the player state goes from active to inactive (10min). Time in milliseconds.
    public static final long POINT_TIME = 10000; // The time in millis after which an active player gains a point (1min).
    public static final long MILLIS_TO_MINUTES = 1000; // Converts ACTIVE_CUTOFF from milliseconds to minutes.

    public static final int ACC_SAMPLE_SIZE = 50;

    public static final double ACC_THRESHOLD = 1.25;
    public static final double ACC_CUTOFF = 20;

    public static final String ACTIVEHOURS_SCORE_KEY = "User_ActiveHoursScore";
    public static final String QRCODE_SCORE_KEY = "User_QRCodeScore";
    public static final String TOTAL_SCORE_KEY = "User_TotalScore";
    public static final String SENSOR_KEY = "Sensor_ID";

    public static final String REMOTE_UPDATE_INFOURL = "https://drive.google.com/drive/folders/1CQHFqTuFobi68fkUPtf4QUKtVXg2b92H?usp=sharing/versionInfo.txt"; //TODO: SET UP .TXT FILE INCLUDING VERSION NUM FOR COMPARISON.
    public static final String REMOTE_UPDATE_HOSTURL = "https://drive.google.com/open?id=1qC36LX20MLgcu4Dd801YsdApRVtEp16H/app-deploy.apk"; //TODO: SET UP APPROPRIATE HOSTING SITE.
}
