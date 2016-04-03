package com.harnk.whereru;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by snull on 11/23/15.
 */
public class DeviceSingleton {
    private static DeviceSingleton mInstance = null;

    private String deviceId; //not used
    private String userId;
    private String gcmToken;
    private String myLocStr;
    private String nickname, secretCode;
    private Location myNewLocation;
    private boolean notificationsAreDisabled;
    private boolean imInARoom;
    private boolean mapIsActive;
    private boolean joinedChat;
    private ArrayList<String> tempTextArray = new ArrayList<String>();
    // Message array
    private ArrayList<Message> messages = new ArrayList<Message>();
    private Context appContext;

    public void init(Context context){
        if(appContext == null){
            appContext = context;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        this.setJoinedChat(prefs.getBoolean("savedJoinedChat", false));
        this.setImInARoom(prefs.getBoolean("savedImInARoom", false));
        this.setUserId(prefs.getString("savedUserId", ""));
        this.setSecretCode(prefs.getString("savedSecretCode", ""));
        this.setNickname(prefs.getString("savedNickname", ""));
//        joinedChat = false;
//        deviceId = this.findDeviceID();
//        myCurrentLoc = this.getMyCurrentLoc;
    }

    private DeviceSingleton(){
    }

    public static DeviceSingleton getInstance(){
        if(mInstance == null)
        {
            mInstance = new DeviceSingleton();
        }
        return mInstance;
    }

    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }

    public void clearMessages() {
        this.messages.clear();
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public void addMessage(Message message){
        this.messages.add(message);
        //Temporary stuff below, will replace later when the custom adapter is done
        this.tempTextArray.add(message.getText());
    }
    // Temporary below
    public ArrayList<String> getTempTextArray() {
        return tempTextArray;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {

        this.userId = userId;
        // save to prefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("savedUserId", userId);
        editor.commit();

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

    public String getNickname() { return nickname; }

    public void setNickname(String myNickname) {
        this.nickname = myNickname;
        // save to prefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("savedNickname", myNickname);
        editor.commit();
    }

    public String getSecretCode() { return secretCode; }

    public void setSecretCode(String mySecretCode) {
        this.secretCode = mySecretCode;
        // save to prefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("savedSecretCode", mySecretCode);
        editor.commit();
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

    public boolean isImInARoom() {return imInARoom;}

    public void setImInARoom(boolean imInARoom) {
        this.imInARoom = imInARoom;
        // save to prefs
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("savedImInARoom", imInARoom);
        editor.commit();

    }

    public boolean isMapIsActive() {
        return mapIsActive;
    }

    public void setMapIsActive(boolean mapIsActive) {
        this.mapIsActive = mapIsActive;
    }

    public boolean isJoinedChat() { return joinedChat; }

    public void setJoinedChat(boolean joinedChat) {
        this.joinedChat = joinedChat;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("savedJoinedChat", joinedChat);
        editor.commit();
    }

}
