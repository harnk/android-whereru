package com.harnk.whereru;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLngBounds;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;

import cz.msebera.android.httpclient.Header;

/**
 * Created by scottnull on 2/13/16.
 */
public class BackgroundLocationService extends Service {

    public static final String BROADCAST_ACTION = "Hello World";
    private static final int THIRTY_SECONDS = 10000;
    public LocationManager locationManager;
    public MyLocationListener listener;
    public Location previousBestLocation = null;
    private boolean isUpdating;
    private boolean deviceHasMoved;
    private boolean iHaventStartedTheGPSYet = true;
    private static final String TAG = "SCXTT";

    Intent intent;
    int retryCounter = 0;

    @Override
    public void onCreate()
    {
        Log.d(TAG, "BackgroundLocationService.onCreate ");
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);
        isUpdating = false;
    }

    private void startLocationManager(Intent intent)
    {
        Log.d(TAG, "BackgroundLocationService.startLocationManager starting up the GPS");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 0, listener);
        deviceHasMoved = true;
        iHaventStartedTheGPSYet = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "BackgroundLocationService.onStartCommand");
        if (iHaventStartedTheGPSYet) {
            startLocationManager(intent);
        } else {
            Log.d(TAG, "DOOD IM ALREADY RUNNING - do nothing");
        }
