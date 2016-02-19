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
import android.support.v4.content.res.TypedArrayUtils;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
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
import com.google.android.gms.maps.model.CameraPosition;
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
import java.util.TimeZone;
import java.util.UUID;

import cz.msebera.android.httpclient.Header;

public class ShowMapActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

//    public static final String API_URL = "http://www.altcoinfolio.com//whereruprod/api/api.php";

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private List<Marker> markers = new ArrayList<Marker>();
    private ListView messageList;
    private ArrayAdapter<String> arrayAdapter;

    private ArrayList<Message> messageObjectList = new ArrayList<Message>();
    private MessageAdapter messageObjectAdapter;

    private ArrayAdapter<String> pinArrayAdapter;
    private Spinner spinner;
    private DeviceUuidFactory deviceUuidFactory;
    private ArrayList<Room> roomArray = new ArrayList<Room>();
    private ArrayList<String> tempPinArray = new ArrayList<String>(); // temporary pin list
    private Toast toast;

    //Control booleans
    private boolean isUpdating;
    private boolean isFromNotification;
    private boolean pickerIsUp;
    private boolean okToRecenterMap;
    private boolean pinPickerButtonEnabled;
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
    private int updateLocsInterval = 5000; // 5 seconds by default, can be changed later
    private int updateMessagesInterval = 62000; // 62 seconds by default, can be changed later
    private Handler updateLocsHandler;
    private Handler updateMessagesHandler;


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
//        Log.d(TAG, "Singleton deviceId : " + (String) deviceSingleton.getDeviceId() + " NOT BEING USED, use userId instead");


    }

    private void userDidLeave() {
        //userDidLeave saves joinedchat false to SharedPrefs
        // and singleton setImInARoom to false
        // then show login
        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        deviceSingleton.setImInARoom(false);
        deviceSingleton.setJoinedChat(false);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("savedJoinedChat", false);
        editor.commit();
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
                Log.d(TAG, "postLeaveRequest API call onFailure = " + errorResponse.toString() + " e: " + e.toString() + " statusCode: " + statusCode);
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

    private void returnToAllWithMessage(String toastMsg) {
        centerOnThisGuy = "";

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("SCXTT", " onCreate");
        MapsInitializer.initialize(getApplicationContext());

        okToRecenterMap = true;
        pickerIsUp = false;
        isFromNotification = false;
        centerOnThisGuy = "";

        setContentView(R.layout.activity_show_map);
//        MapsInitializer.initialize(this);
        setUpMapIfNeeded();

        //Timer setup
        updateLocsHandler = new Handler();
        updateMessagesHandler = new Handler();
//        startRepeatingTask(); <--moved to onStart

        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        messageObjectList = deviceSingleton.getMessages();
        messageList = (ListView) findViewById(R.id.listView);
        messageObjectAdapter = new MessageAdapter(this, R.layout.single_row, messageObjectList);
        messageList.setAdapter(messageObjectAdapter);
        messageList.setDivider(null);
        messageList.setDividerHeight(0);

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

    } ////END onCreate //////////////////////////////////////////////////////////////////////////////////

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
            setTitle(deviceSingleton.getSecretCode());
            setTitleColor(3);
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
        MenuItem item = menu.findItem(R.id.spinner);
        spinner = (Spinner) MenuItemCompat.getActionView(item);

        for(int i = 0; i < roomArray.size(); i++) {
            tempPinArray.add(roomArray.get(i).getMemberNickName());

        }
        pinArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, tempPinArray);
//      Specify the layout to use when the list of choices appears
        pinArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(pinArrayAdapter); // set the adapter to provide layout of rows and content
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d("SCXTT", "WE CAUGHT A FISH: " + tempPinArray.get(position));
                DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
                centerOnThisGuy = tempPinArray.get(position);
                okToRecenterMap = true;
                if (centerOnThisGuy.equals("[" + deviceSingleton.getSecretCode() + "]")) {
                    Toast.makeText(view.getContext(), "Returning to view of entire map group " + centerOnThisGuy, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(view.getContext(), "Locating " + centerOnThisGuy, Toast.LENGTH_LONG).show();
                }

                //scxtty
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d("SCXTT", "WE LOST THE DANG THING");
            }
        });

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

