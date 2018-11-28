package com.example.david.alpha;

import android.Manifest;
import android.app.AlertDialog;
import android.app.IntentService;
import android.content.Context;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.JobIntentService;
import android.support.v7.app.AppCompatActivity;
import android.net.Uri;
import android.widget.Toast;
import android.util.Log;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.HttpURLConnection;


// This is the remote update class. Some methods are broken; the source code can be found at this
// address: https://juristr.com/blog/2011/02/coding-update-functionality-for-your/
public class RemoteUpdateService extends IntentService {

    String remoteUpdateHostURL = GlobalParams.REMOTE_UPDATE_HOSTURL;

    public RemoteUpdateService() {
        super("Check_Remote_Update");
    }


    @Override
    protected void onHandleIntent(Intent workIntent){
        Log.d("Remote Updater", "Work intent initiated");
        Context context = getApplicationContext();
        int currentCodeVersion = getVersionCode(context);
        int nextCodeVersion = Integer.parseInt(downloadText());

        Uri intentURI = workIntent.getData();
        Uri staticURI = Uri.parse(remoteUpdateHostURL);

        //Log.d("Remote Updater", "Checking Host URL");

        //if (intentURI == staticURI) {
            if (nextCodeVersion > currentCodeVersion) {
                Intent updateIntent = new Intent(Intent.ACTION_VIEW, staticURI);
                Log.d("Remote Updater","Starting Update");
                startActivity(updateIntent);
                Log.d("Remote Updater", "Update started");
                return;
            } else {
                Toast.makeText(context, "App up to date", Toast.LENGTH_SHORT).show();
                Log.d("Remote Updater","App up to date; onboard - " + Integer.toString(currentCodeVersion) + "; online - " + Integer.toString(nextCodeVersion));
                return;
            }
        //} else {
        //    return;
        //}
    }


    public static int getVersionCode(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            Log.d("Remote Updater","Code Version Retrieved - " + Integer.toString(pi.versionCode));
            return pi.versionCode;
        } catch (NameNotFoundException ex) {
            Log.d("Remote Updater", "Could not retrieve version");
        }
        return 0;
    }


    public static String getVersionName(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            return pi.versionName;
        } catch (NameNotFoundException ex) {}
        return "0";
    }


    public static String getVersionFile() {
        String location = GlobalParams.REMOTE_UPDATE_INFOURL;
        return location;
    }


    private String downloadText() {
        int BUFFER_SIZE = 2000;
        InputStream in = null;
        try {
            in = openHttpConnection();
            Log.d("downloadText","HTTP Connection finished");
        } catch (IOException e1) {
            Log.d("downloadText", "HTTP Connection failed");
            return "0";
        }

        String str = "";
        if (in != null) {
            InputStreamReader isr = new InputStreamReader(in);
            Log.d("downloadText","InputStreamReader instantiated");
            int charRead;
            char[] inputBuffer = new char[BUFFER_SIZE];
            try {
                while ((charRead = isr.read(inputBuffer)) > 0) {
                    // ---convert the chars to a String---
                    String readString = String.copyValueOf(inputBuffer, 0, charRead);
                    str += readString;
                    inputBuffer = new char[BUFFER_SIZE];
                }
                in.close();
                Log.d("downloadText","input buffer string reader completed");
            } catch (IOException e) {
                Log.d("downloadText", "IOException");
                return "0";
            }
        }
        Log.d("downloadText","String returned");
        Log.d("downloadText",str);
        return str;
    }


    private InputStream openHttpConnection() throws IOException {
        InputStream in = null;
        int response = -1;

        URL url = new URL(getVersionFile());
        Log.d("openHttpConn", "Info URL retrieved - " + getVersionFile());
        URLConnection conn = url.openConnection();

        if (!(conn instanceof HttpURLConnection))
            throw new IOException("Not an HTTP connection");

        try {
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            Log.d("openHttpConn","URL connection generated");
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            Log.d("openHttpConn", "GET request initiated");
            httpConn.connect();
            Log.d("openHttpConn","URL connection successfully opened");

            response = httpConn.getResponseCode();
            Log.d("Remote Updater", "Response code - " + Integer.toString(response));
            if (response == HttpURLConnection.HTTP_OK) {
                Log.d("openHttpConn","Http connection okay");
                in = httpConn.getInputStream();
                Log.d("openHttpConn","Http input stream okay");
            }
        } catch (Exception ex) {
            Log.d("openHttpConn", "Could not open HTTPS");
            throw new IOException("Error connecting");
        }
        Log.d("openHttpConn","input stream returned");
        return in;
    }


}
