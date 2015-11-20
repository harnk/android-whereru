package com.harnk.whereru;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

public class ShowMapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private ListView messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_map);
        MapsInitializer.initialize(this);
        setUpMapIfNeeded();
        messageList = (ListView) findViewById(R.id.listView);
        ///////////////////////////////////////////
        // DUMMY DATA BELOW
        List<String> your_array_list = new ArrayList<String>();
        your_array_list.add("We are going to the lake");
        your_array_list.add("I want to go with you");
        your_array_list.add("Tonight you have to do homework. You know you can come with us silly fool!");
        your_array_list.add("I am wanting to see if this scrolls");
        your_array_list.add("We are going to the lake");
        your_array_list.add("I want to go with you");
        your_array_list.add("Tonight you have to do homework. You know you can come with us silly fool!");
        your_array_list.add("I am wanting to see if this scrolls");
        your_array_list.add("We are going to the lake");
        your_array_list.add("I want to go with you");
        your_array_list.add("Tonight you have to do homework. You know you can come with us silly fool!");
        your_array_list.add("I am wanting to see if this scrolls");

        // This is the array adapter, it takes the context of the activity as a
        // first parameter, the type of list view as a second parameter and your
        // array as a third parameter.
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_list_item_1,
                your_array_list );

        messageList.setAdapter(arrayAdapter);
///////////////////////////////////////////

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        if (!mNavigationDrawerFragment.isDrawerOpen()) {
//            // Only show items in the action bar relevant to this screen
//            // if the drawer is not showing. Otherwise, let the drawer
//            // decide what to show in the action bar.
//            getMenuInflater().inflate(R.menu.main, menu);
//
//            restoreActionBar();
//            return true;
//        }
//        getMenuInflater().inflate(R.layout.actionbar_menu, menu);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle("[room]");
        actionBar.setHomeButtonEnabled(true);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
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
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(41.739362, -86.099086), 16.0f));
            // Check if we were successful in obtaining the map.
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(41.739362, -86.098086))
                    .icon(bluePin)
                    .title("Hey Ho Lets Go"));
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(41.739062, -86.097586))
                    .icon(goldPin)
                    .title("Hey Ho Lets Go"));
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(41.738362, -86.097086))
                    .icon(pinkPin)
                    .title("Hey Ho Lets Go"));
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        map.addMarker(new MarkerOptions()
                .position(new LatLng(41.739362, -86.098086))
                .title("Hello world"));
        Log.v("SCXTT", "doing onMapReady callback is this working");
    }


    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(41.739362, -86.099086)).title("Marker"));
    }
}
