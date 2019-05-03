package com.example.david.alpha;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

/**
 * A subclass of Application whose sole purpose is to provide global context for use
 * in other classes. Currently, this is only used by UserDataUtils.
 */
public class App extends Application {

    /**
     * An Application object to be given to external callers to provide global Context
     */
    private static Application sApplication;

    /**
     * Getter method for this Application
     * @return
     */
    public static Application getApplication() {
        return sApplication;
    }

    /**
     * Getter method for this Application's Context
     * @return
     */
    public static Context getContext() {
        return getApplication().getApplicationContext();
    }

    /**
     * Instantiates the Application in this class's state
     */
    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
    }
}