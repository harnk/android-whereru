package com.harnk.whereru;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.UnsupportedEncodingException;

import cz.msebera.android.httpclient.Header;

/**
 * Created by scottnull on 11/22/15.
 */
public class ComposeActivity extends AppCompatActivity{
    private static final String TAG = "SCXTT";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_compose, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_cancel:
                Log.d(TAG, "Cancel selected");
                this.finish();
                break;
            case R.id.action_send:
                Log.d(TAG, "Send selected");
                this.saveAction();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveAction() {
        // resign the keyboard
        // put up a hud spinner
        // get text entered
        EditText sendText = (EditText) findViewById(R.id.editText);
        String sendString = sendText.getText().toString();
        Log.d(TAG, "sendString: " + sendString);

        // do API call with cmd:message user_id: location: text:
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
        RequestParams params = new RequestParams();

        params.put("cmd", "message");
        params.put("user_id", deviceSingleton.getUserId());
        params.put("location", deviceLocation);
        params.put("text", sendString);

        Log.d(TAG, "params: " + params);

        client2.post(Constants.API_URL, params, new AsyncHttpResponseHandler() {
            @Override
            public void onStart() {
                // called before request is started
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                Log.d(TAG, "saveAction SUCCESS!");
                // called when response HTTP status is "200 OK"
                String decoded = null;
                try {
                    decoded = new String(response, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "API call onSuccess = " + statusCode + ", Headers: " + headers[0] + ", response.length: " + response.length +
                        ", decoded:" + response);
                userSentMessage();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                Log.d(TAG, "saveAction API call onFailure = " + errorResponse.toString() + " e: " + e.toString() + " statusCode: " + statusCode);
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });
        Log.d(TAG, "API saveAction call response out of catch");

    }

    private void userSentMessage() {
        this.finish();
    }
}

