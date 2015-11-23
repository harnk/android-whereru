package com.harnk.whereru;

import android.content.Context;
import android.location.Location;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.UUID;

/**
 * Created by snull on 11/23/15.
 */
public class DeviceSingleton {
    private static DeviceSingleton mInstance = null;

    private String deviceId;
    private String userId;
    private String gcmToken;
    private String myLocStr;
    private Location myNewLocation;
    private boolean notificationsAreDisabled;
    private boolean imInARoom;
    private boolean mapIsActive;

    private Context appContext;
    public void init(Context context){
        if(appContext == null){
            appContext = context;
        }
        deviceId = this.findDeviceID();
//        myCurrentLoc = this.getMyCurrentLoc;
    }

    private DeviceSingleton(){

    }

    //I am not using deviceId, I am instead using userId for the user's unique GUID
    private String findDeviceID() {
        final TelephonyManager tm = (TelephonyManager) appContext.getSystemService(Context.TELEPHONY_SERVICE);

        final String tmDevice, tmSerial, androidId;
        tmDevice = "" + tm.getDeviceId();
        tmSerial = "" + tm.getSimSerialNumber();
        androidId = "" + android.provider.Settings.Secure.getString(appContext.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());
        String deviceId = deviceUuid.toString();

        return deviceId;
    }

    public static DeviceSingleton getInstance(){
        if(mInstance == null)
        {
            mInstance = new DeviceSingleton();
        }
        return mInstance;
    }

    public String getDeviceId(){
        return this.deviceId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getGcmToken() {
        return gcmToken;
    }

    public void setGcmToken(String gcmToken) {
        this.gcmToken = gcmToken;
    }

    public String getMyLocStr() {
        return myLocStr;
    }

    public void setMyLocStr(String myLocStr) {
        this.myLocStr = myLocStr;
    }

    public Location getMyNewLocation() {
        return myNewLocation;
    }

    public void setMyNewLocation(Location myNewLocation) {
        this.myNewLocation = myNewLocation;
    }

    public boolean isNotificationsAreDisabled() {
        return notificationsAreDisabled;
    }

    public void setNotificationsAreDisabled(boolean notificationsAreDisabled) {
        this.notificationsAreDisabled = notificationsAreDisabled;
    }

    public boolean isImInARoom() {
        return imInARoom;
    }

    public void setImInARoom(boolean imInARoom) {
        this.imInARoom = imInARoom;
    }

    public boolean isMapIsActive() {
        return mapIsActive;
    }

    public void setMapIsActive(boolean mapIsActive) {
        this.mapIsActive = mapIsActive;
    }

}