//            case R.id.action_pinpicker:
//                Log.d(TAG, "Pin picker selected");
////                centerOnThisGuy = "chels";
////                okToRecenterMap = true;
//                break;

            case R.id.action_reload:
                Log.d(TAG, "Reload selected");
//                Temporary below
                this.arrayAdapter.notifyDataSetChanged();
                this.scrollMyListViewToBottom();
                centerMapOnMyLoc();
                this.postFindRequest();
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
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {

            // Google map init block
            CustomMapFragment customMapFragment = ((CustomMapFragment) getSupportFragmentManager().findFragmentById(R.id.map));
            customMapFragment.setOnDragListener(new MapWrapperLayout.OnDragListener() {
                @Override
                public void onDrag(MotionEvent motionEvent) {
                    Log.v("SCXTT", String.format("DRAGGGING ME: %s", motionEvent));
                    // Handle motion event:
                    int action = motionEvent.getAction();
                    if (1 == action){
                        Log.v("SCXTT", "drag ended - action: " + action);
                        okToRecenterMap = false;
                    }
                }
            });
            mMap = customMapFragment.getMap();

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
            Log.d("SCXTT", "The play beep sound was here");
//            try {
//                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
//                r.play();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }

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

    private void postFindRequest() {
        Log.d("SCXTT", "postFindRequest");
        //Do a getroom API call
        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("cmd", "find");
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
                    JSONArray list = new JSONArray(decoded);
                    Log.d(TAG, "API Call postGetRoomWIP getroom returned list.length: " + list.length());
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    roomArray.clear();

                    for (int i=0; i < list.length(); i++) {
                        JSONObject obj = list.getJSONObject(i);
                        String nickName = obj.getString("nickname");
                        String mLocation = obj.getString("location");
                        String gmtDateString = obj.getString("loc_time");
                        // add the guys above to Room roomObj
                        Room roomObj = new Room(deviceSingleton.getSecretCode(), nickName, mLocation, gmtDateString);
                        roomArray.add(roomObj);
                    }
                    Log.d(TAG, "WIP remove this next call to updatePointsOnMapWithAPIData and make a notification trigger it like iOS");
                    try {
                        updatePointsOnMapWithAPIData();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.v(TAG, "postGetRoomWIP API call onFailure = " + errorResponse);
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });
        Log.v(TAG, "API call response out of catch = " + response);


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
                    JSONArray list = new JSONArray(decoded);
                    Log.d(TAG, "API Call postGetRoomWIP getroom returned list.length: " + list.length());
                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    roomArray.clear();

                    for (int i=0; i < list.length(); i++) {
                        JSONObject obj = list.getJSONObject(i);
                        String nickName = obj.getString("nickname");
                        String mLocation = obj.getString("location");
                        String gmtDateString = obj.getString("loc_time");
                        // add the guys above to Room roomObj
                        Room roomObj = new Room(deviceSingleton.getSecretCode(), nickName, mLocation, gmtDateString);
                        roomArray.add(roomObj);
                    }
                    Log.d(TAG, "WIP remove this next call to updatePointsOnMapWithAPIData and make a notification trigger it like iOS");
                    try {
                        updatePointsOnMapWithAPIData();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.v(TAG, "postGetRoomWIP API call onFailure = " + errorResponse);
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });
        Log.v(TAG, "API call response out of catch = " + response);

    }

    private void hookUpPinListAdapter(){
        this.pinArrayAdapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, tempPinArray);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, tempPinArray);
//      Specify the layout to use when the list of choices appears
        pinArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//      spinner.setAdapter(pinArrayAdapter); // set the adapter to provide layout of rows and content

    }

    private void hookUpMessageListAdapter(){
        ///////////////////////////////////////////
        // Temporary below - Set to use singleton tempTextArray for now
        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        this.arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                deviceSingleton.getTempTextArray() );
