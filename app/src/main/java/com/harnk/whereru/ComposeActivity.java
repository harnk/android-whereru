package com.harnk.whereru;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

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
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}

