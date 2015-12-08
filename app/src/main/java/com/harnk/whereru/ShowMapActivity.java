package com.harnk.whereru;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import com.loopj.android.http.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import cz.msebera.android.httpclient.Header;

public class ShowMapActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

//    public static final String API_URL = "http://www.altcoinfolio.com//whereruprod/api/api.php";

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private List<Marker> markers = new ArrayList<Marker>();
    private ListView messageList;
    private ArrayAdapter<String> arrayAdapter;
    private DeviceUuidFactory deviceUuidFactory;
    private ArrayList<Room> roomArray = new ArrayList<Room>();

    //Control booleans
    private boolean isUpdating;
    private boolean isFromNotification;
    private boolean pickerIsUp;
    private boolean okToRecenterMap;
    private String centerOnThisGuy;


    //GCM stuff
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "SCXTT";
    private String response;

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private ProgressBar mRegistrationProgressBar;

    //Location service
    public GoogleApiClient mGoogleApiClient;

    //Interval timers
    private int mInterval = 5000; // 5 seconds by default, can be changed later
    private Handler mHandler;


    private void scrollMyListViewToBottom() {
        messageList.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                messageList.setSelection(arrayAdapter.getCount() - 1);
            }
        });
    }

    private void loadSingletonWithSharedPrefs() {
        Log.v(TAG, "loadSingletonWithSharedPrefs");
        //Get SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String savedNickname = prefs.getString("nickname", "");
        String savedSecretCode = prefs.getString("secretCode", "");
        String savedLocStr = prefs.getString("savedLocStr", "");
        Log.d(TAG, "Getting SharedPreferences ... Stored nickname is: " + savedNickname + " secretCode: " + savedSecretCode + " savedLocStr: " + savedLocStr);

        deviceUuidFactory = new DeviceUuidFactory(this);
        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        deviceSingleton.init(this.getApplicationContext());
        String userId = deviceUuidFactory.getDeviceUuidString();
        deviceSingleton.setUserId(userId);
        deviceSingleton.setNickname(savedNickname);
        deviceSingleton.setSecretCode(savedSecretCode);
        deviceSingleton.setMyLocStr(savedLocStr);
        Log.d(TAG, "Get UUID-> userId: " + (String) userId);
        Log.d(TAG, "Singleton deviceId : " + (String) deviceSingleton.getDeviceId() + " NOT BEING USED, use userId instead");


    }

    private void userDidLeave() {
        //userDidLeave saves joinedchat false to SharedPrefs
        // and singleton setImInARoom to false
        // then show login
        Intent intent2 = new Intent(ShowMapActivity.this, LoginActivity.class);
        startActivity(intent2);
//        break;

    }

    private void postLeaveRequest(){
        Log.d(TAG, "postLeaveRequest");
        //postLeaveRequest should do API call cmd leave
        // if success - call userDidLeave

        //part1 do API cmd = leave
        /////////////////////////////////////////////////////////////////////////////////////////
        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        AsyncHttpClient client2 = new AsyncHttpClient();
        RequestParams params2 = new RequestParams();

        params2.put("cmd", "leave");
        params2.put("user_id", deviceSingleton.getUserId());

        Log.d(TAG, "params2: " + params2);

        client2.post(Constants.API_URL, params2, new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {
                // called before request is started
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                // called when response HTTP status is "200 OK"
                String decoded = null;
                try {
                    decoded = new String(response, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "API call onSuccess = " + statusCode + ", Headers: " + headers[0] + ", response.length: " + response.length +
                        ", decoded:" + response);
                userDidLeave();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.d(TAG, "API call onFailure = " + errorResponse.toString() + " e: " + e.toString() + " statusCode: " + statusCode);
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });
        Log.d(TAG, "API call response out of catch");

        /////////////////////////////////////////////////////////////////////////////////////////


    }

    private void myShowDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG, "pressed OK");

                        //alertview should call postLeaveRequest
                        postLeaveRequest();

                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Log.d(TAG, "pressed Cancel");
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void showAlert(String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do something dummy
            }
        });
        alertDialog.show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("SCXTT", " onCreate");

        okToRecenterMap = true;
        pickerIsUp = false;
        isFromNotification = false;
        centerOnThisGuy = "";

        setContentView(R.layout.activity_show_map);
        MapsInitializer.initialize(this);
        setUpMapIfNeeded();

        //Timer setup
        mHandler = new Handler();
