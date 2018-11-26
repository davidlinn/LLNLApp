package com.example.david.alpha;

public enum GlobalParams {;

    public static final long ACTIVE_CUTOFF = 10000; // The time after which the player state goes from active to inactive. Time in milliseconds.
    public static final long POINT_TIME = 10000; // The time in millis after which an active player gains a point.

    public static final int ACC_SAMPLE_SIZE = 50;

    public static final double ACC_THRESHOLD = 1.25;
    public static final double ACC_CUTOFF = 20;

    public static final String SCORE_KEY = "User_Score";
    public static final String SENSOR_KEY = "Sensor_ID";
}
