package com.harnk.whereru;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by snull on 11/20/15.
 */
public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "SCXTT";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String message = data.getString("message");
        String payload = data.getString("payload");
        String extra = "";
        String asker = "";
        String loc = "";

        JSONObject jsonObject = null;
        // Parse the json payload
        try {
            jsonObject = new JSONObject(payload);
        } catch (JSONException e){
            e.printStackTrace();
        }
        try {
            extra = jsonObject.getString("extra");
        }catch (JSONException e){
            e.printStackTrace();
        }
        try {
            asker = jsonObject.getString("asker");
        }catch (JSONException e){
            e.printStackTrace();
        }
        try {
            loc = jsonObject.getString("loc");
        }catch (JSONException e){
            e.printStackTrace();
        }

        Log.d(TAG, "From: " + from);
        Log.d(TAG, "payload: " + payload);
        Log.d(TAG, "Message: " + message);
        Log.d(TAG, "extra: " + extra);
        Log.d(TAG, "asker: " + asker);
        Log.d(TAG, "loc: " + loc);

        // if extra == whereru then start updating loc and copy iOS [self postImhere:asker];

        if (extra.equals("whereru")){
            Log.d("SCXTT", "silent push received");
            startBackgroundLocationService(asker);

        } else {
            sendNotification(message);
            updateMyActivity(this, "HEY GO GET ROOM MESSGES DUDE - only do this for messages");
        }


        // [START_EXCLUDE]
        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */
//        sendNotification(message);
        // [END_EXCLUDE]
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     */
    private void startBackgroundLocationService(String asker) {
        Log.d("SCXTT", "GCM -> startBackgroundLocationService to BackgroundLocationService.class");
        startService(new Intent(this, BackgroundLocationService.class));
    }

    private void sendNotification(String message) {
        Log.d("SCXTT", "sendNotification to ShowMapActivity.class");
        Intent intent = new Intent(this, ShowMapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        if (deviceSingleton.isMapIsActive()) {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("WhereRU")
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
        } else {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("WhereRU")
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent);
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
        }

    }

    static void updateMyActivity(Context context, String message) {

        Intent intent = new Intent("com.harnk.whereru.gcm");

        //put whatever data you want to send, if any
        intent.putExtra("message", message);

        //send broadcast
        Log.d("SCXTT", "SENDING context.sendBroadcast(intent)");
        context.sendBroadcast(intent);
    }
}
