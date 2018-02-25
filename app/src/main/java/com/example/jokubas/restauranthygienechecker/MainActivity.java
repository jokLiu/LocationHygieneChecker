package com.example.jokubas.restauranthygienechecker;

import android.Manifest;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    // longitude and latitude has to be inserted
    private static final String LOCAL_SEARCH_URL = "http://api.ratings.food.gov.uk/Establishments?longitude=%f&latitude=%f&sortOptionKey=distance&pageSize=15";
    private static final String SIMPLE_SEARCH_URL = "http://api.ratings.food.gov.uk/Establishments?address=%s&pageSize=10&pageNumber=%d";
    private static final String ADVANCED_SEARCH_URL = "http://api.ratings.food.gov.uk/Establishments?address=%s&pageSize=10&pageNumber=%d";
    private final int FINE_LOCATION_PERMISSION = 1;
    String json = "";
    private ArrayList<Establishments> establishments = new ArrayList<>();
    private double longitude;
    private double latitude;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private ArrayAdapter estAdpt;
    private SupportMapFragment mapFragment;
    private GoogleMap map;
    private EditText searchBarView;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.map:
                    showMapFragment();
                    return true;
                case R.id.list:
                    hideMapFragment();
                    return true;
            }
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
//                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
////
//        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
//        getWindow().setStatusBarColor(getResources().getColor(android.R.color.transparent));

        // adapter for the list view of the returned responses
        estAdpt = new ArrayAdapter(this, android.R.layout.simple_selectable_list_item, establishments);
        final ListView establView = findViewById(R.id.establishments);
        establView.setAdapter(estAdpt);
        final AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Establishments e = (Establishments) estAdpt.getItem(position);
                //We need to get the instance of the LayoutInflater, use the context of this activity
                LayoutInflater inflater = (LayoutInflater) MainActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                //Inflate the view from a predefined XML layout (no need for root id, using entire layout)
                View layout = inflater.inflate(R.layout.pop_up_establishment_window, null);

                ((TextView) layout.findViewById(R.id.name)).setText(e.BusinessName);
                ((TextView) layout.findViewById(R.id.type)).setText(e.BusinessType);
                ((TextView) layout.findViewById(R.id.address)).setText(e.AddressLine1);
                ((TextView) layout.findViewById(R.id.authority)).setText(e.LocalAuthorityName);
                ((TextView) layout.findViewById(R.id.authority_email)).setText(e.LocalAuthorityEmailAddress);
                ((TextView) layout.findViewById(R.id.rating)).setText(e.RatingValue);

                //Get the devices screen density to calculate correct pixel sizes
                float density = MainActivity.this.getResources().getDisplayMetrics().density;
                // create a focusable PopupWindow with the given layout and correct size
                final PopupWindow pw = new PopupWindow(layout, (int) density * 400, (int) density * 400, true);
                //Button to close the pop-up
                ((Button) layout.findViewById(R.id.close)).setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        pw.dismiss();
                    }
                });
                //Set up touch closing outside of pop-up
                pw.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                pw.setTouchInterceptor(new View.OnTouchListener() {
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {
                            pw.dismiss();
                            return true;
                        }
                        return false;
                    }
                });
                pw.setOutsideTouchable(true);
                // display the pop-up in the center
                pw.showAtLocation(layout, Gravity.CENTER, 0, 0);
            }
        };


        establView.setOnItemClickListener(itemClickListener);

        searchBarView = findViewById(R.id.searchView);
        searchBarView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    onSimpleSearcClick();
                    return true;
                }
                return false;
            }
        });


        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
                Log.e("printed", String.valueOf(latitude));
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
            }

            @Override
            public void onProviderDisabled(String s) {
            }
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setMessage("The application is about to request access to your location.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                requestLocPerms();
                            }
                        })
                        .create()
                        .show();
            } else {
                requestLocPerms();
            }
        } else {
            attachLocManager();
        }
        requestUpdate();


        mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this);
        hideMapFragment();
    }


    public void onSimpleSearcClick() {
        hideSoftKeyboard();
        String address = searchBarView.getText().toString();
        Log.v("Print", String.format(SIMPLE_SEARCH_URL, address, 1));
        try {
            readUrl(String.format(SIMPLE_SEARCH_URL, address, 1));
        } catch (Exception e) {
//            Log.e("Exception", e.getMessage());
            // TODO handle error state or no response being returned
        }

    }

    public void onLocalSearchClick(View view) {
        requestUpdate();
        Log.v("Print", String.format(LOCAL_SEARCH_URL, longitude, latitude));
        try {
            readUrl(String.format(LOCAL_SEARCH_URL, longitude, latitude));
        } catch (Exception e) {
//            Log.e("Exception", e.getMessage());
            // TODO handle error state or no response being returned
        }
    }

    public void onAdvancedSearchClieck(View view){
//        hideMapFragment();
        Intent intent = new Intent(MainActivity.this, AdvancedSearchActivity.class);
        startActivity(intent);


////         Create new fragment and transaction
//        Fragment newFragment = new AdvancedSearchFragment();
//        FragmentTransaction transaction = mapFragment.getFragmentManager().beginTransaction();
//
//// Replace whatever is in the fragment_container view with this fragment,
//// and add the transaction to the back stack if needed
//        transaction.replace(mapFragment.getId(), newFragment);
//        transaction.addToBackStack(null);
//
//        FragmentTransaction ft = mapFragment.getFragmentManager().beginTransaction();
//        ft.show(mapFragment);
//        ft.commit();
//
//// Commit the transaction
//        transaction.commit();
    }


    private void readUrl(String urlString) throws Exception {

        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("x-api-version", "2");
        client.addHeader("Accept", "application/json");
        client.get(urlString,
                new TextHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String response) {
                        Log.v("Response", response);
                        Gson gson = new Gson();
                        Response result = gson.fromJson(response, Response.class);
                        populateList(result);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String response, Throwable t) {
                        // TODO figure out later
//                        Log.v("JSON",response);
                    }
                });
    }

    private void populateList(Response response) {
        establishments.clear();
        establishments.addAll(response.establishments);
        estAdpt.notifyDataSetChanged();
        onMapReady(map);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case FINE_LOCATION_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    attachLocManager();
                } else {
                }
                return;
            }
        }
    }

    public void attachLocManager() {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        } catch (SecurityException err) {
        }
    }

    public void requestLocPerms() {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_PERMISSION);
    }


    private void requestUpdate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Location loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) {
                    latitude = loc.getLatitude();
                    longitude = loc.getLongitude();
                }
            }

            if (locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc != null) {
                    latitude = loc.getLatitude();
                    longitude = loc.getLongitude();
                }
            }


        }

    }

    private void showMapFragment() {
        onMapReady(map);
        try {
            FragmentTransaction ft = mapFragment.getFragmentManager().beginTransaction();
            ft.setCustomAnimations(android.R.animator.fade_in,
                    android.R.animator.fade_out);
            ft.show(mapFragment);
            ft.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideMapFragment() {
        try {
            FragmentTransaction ft = mapFragment.getFragmentManager().beginTransaction();
            ft.hide(mapFragment);
            ft.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        googleMap.clear();
        googleMap.addMarker(new MarkerOptions().position(new LatLng(latitude, longitude)).title("your new lcoation").snippet("and snippet").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
        for (Establishments e : establishments) {
            googleMap.addMarker(new MarkerOptions().position(new LatLng(Double.valueOf(e.geocode.latitude), Double.valueOf(e.geocode.longitude))).
                    title(e.BusinessName).
                    snippet(e.BusinessType + "\n" +
                            e.AddressLine1 + "\n" +
                            e.LocalAuthorityName + "\n" +
                            e.LocalAuthorityEmailAddress + "\n" +
                            e.RatingValue
                    ).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));
        }

        googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));

        // Showing the current location in Google Map
        CameraPosition camPos = new CameraPosition.Builder()
                .target(new LatLng(latitude, longitude))
                .zoom(15)
                .tilt(70)
                .build();

        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(camPos));
    }



    /**
     * Hides the soft keyboard
     */
    public void hideSoftKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager inputManager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

}

