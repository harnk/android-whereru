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

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;

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

    Intent intent;
    int counter = 0;

    @Override
    public void onCreate()
    {
        Log.d("SCXTT", "starting background location service");
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);
        isUpdating = false;
    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 4000, 0, listener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 4000, 0, listener);
        deviceHasMoved = true;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            Log.d("SCXTT", "A new location is always better than no location");
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
            Log.d("SCXTT", "new location is isSignificantlyNewer");
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        Log.d("SCXTT", "accuracyDelta:" + accuracyDelta);
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            Log.d("SCXTT", "new location isMoreAccurate");
            return true;
        } else if (isNewer && !isLessAccurate) {
            Log.d("SCXTT", "new location isNewer && !isLessAccurate");
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            Log.d("SCXTT", "new location is isNewer && !isSignificantlyLessAccurate && isFromSameProvider");
            return true;
        }
        return false;
    }

    protected void resetIsUpdating(){
        isUpdating = false;
    }

    protected void postLiveUpdate() {
        Log.d("SCXTT", "postLiveUpdate cmd:liveupdate user_id:getfromsigleton location:this is a loc string");
        //Do a liveupdate API call
        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("cmd", "liveupdate");
        params.put("user_id", deviceSingleton.getUserId());
        params.put("location", deviceSingleton.getMyLocStr());
        // need to add API call next


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
        Log.d("SCXTT", "DONE");
        locationManager.removeUpdates(listener);
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
            Log.d("SCXTT", "Location changed in BACKGROUND SERVICE to Lat:" + loc.getLatitude() + ", Lon:" + loc.getLongitude() + " Provider:" + loc.getProvider() + " Accuracy:" + loc.getAccuracy());
            if (previousBestLocation == null) {
                previousBestLocation = loc;
            }
            if(isBetterLocation(loc, previousBestLocation)) {
                deviceHasMoved = true;
                loc.getLatitude();
                loc.getLongitude();
                Log.d("SCXTT", "BETTER location found Lat:" + loc.getLatitude() + ", Lon:" + loc.getLongitude() + " Provider:" + loc.getProvider() + " Accuracy:" + loc.getAccuracy());
                intent.putExtra("Latitude", loc.getLatitude());
                intent.putExtra("Longitude", loc.getLongitude());
                intent.putExtra("Provider", loc.getProvider());
                sendBroadcast(intent);
                //SCXTT What to do with the intent above???
                //SCXTT do and API call to update my loc
                this.postMyLoc();
                previousBestLocation = loc;

            }
        }

        public void postMyLoc() {
            Log.d("SCXTT", "NEED TO postMyLoc now baby");
            DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
            if (deviceSingleton.isImInARoom()){
                Log.d("SCXTT", "Im IN a room and isUpdating:" + isUpdating + ", deviceHasMoved:" + deviceHasMoved);
                if (!isUpdating && deviceHasMoved){
                    Log.d("SCXTT", "!isUpdating && deviceHasMoved so postLiveUpdate dood");
                    isUpdating = true;
                    postLiveUpdate();
                    deviceHasMoved = false;

                }
            } else {
                Log.d("SCXTT", "Im not in a room");
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