//        startRepeatingTask(); <--moved to onStart

        messageList = (ListView) findViewById(R.id.listView);

        //GCM stuff
        mRegistrationProgressBar = (ProgressBar) findViewById(R.id.registrationProgressBar);
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                Log.v(TAG,"mRegistrationBroadcastReceiver onReceive");
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    Log.d(TAG, getString(R.string.gcm_send_message));
//                    mInformationTextView.setText(getString(R.string.gcm_send_message));
                } else {
                    Log.d(TAG, getString(R.string.token_error_message));
//                    mInformationTextView.setText(getString(R.string.token_error_message));
                }
            }
        };
        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }


        //Get Stored SharedPrefs and put them into the singleton
        loadSingletonWithSharedPrefs();



        //Google API build
        buildGoogleApiClient();

        //Use the LocationManager class to obtain updated GPS locations
        LocationManager mlocManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        LocationListener mlocListener = new MyLocationListener();
        //                                                               0, 0, is minTime ms, minDistance meters
        mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mlocListener);



        ///////////////////////////////////////////////////////////////////////////////////////////////
        //Do a getroom API call
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("cmd", "getroom");
        params.put("user_id", "381CA86D2E3A4F18B2E6A63CF0C52EDF");
        params.put("location", "41.739567, -86.098872");
        params.put("text", "notused");

        client.post(Constants.API_URL, params, new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {
                // called before request is started
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                //setting the new location AND GETTING THE JSON RESPONSE!

                String decoded = null;  // example for one encoding type

                try {
                    decoded = new String(response, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                Log.v(TAG, "API call onSuccess = " + statusCode + ", Headers: " + headers[0] + ", response.length: " +response.length +
                        ", decoded:" + decoded);


                JSONObject jObj = null;
                try {
                    jObj = new JSONObject(decoded);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
                    String[] myPinImages = new String[]{"blue","cyan","darkgreen","gold","green","orange","pink","purple","red","yellow","cyangray"};
                    JSONArray list = new JSONArray(decoded);
                    Log.v(TAG, "API Call getroom returned list.length: " + list.length());
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    for (int i=0; i < list.length(); i++) {
                        JSONObject obj = list.getJSONObject(i);

                        String nickName = obj.getString("nickname");
                        char ch = nickName.charAt(0);
                        int asciiCode = (int) ch;
                        int digit = asciiCode % 10;
                        int resID = getResources().getIdentifier(myPinImages[digit], "drawable", getPackageName());


//                        Log.v(TAG, "nickname: " + obj.getString("nickname") + " char: " + ch + " asciiCode: " + asciiCode + " digit: " + digit);
//                        Log.v(TAG, "location: " + obj.getString("location"));
//                        Log.v(TAG, "loc_time: " + obj.getString("loc_time"));

                        String[] latlong =  obj.getString("location").split(",");
                        double latitude = Double.parseDouble(latlong[0]);
                        double longitude = Double.parseDouble(latlong[1]);

                        //Get distance from me to LatLng for the title
                        Location oldLoc = deviceSingleton.getMyNewLocation();

                        Location pinLoc = new Location("dummyprovider");
                        pinLoc.setLatitude(latitude);
                        pinLoc.setLongitude(longitude);

                        float distanceBetween;
                        float distanceInYards;
                        float distanceInMiles;

                        if (oldLoc == null ) {
                            distanceBetween = 0;
                            distanceInYards = 0;
                            distanceInMiles = 0;

                        } else {
                            distanceBetween = oldLoc.distanceTo(pinLoc);
                            distanceInYards = (float) (distanceBetween * 1.09361);
                            distanceInMiles = distanceInYards / 1760;
                        }

                        String pinDisplayDistance;


                        if (distanceInYards > 500) {
//                            String  = [NSString stringWithFormat:@"%@ %@, %.1f miles", senderName, dateString, distanceInMiles];
                            String myMiles = String.format("%.1f", distanceInMiles);
                            pinDisplayDistance = myMiles + " miles";
                        } else {
//                            _label.text = [NSString stringWithFormat:@"%@ %@, %.1f y", senderName, dateString, distanceInYards];
                            String myYards = String.format("%.1f", distanceInYards);
                            pinDisplayDistance = myYards + " y";
                        }

                        String annotationTitle = obj.getString("nickname");
                        String annotationSnippet = obj.getString("loc_time") + ", "+ pinDisplayDistance;
                        Marker m = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(latitude, longitude))
                                .icon(BitmapDescriptorFactory.fromResource(resID))
                                .title(annotationTitle)
                                .snippet(annotationSnippet)
                                .anchor(0.4727f, 0.5f));
                        markers.add(m);

                        builder.include(m.getPosition());

                    }
                    //Back up camera zoom level to see all pins
                    //this should prob come out of here and go into the main UI thread and use saved objects
                    LatLngBounds bounds = builder.build();
                    int padding = 20; // offset from edges of the map in pixels
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                    mMap.moveCamera(cu);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // called when response HTTP status is "200 OK"
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.v(TAG, "API call onFailure = " + errorResponse);
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });
        Log.v(TAG, "API call response out of catch = " + response);

        //END getroom API call
        ///////////////////////////////////

    }
