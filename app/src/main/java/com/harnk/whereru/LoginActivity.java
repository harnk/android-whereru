package com.harnk.whereru;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;

/**
 * Created by scottnull on 11/22/15.
 */
public class LoginActivity extends AppCompatActivity {

    EditText nicknameTextField, secretCodeTextField;
    private static final String TAG = "SCXTT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();

        nicknameTextField = (EditText)findViewById(R.id.nickName);
        nicknameTextField.setText(deviceSingleton.getNickname());
        secretCodeTextField = (EditText)findViewById(R.id.mapGroup);
        secretCodeTextField.setText(deviceSingleton.getSecretCode());

    }
    private void showErrorAlert(String text) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(text);
//            alertDialog.setMessage("Fill in additional message");
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // do something dummy
            }
        });
        alertDialog.show();
    }

    private void userDidJoin() {
        Log.d("SCXTT", "userDidJoin");
        //set values to singleton and sharedpreferences
        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        deviceSingleton.setImInARoom(true);
        deviceSingleton.setJoinedChat(true);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("savedJoinedChat", true);
        editor.commit();
        //Dismiss the login screen
        this.finish();
        //Set a notification to the app that userJoinedRoom which is in iOS but I dont think iOS is even using it

    }

    private void postJoinRequest(){

        Log.d(TAG, "postJoinRequest");
        // Throw up a HUD spinner saying "Connecting ..."

        // Do API call cmd = join
        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        String deviceLocation = deviceSingleton.getMyLocStr();
        if (deviceLocation.length() == 0) {
            deviceLocation = "40.689124, -74.044611"; //statue of liberty
        }
        String deviceToken = deviceSingleton.getGcmToken();
        if (deviceToken.length() == 0) {
            deviceToken = "7e59c0d7852e87e594a075d7a81c90b13c076637851f36c6bb32dd1e4e62a639"; // fake apple token
        }
        AsyncHttpClient client2 = new AsyncHttpClient();
        RequestParams params2 = new RequestParams();

        params2.put("cmd", "join");
        params2.put("user_id", deviceSingleton.getUserId());
        params2.put("token", deviceSingleton.getGcmToken());
        params2.put("name", deviceSingleton.getNickname());
        params2.put("location", deviceLocation);
        params2.put("code", secretCodeTextField.getText());

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
                userDidJoin();
            }
            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.d(TAG, "postJoinRequest API call onFailure = " + errorResponse.toString() + " e: " + e.toString() + " statusCode: " + statusCode);
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }
            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });
        Log.d(TAG, "API call response out of catch");

    }


    public void doStartPressed(View view){
        nicknameTextField = (EditText)findViewById(R.id.nickName);
        secretCodeTextField = (EditText)findViewById(R.id.mapGroup);
        //if either field is zero length throw up alert
        if (nicknameTextField.length()==0){
            showErrorAlert("Fill in your nickname");
            return;
        }
        if (secretCodeTextField.length()==0){
            showErrorAlert("Fill in a map group name");
            return;
        }

        //set values to singleton and sharedpreferences
        DeviceSingleton deviceSingleton = DeviceSingleton.getInstance();
        deviceSingleton.setNickname(nicknameTextField.getText().toString());
        deviceSingleton.setSecretCode(secretCodeTextField.getText().toString());
        Log.d("SCXTT", "doStart clicked in LoginActivity: " + deviceSingleton.getNickname() + " " + deviceSingleton.getSecretCode());


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("nickname", deviceSingleton.getNickname());
        editor.putString("secretCode", deviceSingleton.getSecretCode());
        editor.commit();

        String savedMapGroup = prefs.getString("secretCode", "");
        Log.d(TAG, "SAVED prefs secretCode: " + savedMapGroup + " nickname: " + deviceSingleton.getNickname() + " SCXTT WIP check what is saved for savedLocStr");

        // do postJoinRequest
        postJoinRequest();

    }

}
