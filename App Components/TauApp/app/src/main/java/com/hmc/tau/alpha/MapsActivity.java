package com.hmc.tau.alpha;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;


/**
 *     Maps Activity:
 *     Displays location given by the user's current position and the location of the puzzles and bonus
 *     QR codes in the hunt.
 *     Josh Morgan, David Linn - jmorgan@hmc.edu, dlinn@hmc.edu - 12/7/18
 *     Richie Harris, Tim Player - rkharris@hmc.edu, tplayer@hmc.edu - 4/6/19
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SupportMapFragment mapFrag;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private Marker mCurrLocationMarker;
    private FusedLocationProviderClient mFusedLocationClient;
    private int locationRequests = 0;
    public static LatLng myPos;
    public static int numPuzzleStops;
    public static String[] lats;
    public static String[] longs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWaypoints();

        setContentView(R.layout.activity_maps);

        getSupportActionBar().setTitle("Maps Location");

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     *
     * @param googleMap The map object to be manipulated
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(100); // tenth-second update interval.
        mLocationRequest.setFastestInterval(10); // .01s upper limit
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        }
        else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mMap.setMyLocationEnabled(true);
        }
    }

    //@Override
    //public void onPause() {
    //    super.onPause();

    //    //stop location updates when Activity is no longer active
    //    if (mFusedLocationClient != null) {
    //        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    //    }
    //}

    /**
     * Obtains the user's current location, places a marker on the map, and moves the
     * camera to view the users' current location.
     */
    LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            if (locationList.size() > 0) {
                locationRequests++;
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.d("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;
                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }

                //Place current location marker
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                myPos = latLng;
                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title("Current Position");
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                mCurrLocationMarker = mMap.addMarker(markerOptions);

                if (locationRequests == 1) {
                    //move map camera
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myPos, 17));
                }
            }
        }
    };

    /**
     * Checks if permission has already been granted.  If not, request location permission
     */
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();
            }
            else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    /**
     * Called when the user has responded to the prompt for location permission.
     *
     * @param requestCode The request code passed in requestPermissions
     * @param permissions The requested permissions
     * @param grantResults Results for corresponding permissions.  Either PERMISSION_GRANTED
     *                     or PERMISSION_DENIED
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    /**
     * Sends a JSON request to the Ground Truth google sheet.  The sheet returns the latitudes and
     * longitudes of the puzzles and bonus QR codes, and how many of these are puzzle stops as
     * opposed to bonus QR codes.  Then places labeled markers on the map corresponding to the
     * latitudes and longitudes recieved in the JSON request.
     */
    public void getWaypoints() {
        //Create queue that accepts requests
        RequestQueue queue = Volley.newRequestQueue(this);
        //Build URL and query string from JSON object
        String url = getApplicationContext().getString(R.string.ground_truth_script_url);
        url += '?';
        url += "Sheet=" + "MSTR" + '&'; // note: need to change sheet name each deployment
        url += "RequestType=" + "GetWaypoints";
        url = ensureValidURL(url);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String str = response.toString();
                        Log.d("QR JSON response",str);
                        boolean result = false;
                        try {
                            // Get the returned data from the JSON response
                            result = response.getString("result").equals("success");
                            numPuzzleStops = Integer.parseInt(response.getString("numPuzzleStops"));
                            Log.d("numPuzzleStops", Integer.toString(numPuzzleStops));

                            // convert from JSONArray to String[]
                            JSONArray arrJsonLat = response.getJSONArray("latitudes");
                            lats = new String[arrJsonLat.length()];
                            for(int i=0;i<arrJsonLat.length();i++)
                            {
                                lats[i] = arrJsonLat.getString(i);
                                Log.d("lat", Integer.toString(i)+" "+lats[i]);
                            }

                            JSONArray arrJsonLong = response.getJSONArray("longitudes");
                            longs = new String[arrJsonLong.length()];
                            for(int i=0;i<arrJsonLong.length();i++)
                            {
                                longs[i] = arrJsonLong.getString(i);
                                Log.d("long", Integer.toString(i)+" "+longs[i]);
                            }

                            //Place waypoints
                            LatLng waypoint;
                            MarkerOptions markopt;
                            double latitude;
                            double longitude;
                            for (int i=0;i<lats.length;i++) {
                                latitude = Double.parseDouble(lats[i]);
                                longitude = Double.parseDouble(longs[i]);
                                waypoint = new LatLng(latitude, longitude);
                                markopt = new MarkerOptions();
                                markopt.position(waypoint);
                                // The first "numPuzzleStops" latitude/longitude pairs are puzzle stops
                                if (i<numPuzzleStops) {
                                    markopt.title("Puzzle#"+Integer.toString(i+1));
                                    markopt.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW));
                                    mMap.addMarker(markopt);
                                }
                                // the rest are Bonus QR code stops
                                else {
                                    markopt.title("Bonus#"+Integer.toString(i-numPuzzleStops+1));
                                    markopt.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                                    mMap.addMarker(markopt);
                                }
                            }
                        }
                        catch (JSONException exception) {
                            Log.e("error: ","Json request failed");
                        }
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("error: ","Error in HTTP request");
                    }
                });
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