////END onCreate //////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onStart() {
        super.onStart();
        Log.d("SCXTT", " onStart");
        mGoogleApiClient.connect();
        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        //if NOT singleton.joinedChat, set singleton.imInARoom=false, show LoginActivity
        // Need to store joinedChat on device and check that instead SCXTT WIP

        if (!deviceSingleton.isJoinedChat()) {
            deviceSingleton.setImInARoom(false);
            Intent intent2 = new Intent(ShowMapActivity.this, LoginActivity.class);
            startActivity(intent2);
        } else {
            deviceSingleton.setImInARoom(true);
            // do postGetRoomMessages, postGetRoom
            postGetRoomMessages();
            postGetRoom();
            // refresh listview, send local notification to startRepeatingTask (now in onResume)
        }

    }

    protected void onStop() {
        super.onStop();
        Log.d("SCXTT", " onStop");

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        startRepeatingTask();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
            new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        stopRepeatingTask();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_compose:
                Log.d(TAG, "Compose selected");
                Intent intent = new Intent(ShowMapActivity.this, ComposeActivity.class);
                startActivity(intent);
                break;
            case R.id.action_login:
                Log.d(TAG, "Signout/Login selected");
                //throw alertview are they sure they want to signout

                myShowDialog("Are you sure you wish to sign out of this map group? Your friends here will miss you!");
//                myShowDialog("Sign Out of This Map Group", "Are you sure you wish to sign out of this map group? You friends here will miss you!");
//                showAlert("Sign Out of This Map Group", "Are you sure you wish to sign out of this map group? You friends here will miss you!");

            case R.id.action_pinpicker:
                Log.d(TAG, "Pinpicker selected");

                break;
            case R.id.action_reload:
                Log.d(TAG, "Reload selected");
//                Temporary below
                this.arrayAdapter.notifyDataSetChanged();
                this.scrollMyListViewToBottom();
                centerMapOnMyLoc();
                break;
            case R.id.action_sat:
                Log.d(TAG, "Sat/Map selected");
                if (mMap.getMapType()== 1 ) {
                    mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                } else {
                    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //region Map Stuff
    //Map Stuff
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        Log.i(TAG, "checkPlayServices " + resultCode);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }
    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        BitmapDescriptor bluePin;
        bluePin = BitmapDescriptorFactory.fromResource(R.drawable.blue);
        BitmapDescriptor goldPin;
        goldPin = BitmapDescriptorFactory.fromResource(R.drawable.gold);
        BitmapDescriptor pinkPin;
        pinkPin = BitmapDescriptorFactory.fromResource(R.drawable.pink);
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
//            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(41.739362, -86.099086), 16.0f));


//            centerMapOnMyLoc();

            // Check if we were successful in obtaining the map.
//            mMap.addMarker(new MarkerOptions()
//                    .position(new LatLng(41.738362, -86.097086))
//                    .icon(pinkPin)
//                    .title("Fake - Today, 8:43 PM, 6.3 y"));
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void centerMapOnMyLoc() {
        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        String myLoc = deviceSingleton.getMyLocStr();
        String[] latlong =  myLoc.split(",");
        double latitude = Double.parseDouble(latlong[0]);
        double longitude = Double.parseDouble(latlong[1]);
//        scxtt wip take next line out for now
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 12.0f)); // was 16.0f
    }

    @Override
    public void onMapReady(GoogleMap map) {
        map.addMarker(new MarkerOptions()
                .position(new LatLng(41.739362, -86.098086))
                .title("Hello world"));
        Log.d("SCXTT", "doing onMapReady callback is this working but LOCATION is still hardcoded and wrong");
//        centerMapOnMyLoc();
    }


    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(41.739362, -86.099086)).title("My Location - this is currently hardcoded and wrong"));
    }


    /////////////////////////////////////////////////////////////////////////////////////
    //Google geolocation API
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    public void onConnectionSuspended(int cause) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // TODO Auto-generated method stub
        Log.d("SCXTT", " onConnectionFailed " + result.toString());

    }


    @Override
    public void onConnected(Bundle connectionHint) {
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        Log.v("SCXTT", "Found the location");



        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();




        if (mLastLocation != null) {
            // Store it in the singleton
            DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
            deviceSingleton.setMyNewLocation(mLastLocation);

            String mLatitudeText = String.valueOf(mLastLocation.getLatitude());
            String mLongitudeText = String.valueOf(mLastLocation.getLongitude());
            Log.d("SCXTT", " mLastLocation: " + mLatitudeText + ", " + mLongitudeText);
            editor.putString("savedLocStr", mLatitudeText + ", " + mLongitudeText);
            editor.commit();

            //ALSO Store this in the singleton
            deviceSingleton.setMyLocStr(mLatitudeText + ", " + mLongitudeText);

//            centerMapOnMyLoc();




            //play beep
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
                r.play();
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            //Store this in the singleton
            DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
//            deviceSingleton.init(this.getApplicationContext());
            //If no last loc then seed it to be the Statue of Liberty
            Location myLoc = new Location("dummyprovider");
            String mySeedLocStr = "40.689124, -74.044611";
            myLoc.setLatitude(40.689124);
            myLoc.setLongitude(-74.044611);
            deviceSingleton.setMyNewLocation(myLoc);
            editor.putString("savedLocStr", mySeedLocStr);
            editor.commit();
            Log.d("SCXTT", " deviceSingleton.setMyNewLocation(): " + myLoc.toString());
        }
    }

    public void onDisconnected() {
        // TODO Auto-generated method stub

    }

    public void postGetRoom() {
        Log.v(TAG, "postGetRoom should happene every 5 secs ->WIP");
        postGetRoomWIP();
    }

    public void postGetRoomWIP() {
        //Do a getroom API call
        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("cmd", "getroom");
        params.put("user_id", deviceSingleton.getUserId());
        params.put("location", deviceSingleton.getMyLocStr());
        params.put("text", "notused");

        client.post(Constants.API_URL, params, new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {
                // called before request is started
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                //setting the new location AND GETTING THE JSON RESPONSE!
                String decoded = null;  // example for one encoding type
                try { decoded = new String(response, "UTF-8");}
                catch (UnsupportedEncodingException e) { e.printStackTrace();}

                Log.v(TAG, "API call onSuccess = " + statusCode + ", Headers: " + headers[0] + ", response.length: " +response.length +
                        ", decoded:" + decoded);

                try {
                    DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
                    String[] myPinImages = new String[]{"blue","cyan","darkgreen","gold","green","orange","pink","purple","red","yellow","cyangray"};
                    JSONArray list = new JSONArray(decoded);
                    Log.d(TAG, "API Call postGetRoomWIP getroom returned list.length: " + list.length());
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();



                    //SCXTT WIP
                    // WE NEED to remove all the junk further below and just set up the room array now
                    // if no _roomArray then init it else removeallobjects from it
                    // EXAMPLE:  ArrayList<Room> roomArray = new ArrayList<Room>();
                    // EXAMPLE:  students.add(new Student());
                    roomArray.clear();

                    // set a myPinImages array


                    for (int i=0; i < list.length(); i++) {
                        JSONObject obj = list.getJSONObject(i);



                        //SCXTT WIP
                        // WE NEED to remove all the junk further below and just set up the room array now
                        // set vals (see iOS) for mNickName, mLocation, gmtDateString, myPinImages
                        String nickName = obj.getString("nickname");
                        String mLocation = obj.getString("location");
                        String gmtDateString = obj.getString("loc_time");
                        //String myPinImages = obj.getString("");


                        // if !mLocation=0.0,0.0 then new Room roomObj(deviceSingleton.roomName, mNickName, mLocation, gmtDateString, myPinImages
                        // if !_roomArray the init _roomArray
                        // _roomArray add object roomObj

                        // if _roomArray length == 0 && centerOnThisGuy.length > 0 then returnToAllWithMessage:@"Eveyone has left the map group"]



                        char ch = nickName.charAt(0);
                        int asciiCode = (int) ch;
                        int digit = asciiCode % 10;
                        int resID = getResources().getIdentifier(myPinImages[digit], "drawable", getPackageName());

                        String[] latlong =  obj.getString("location").split(",");
                        double latitude = Double.parseDouble(latlong[0]);
                        double longitude = Double.parseDouble(latlong[1]);

                        //Get distance from me to LatLng for the title
                        Location oldLoc = deviceSingleton.getMyNewLocation();

                        Location pinLoc = new Location("dummyprovider");
                        pinLoc.setLatitude(latitude);
                        pinLoc.setLongitude(longitude);

                        float distanceBetween;
                        float distanceInYards;
                        float distanceInMiles;

                        if (oldLoc == null ) {
                            distanceBetween = 0;
                            distanceInYards = 0;
                            distanceInMiles = 0;

                        } else {
                            distanceBetween = oldLoc.distanceTo(pinLoc);
                            distanceInYards = (float) (distanceBetween * 1.09361);
                            distanceInMiles = distanceInYards / 1760;
                        }

                        String pinDisplayDistance;

                        if (distanceInYards > 500) {
                            String myMiles = String.format("%.1f", distanceInMiles);
                            pinDisplayDistance = myMiles + " miles";
                        } else {
                            String myYards = String.format("%.1f", distanceInYards);
                            pinDisplayDistance = myYards + " y";
                        }

                        String annotationTitle = obj.getString("nickname");
                        String annotationSnippet = obj.getString("loc_time") + ", "+ pinDisplayDistance;

                        // MOVE this to updatePointsOnMapWithAPIData
                        //AND REPLACE WITH adding Room roomObj to _roomArray
                        Marker mAdd = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(latitude, longitude))
                                .icon(BitmapDescriptorFactory.fromResource(resID))
                                .title(annotationTitle)
                                .snippet(annotationSnippet)
                                .anchor(0.4727f, 0.5f));

                        builder.include(mAdd.getPosition());

                        for (Marker m : markers) {
                            if (m.getTitle().equals(annotationTitle)){
                                //groove the marker baby
                                m.setPosition(new LatLng(latitude, longitude));
                                if (m.getTitle().equals("5sSimulator")){
                                    m.setSnippet("WHA " + annotationSnippet);
                                    m.showInfoWindow();
                                    Log.d(TAG, "WIP-> FOUND 5sSim and the snippet is " + annotationSnippet);
                                }
                                break;
                            } else {

                            }
                        }
                        // END MOVE this to updatePointsOnMapWithAPIData

                    }

                    //MOVE this to updatePointsOnMapWithAPIData
                    //Back up camera zoom level to see all pins
                    LatLngBounds bounds = builder.build();
                    int padding = 20; // offset from edges of the map in pixels
                    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
//                    mMap.moveCamera(cu);
                    // END MOVE this to updatePointsOnMapWithAPIData

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.v(TAG, "API call onFailure = " + errorResponse);
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });
        Log.v(TAG, "API call response out of catch = " + response);

        //END getroom API call
        //end getroom code
    }

    private void hookUpMessageListAdapter(){
        ///////////////////////////////////////////
        // Temporary below - Set to use singleton tempTextArray for now
        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        this.arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                deviceSingleton.getTempTextArray() );

        messageList.setAdapter(arrayAdapter);
        ///////////////////////////////////////////
    }

    public void postGetRoomMessages(){
        Log.d(TAG, "postGetRoomMessages");
        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        AsyncHttpClient client2 = new AsyncHttpClient();
        RequestParams params2 = new RequestParams();
        params2.put("cmd", "getroommessages");
        params2.put("user_id", deviceSingleton.getUserId());
        params2.put("location", deviceSingleton.getMyLocStr());
        params2.put("secret_code", deviceSingleton.getSecretCode());

        client2.post(Constants.API_URL, params2, new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {
                // called before request is started
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                //setting the new location AND GETTING THE JSON RESPONSE!

                String decoded = null;  // example for one encoding type
                try {
                    decoded = new String(response, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Log.v(TAG, "API call onSuccess = " + statusCode + ", Headers: " + headers[0] + ", response.length: " + response.length +
                        ", decoded:" + decoded);
                JSONObject jObj = null;
                try {
                    jObj = new JSONObject(decoded);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
                    JSONArray list = new JSONArray(decoded);
                    Log.d(TAG, "API Call getroommessages returned list.length: " + list.length());
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject obj = list.getJSONObject(i);

                        // JSON format is {"message_id":"1051","user_id":"8BF13A775C1844669F678DBB36F6D73D","nickname":"gramma null","message":"Good night all love you guys","location":"39.941742, -85.916614","secret_code":"harnk","time_posted":"2015-09-25 01:11:52"},
                        String senderName = obj.getString("nickname");
                        String text = obj.getString("message");
                        String location = obj.getString("location");
                        String dateStr = obj.getString("time_posted");
                        Message message = new Message(senderName, dateStr, text, location);
//                        Message message = new Message();
//                        message.setSenderName(senderName);
//                        message.setText(text);
                        // Now add the message to the ArrayList
                        deviceSingleton.addMessage(message);

                        //Need to get these into the adapter
                        Log.v(TAG, senderName + " - " + dateStr + " - " + location + " ADDED to Singleton ArrayList messages message: " + text);
                    }
                    hookUpMessageListAdapter();
                    scrollMyListViewToBottom();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                // called when response HTTP status is "200 OK"
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.v(TAG, "API call onFailure = " + errorResponse);
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });
        Log.v(TAG, "API call response out of catch = " + response);

    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
//            updateStatus(); //this function can change value of mInterval.
            postGetRoom();
            mHandler.postDelayed(mStatusChecker, mInterval);
        }
    };

    void startRepeatingTask() {
        Log.d(TAG, "startRepeatingTask");
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

//    public void startGetRoomTimer() {
//        Log.v(TAG, "startGetRoomTimer");
//        this.stopGetRoomTimer();
//        //iOS _isFromNotification = YES;
//        getRoomTimer = new Handler();
//        postGetRoom();
//    }
//
//    public void stopGetRoomTimer() {
//        Log.v(TAG, "stopGetRoomTimer");
//        getRoomTimer.removeCallbacks(mStatusChecker);
//
//    }

    /* Class My Location Listener */
    public class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();

            String oldLocStr = deviceSingleton.getMyLocStr() + "";
            String newLocStr = loc.getLatitude() + ", " + loc.getLongitude();
            if (oldLocStr.equals(newLocStr)) {
//                Log.d(TAG, "WE DIDNT REALLY MOVE");
            } else {
                Log.v(TAG, "oldLoc: " + deviceSingleton.getMyNewLocation() + " loc: " + loc);
                Location oldLoc = deviceSingleton.getMyNewLocation();
                float distanceMoved = oldLoc.distanceTo(loc);
                Log.v(TAG, "WE MOVED oldLocStr:[" + oldLocStr + "] newLocStr:[" + newLocStr + "] distance in meters: " + distanceMoved);
                centerMapOnMyLoc();
            }

            deviceSingleton.setMyNewLocation(loc);
            deviceSingleton.setMyLocStr(newLocStr);

        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "GPS Disabled");

        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "GPS Enabled");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.v(TAG, "GPS onStatusChanged provider: " + provider + ", status: " + status + ", extras: " + extras);

        }

    }/* End of Class MyLocationListener */
    //endregion

}