//        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        listener = new MyLocationListener();
//        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, listener);
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 0, listener);
//        deviceHasMoved = true;
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        Log.v(TAG, "isBetterLocation??");
        if (currentBestLocation == null) {
            // A new location is always better than no location
            Log.v(TAG, "A new location is always better than no location");
//            previousBestLocation = location;
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > THIRTY_SECONDS;
        boolean isSignificantlyOlder = timeDelta < -THIRTY_SECONDS;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            Log.v(TAG, "new location is isSignificantlyNewer");
            return true;
            // If the new location is more than two minutes older, it must be worse
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        Log.v(TAG, "accuracyDelta:" + accuracyDelta);
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            Log.v(TAG, "new location isMoreAccurate");
            return true;
        } else if (isNewer && !isLessAccurate) {
            Log.v(TAG, "new location isNewer && !isLessAccurate");
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            Log.v(TAG, "new location is isNewer && !isSignificantlyLessAccurate && isFromSameProvider");
            return true;
        }
        Log.v(TAG, "isBetterLocation ... nope");
        return false;
    }

    protected void resetIsUpdating(){
        isUpdating = false;
    }

    protected void postLiveUpdate() {
        Log.v(TAG, "postLiveUpdate cmd:liveupdate user_id:getfromsigleton location:this is a loc string");
        //Do a liveupdate API call
        Log.v(TAG, "BackgroundLocationService postLiveUpdate set looking = 0");
        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("cmd", "liveupdate");
        params.put("user_id", deviceSingleton.getUserId());
        params.put("location", deviceSingleton.getMyLocStr());
        Log.d(TAG, "BACKGROUND postLiveUpdate is using deviceSingleton.getMyLocStr():" + deviceSingleton.getMyLocStr());
        // need to add API call next
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
                    Log.v(TAG, "BackgroundLocationService try");
                    decoded = new String(response, "UTF-8");
                }
                catch (UnsupportedEncodingException e) {
                    Log.d(TAG, "BackgroundLocationService catch");
                    e.printStackTrace();
                }

                Log.d(TAG, "API BACKGROUND call onSuccess = " + statusCode + ", Headers: " + headers[0] + ", response.length: " +response.length +
                        ", decoded:" + decoded);

                try {
                    DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
                    JSONArray list = new JSONArray(decoded);
                    Log.v(TAG, "API BACKGROUND Call postLiveUpdate returned list.length: " + list.length());
                    // SCXTT Need to change this next loop to look for a looker like iOS does
//                    LatLngBounds.Builder builder = new LatLngBounds.Builder();

                    boolean foundALooker = false;
                    for (int i=0; i < list.length(); i++) {
                        JSONObject obj = list.getJSONObject(i);
                        String nickName = obj.getString("nickname");
                        String mLooking = obj.getString("looking");
                        if (mLooking.equals("1")) {
                            Log.d(TAG, nickName + " is looking");
                            foundALooker = true;
                            Log.d(TAG, "Toggle singleton BOOL someoneIsLooking to foundALooker=YES and break the loop");
                            break;
                        }
                    }
                    if (foundALooker){
                        Log.d(TAG, "since someoneIsLooking keep updating my loc in the background");
                        retryCounter = 0;
                    } else {
                        retryCounter += 1;
                        Log.d(TAG, "NO ONE is looking so why am I wasting my battery with these background API calls?!? Retry:" + retryCounter);
                        if (retryCounter > 3) {
                            Log.d(TAG, "IM DONE IN BackgroundLocationService STOPPING");
                            retryCounter = 0;
                            stopSelf();
                        }
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

        //do a bunch of stuff then ...
        resetIsUpdating();
    }



    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }



    @Override
    public void onDestroy() {
        // handler.removeCallbacks(sendUpdatesToUI);
        super.onDestroy();
        Log.d(TAG, "onDestroy DONE");
        locationManager.removeUpdates(listener);
        iHaventStartedTheGPSYet = true;
    }

    public static Thread performOnBackgroundThread(final Runnable runnable) {
        final Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } finally {

                }
            }
        };
        t.start();
        return t;
    }




    public class MyLocationListener implements LocationListener
    {

        public void onLocationChanged(final Location loc)
        {
            Log.v(TAG, "BACKGROUND SERVICE Location changed to Lat:" + loc.getLatitude() + ", Lon:" + loc.getLongitude() + " Provider:" + loc.getProvider() + " Accuracy:" + loc.getAccuracy());
            DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
            String newLocStr = loc.getLatitude() + ", " + loc.getLongitude();

            if (previousBestLocation == null) {
                previousBestLocation = loc;
            }
            if(isBetterLocation(loc, previousBestLocation)) {
                deviceHasMoved = true;
                deviceSingleton.setMyNewLocation(loc);
                deviceSingleton.setMyLocStr(newLocStr);
                Log.d(TAG, "BETTER location found Lat:" + loc.getLatitude() + ", Lon:" + loc.getLongitude() + " Provider:" + loc.getProvider() + " Accuracy:" + loc.getAccuracy());
//                intent.putExtra("Latitude", loc.getLatitude());
//                intent.putExtra("Longitude", loc.getLongitude());
//                intent.putExtra("Provider", loc.getProvider());
//                sendBroadcast(intent);
                //SCXTT What to do with the intent above???

                //SCXTT do and API call to update my loc
                this.postMyLoc();
                previousBestLocation = loc;

            }
        }

        public void postMyLoc() {
            Log.v(TAG, "NEED TO postMyLoc now baby");
            DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
            if (deviceSingleton.isImInARoom()){
                Log.v(TAG, "Im IN a room and isUpdating:" + isUpdating + ", deviceHasMoved:" + deviceHasMoved);
                if (!isUpdating && deviceHasMoved){
                    Log.v(TAG, "!isUpdating && deviceHasMoved so postLiveUpdate dood");
                    isUpdating = true;
                    postLiveUpdate();
                    deviceHasMoved = false;

                }
            } else {
                Log.d(TAG, "Im not in a room");
                Log.d(TAG, "deviceSingleton.getSecretCode():" + deviceSingleton.getSecretCode());
                Log.d(TAG, "deviceSingleton.getUserId():" + deviceSingleton.getUserId());
                Log.d(TAG, "deviceSingleton.getMyLocStr():" + deviceSingleton.getMyLocStr());
            }
//            if ([[SingletonClass singleObject] imInARoom]) {
////        NSLog(@"imInARoom is true");
//                if (!_isUpdating) {
////            NSLog(@"were not _isUpdating");
//                    if (_deviceHasMoved) {
//                        _isUpdating = YES;
////                NSLog(@" bkgnd posting my loc %@", [[SingletonClass singleObject] myLocStr]);
//                        [self postLiveUpdate];
//                        _deviceHasMoved = NO;
//
//                        // Need to check response for anyone still looking and set _isAnyoneStillLooking
//
//                    }
//                } else {
////            NSLog(@"no API call since _isUpdating is already YES = Busy");
//                }
//            } else {
////        NSLog(@"imInARoom is false - no update");
//            }

        }

        public void onProviderDisabled(String provider)
        {
            Toast.makeText(getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT).show();
        }


        public void onProviderEnabled(String provider)
        {
            Toast.makeText( getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
        }


        public void onStatusChanged(String provider, int status, Bundle extras)
        {

        }

    }

}