//        messageList.setAdapter(arrayAdapter);


        //ADD MessageAdapter here to replace above stuff
        messageObjectList = deviceSingleton.getMessages();
        messageList = (ListView) findViewById(R.id.listView);
        messageObjectAdapter = new MessageAdapter(this, R.layout.single_row, messageObjectList);
        messageList.setAdapter(messageObjectAdapter);
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
                Log.v(TAG, "postGetRoomMessages API call onFailure = " + errorResponse);
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });
        Log.v(TAG, "API call response out of catch = " + response);

    }


    Runnable messageStatusChecker = new Runnable() {
        @Override
        public void run() {
//            updateStatus(); //this function can change value of updateMessagesInterval.
            postGetRoomMessages();
            updateMessagesHandler.postDelayed(messageStatusChecker, updateMessagesInterval);
        }
    };

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
//            updateStatus(); //this function can change value of updateLocsInterval.
            postGetRoom();
            updateLocsHandler.postDelayed(mStatusChecker, updateLocsInterval);
        }
    };

    void startRepeatingTask() {
        Log.d(TAG, "startRepeatingTask");
        mStatusChecker.run();
        messageStatusChecker.run();
    }

    void stopRepeatingTask() {
        updateLocsHandler.removeCallbacks(mStatusChecker);
        updateMessagesHandler.removeCallbacks(messageStatusChecker);
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

    private int getThisGuysRow(String thisGuy){
        for(int i = 0; i < roomArray.size(); i++) {
            Room thisRoomObj = roomArray.get(i);
            if (thisGuy.equals(thisRoomObj.getMemberNickName())) {
                return i;
            }
        }
            return -1;
    }

    public boolean annTitleHasLeftRoom(String nickName) {
        if (nickName.equals("Current Location")) {
            return false;
        }
        for(int i = 0; i < roomArray.size(); i++){
            Room thisRoomObj = roomArray.get(i);
            if (nickName.equals(thisRoomObj.getMemberNickName())){
                return false;
            }
        }
        return true;
    }

    private boolean isToday(String dateStringToCheck){
        try{
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            Date checkDate = formatter.parse(dateStringToCheck);
            Date todaysDate = formatter.parse(today);

            if (checkDate.compareTo(todaysDate)<0) {
//                Log.d("SCXTT", "today is Greater than dateStringToCheck: " + checkDate.compareTo(todaysDate));
                return false;
            } else {
                return true;
            }

        }catch (ParseException e1){
            e1.printStackTrace();
        }
        return true;
    }

    private boolean isYesterday(String dateStringToCheck){
        return false;
    }

    private String formatDateTime(String inputDateTime) throws ParseException {
        // formatDateTime received: 2015-11-26 01:58:37
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date myDate = simpleDateFormat.parse(inputDateTime);
        DateFormat outputFormat = new SimpleDateFormat("M/d/yy, h:mm aa");
        DateFormat todayFormat = new SimpleDateFormat("h:mm aa");
        String outputDateStr = "";
        if (isToday(inputDateTime.split(" ")[0])){
            outputDateStr = "Today, " + todayFormat.format(myDate);
        } else {
            outputDateStr = outputFormat.format(myDate);
        }
        return outputDateStr;
    }


    public void updatePointsOnMapWithAPIData() throws ParseException {
        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        Location location = new Location("dummyprovider");

        // seed the region values with my current location and to set the span later to include all the Markers
        Location southWest = new Location("dummyprovider");
        Location northEast = new Location("dummyprovider");
        double southWestLat, southWestLon, northEastLat, northEastLon;
        String mLoc = deviceSingleton.getMyLocStr();
        String[] strs = mLoc.split(",");
        southWest.setLatitude(Double.parseDouble(strs[0]));
        southWest.setLongitude(Double.parseDouble(strs[1]));
        northEast = southWest;

        southWestLat = southWest.getLatitude();
        southWestLon = southWest.getLongitude();
        northEastLat = northEast.getLatitude();
        northEastLon = northEast.getLongitude();
//        MKCoordinateRegion region;

        // loop thru all roomArray objects
        // pull from roomArray where 'who' matches memberNikName

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        if (roomArray.size() == 0) {
            pinPickerButtonEnabled = false;
            mMap.clear();
            markers.clear();
        } else {
            pinPickerButtonEnabled = true;
        }

        tempPinArray.clear();
        if (roomArray.size() > 0) {
            tempPinArray.add("[" + roomArray.get(0).getRoomName() + "]");
        }
        // Loop thru all Room items in roomArray
            for(int i = 0; i < roomArray.size(); i++) {
                tempPinArray.add(roomArray.get(i).getMemberNickName());
                boolean whoFound = false;
                Room thisRoomObj = roomArray.get(i);
                if (!thisRoomObj.getMemberLocation().equals("0.000000, 0.000000")) {
                    String[] strings = thisRoomObj.getMemberLocation().split(",");
                    double latitude = Double.parseDouble(strings[0]);
                    double longitude = Double.parseDouble(strings[1]);
                    String who = thisRoomObj.getMemberNickName();
                    String imageString = thisRoomObj.getMemberPinImage();
                    int useThisPin = getResources().getIdentifier(thisRoomObj.getMemberPinImage(), "drawable", getPackageName());

                    String gmtDateStr = thisRoomObj.getMemberUpdateTime(); //UTC needs to be converted to currentLocale
//                    NSString* dateString = [self localDateStrFromUTCDateStr:gmtDateStr];
//                    NSDate* date = [self dateFromUTCDateStr:gmtDateStr];
                    Log.v(TAG, "markers.size() = " + markers.size());
                    for (int m = 0; m < markers.size(); m++) {
                        Marker ann = markers.get(m);
                        //First see if this ann still has a _roomArray match
                        //or if the person has left the room kill this ann
                        if (annTitleHasLeftRoom(ann.getTitle())) {
                            if (ann.getTitle().equals(centerOnThisGuy)) {
//                               [self returnToAllWithMessage:@""];
                                //implement
                            }
                            Toast.makeText(this, ann.getTitle() + " has left the map group", Toast.LENGTH_SHORT).show();
                            centerOnThisGuy = "";
//                            markers.get(m).remove();
                            ann.remove();
                            markers.remove(m);
                        }
                        // implement
                        southWestLat = (Math.min(southWestLat, ann.getPosition().latitude));
                        southWestLon = (Math.min(southWestLon, ann.getPosition().longitude));
                        northEastLat = (Math.max(northEastLat, ann.getPosition().latitude));
                        northEastLon = (Math.max(northEastLon, ann.getPosition().longitude));
//                        southWest.setLatitude(Math.min(southWest.getLatitude(), ann.getPosition().latitude));
//                        southWest.setLongitude(Math.min(southWest.getLongitude(), ann.getPosition().longitude));
//                        northEast.setLatitude(Math.max(northEast.getLatitude(), ann.getPosition().latitude));
//                        northEast.setLongitude(Math.max(northEast.getLongitude(), ann.getPosition().longitude));
                        Log.v("SCXTT", "sw: " + southWestLat + ", " + southWestLon + " ne: " + northEastLat + ", " + northEastLon);

                        // Move the updated pin to its new locations
                        if (ann.getTitle().equals(who)) {
                            Log.v(TAG, "grooving " + thisRoomObj.getMemberNickName() + " at " + thisRoomObj.getMemberLocation() + " " + thisRoomObj.getMemberUpdateTime() + " memberPinImage:" + thisRoomObj.getMemberPinImage());
                            whoFound = true;
                            location.setLatitude(Double.parseDouble(strings[0]));
                            location.setLongitude(Double.parseDouble(strings[1]));
                            if (!thisRoomObj.getMemberLocation().equals("0.000000, 0.000000")) {
                                //Format the location to read distance from me now
                                //get location of me
                                Location locA = deviceSingleton.getMyNewLocation();

                                // Handle the location of the remote devices from the saved messages
//                                String[] strings =  thisRoomObj.getMemberLocation().split(",");
                                Location locB = new Location("dummyprovider");
                                locB.setLatitude(latitude);
                                locB.setLongitude(longitude);

                                float distanceFromMeInMeters = locA.distanceTo(locB);
                                float distanceInYards = (float) (distanceFromMeInMeters * 1.09361);
                                float distanceInMiles = distanceInYards / 1760;
                                String dateTimeToDisplay = formatDateTime(thisRoomObj.getMemberUpdateTime());
                                if (distanceInYards > 500) {
                                    ann.setSnippet(dateTimeToDisplay + ", " + String.format("%.1f", distanceInMiles) + " mi");
                                } else {
                                    ann.setSnippet(dateTimeToDisplay + ", " + String.format("%.1f", distanceInYards) + " y");
                                }
//                                ann.hideInfoWindow();
//                                ann.showInfoWindow();
//                              ann.loctime = date; // this prob isnt working either
                                ann.setPosition(new LatLng(latitude, longitude));
                            } // 0.000, 0.000
                        } // ann title = who
                    } // for marker in markers
                    // new who so add addAnnotation and set coordinate and location time and recenter the map
                    if (!whoFound) {
                        Log.v(TAG, "Adding new who with pin " + who + "" + imageString);

                        if (!thisRoomObj.getMemberLocation().equals("0.000000, 0.000000")) {
                            if (toast != null){
                                toast.cancel();
                            }
                            toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
                            toast.setText(who + " is in the map group");
                            toast.show();
//                            Toast.makeText(this, who + " is in the map group", Toast.LENGTH_SHORT).show();

                            String annotationTitle = thisRoomObj.getMemberNickName();
                            String dateTimeToDisplay = formatDateTime(thisRoomObj.getMemberUpdateTime());
                            String annotationSnippet = dateTimeToDisplay + ", " + "";
                            Marker annNew = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(latitude, longitude))
                                    .icon(BitmapDescriptorFactory.fromResource(useThisPin))
                                    .title(annotationTitle)
                                    .snippet(annotationSnippet)
                                    .anchor(0.4727f, 0.5f));
                            markers.add(annNew);
                            Log.v(TAG, "FORREALZ added one to markers now markers.size() = " + markers.size());
                            builder.include(annNew.getPosition());
                        } // 0.000, 0.000
                    } // !whofound
                } // 0.000, 0.000
            } // end for (Room *item in _roomArray)
        pinArrayAdapter.notifyDataSetChanged();
        // Recenter map
        if (okToRecenterMap) {
            if (getThisGuysRow(centerOnThisGuy) >= 0) {
                Log.d("SCXTT", "we found a guy to center on");
                Room thisRoomObj = roomArray.get(getThisGuysRow(centerOnThisGuy));
                Log.d(TAG, "CENTER ON " + thisRoomObj.getMemberNickName() + " at " + thisRoomObj.getMemberLocation() + " " + thisRoomObj.getMemberUpdateTime() + " memberPinImage:" + thisRoomObj.getMemberPinImage());

                String[] strings = thisRoomObj.getMemberLocation().split(",");
                double latitude = Double.parseDouble(strings[0]);
                double longitude = Double.parseDouble(strings[1]);
                Log.d("SCXTT", "center on latitude: " + latitude + " logitude: " + longitude);

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15));

            } else {
                Log.d("SCXTT", "NO guy to center on");
                LatLngBounds.Builder allBuilder = new LatLngBounds.Builder();
                //Convert Location to LatLng
                LatLng swLatLng = new LatLng(southWestLat, southWestLon);
                allBuilder.include(swLatLng);
                LatLng neLatLng = new LatLng(northEastLat, northEastLon);
                allBuilder.include(neLatLng);
                LatLngBounds region = allBuilder.build();
                int padding = 10; // offset from edges of the map in pixels
                CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(region, padding);
                mMap.animateCamera(cu);
//                [self reCenterMap:region meters:meters];
            } //end getThisGuysRow
        }
    }

    /* Class My Location Listener *************************************************************************************/
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
                Log.v(TAG, "WIP this next line is throwing exception errors every so often - FIX IT");
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


}
