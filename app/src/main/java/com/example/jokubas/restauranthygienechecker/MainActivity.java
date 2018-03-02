package com.example.jokubas.restauranthygienechecker;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jokubas.restauranthygienechecker.util.SearchQueries;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    // longitude and latitude has to be inserted
    private final int FINE_LOCATION_PERMISSION = 1;
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
                    switchHygieneAndDate(false);
                    switchLocation(false);
                    showMapFragment();
                    return true;
                case R.id.list:
                    if (establishments != null && establishments.size() > 0) {
                        switchHygieneAndDate(true);
                        switchLocation(true);
                    }
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


        Intent intent = getIntent();
        QueryData queryData = (QueryData) intent.getSerializableExtra("query_data");

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
                View layout = inflater.inflate(R.layout.pop_up_new, null);

                ((TextView) layout.findViewById(R.id.name)).setText(e.BusinessName);
                ((TextView) layout.findViewById(R.id.type)).setText(e.BusinessType);
                ((TextView) layout.findViewById(R.id.address)).setText(e.AddressLine1);
                ((TextView) layout.findViewById(R.id.authority)).setText(e.LocalAuthorityName);
                ((TextView) layout.findViewById(R.id.authority_email)).setText(e.LocalAuthorityEmailAddress);
                ((TextView) layout.findViewById(R.id.rating)).setText(e.RatingValue);

                //Get the devices screen density to calculate correct pixel sizes
                float density = MainActivity.this.getResources().getDisplayMetrics().density;
                // create a focusable PopupWindow with the given layout and correct size
                final PopupWindow pw = new PopupWindow(layout, (int) density * 500, (int) density * 450, true);
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


        if (queryData != null) { // use boolean from the other intent
            queryAdvancedSearch(queryData);
        }
        switchLoadingGif();
        switchHygieneAndDate(false);
        switchLocation(false);
    }

    private void queryAdvancedSearch(QueryData qData) {
        try {
            if (qData.useLocation) {
                readUrl(String.format(Locale.ENGLISH, SearchQueries.ADVANCED_SEARCH_RADIUS_URL,
                        qData.name, qData.maxDistanceLimit, qData.businessTypeId,
                        qData.ratingKey, "distance", 1, 20, longitude, latitude));

            } else {
                readUrl(String.format(Locale.ENGLISH, SearchQueries.ADVANCED_SEARCH_URL,
                        qData.name, qData.businessTypeId, qData.ratingKey,
                        qData.localAuthorityId, "rating", 1, 20));
            }
        } catch (Exception e) {
            // TODO handle exception
        }
    }

    public void onSimpleSearcClick() {
        hideSoftKeyboard();
        String address = searchBarView.getText().toString();
        try {
            readUrl(String.format(SearchQueries.SIMPLE_SEARCH_URL, address, 1));
        } catch (Exception e) {
            // TODO handle error state or no response being returned
        }

    }

    public void onLocalSearchClick(View view) {
        requestUpdate();
        try {
            readUrl(String.format(SearchQueries.LOCAL_SEARCH_URL, longitude, latitude));
        } catch (Exception e) {
            // TODO handle error state or no response being returned
        }
    }

    public void onAdvancedSearchClieck(View view) {
        Intent intent = new Intent(MainActivity.this, AdvancedSearchActivity.class);
        startActivity(intent);
    }


    private void readUrl(String urlString) throws Exception {
        switchLoadingGif();
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("x-api-version", "2");
        client.addHeader("Accept", "application/json");
        client.get(urlString,
                new TextHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String response) {
                        switchLoadingGif();
                        Log.v("Response", response);
                        Gson gson = new Gson();
                        Response result = gson.fromJson(response, Response.class);
                        populateList(result);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String response, Throwable t) {
                        switchLoadingGif();
                        Log.e("TOAST", "TPAST");
                        // TODO figure out later
//                        Log.v("JSON",response);
                    }
                });
    }

    private void populateList(Response response) {
        establishments.clear();
        establishments.addAll(response.establishments);
        estAdpt.notifyDataSetChanged();
        if (establishments.size() == 0) noResultsToast();
        switchLocation(true);
        switchHygieneAndDate(true);
        for(Establishments e : establishments)
            if(e.geocode.longitude ==null || e.geocode.latitude == null) {
//                readUrl(String.format(Locale.UK, SearchQueries.GEOCODE_POSTCODE_TO_LATLANG_URL, e.PostCode);
                Log.e("longs", String.valueOf(e.geocode.latitude) + String.valueOf(e.geocode.longitude));
            }
        onMapReady(map);




    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
//        switch (requestCode) {
//            case FINE_LOCATION_PERMISSION: {
//                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    attachLocManager();
//                } else {
//                }
//                return;
//            }
//        }
//
//
//    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == FINE_LOCATION_PERMISSION) {
            if (permissions.length == 1 &&
                    permissions[0] == Manifest.permission.ACCESS_FINE_LOCATION &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                map.setMyLocationEnabled(true);
                attachLocManager();
            } else {
                // Permission was denied. Display an error message.
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

    // TODO fix this method
    @Override
    public void onMapReady(GoogleMap googleMap) {

        map = googleMap;
        googleMap.clear();
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        MarkerOptions myPosition = new MarkerOptions();
        if (latitude != 0f && longitude != 0) {
            myPosition.position(new LatLng(latitude, longitude)).title("Your Current Location").
                    icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            googleMap.addMarker(myPosition);
            builder.include(myPosition.getPosition());
        }

        int i=0;
        // TODO Big Johns in simple search causes error
        for (Establishments e : establishments) {
            if(e.geocode.longitude ==null || e.geocode.latitude == null) continue;
            MarkerOptions option = new MarkerOptions();
            googleMap.addMarker(option.position(new LatLng(Double.valueOf(e.geocode.latitude), Double.valueOf(e.geocode.longitude))).
                    title(e.BusinessName).
                    snippet(e.BusinessType).
                    icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));
            builder.include(option.getPosition());
            Log.e("lol", String.valueOf(i++));
        }

        CameraUpdate camUpd = null;
        if (establishments.size() == 0 && myPosition.getPosition() == null)
            camUpd = CameraUpdateFactory.newLatLngZoom(
                    new MarkerOptions().position(new LatLng(51.5074, 0.1278)).getPosition(), 10F);
        else if (establishments.size() == 0) camUpd = CameraUpdateFactory.newLatLngZoom(
                new MarkerOptions().position(new LatLng(latitude, longitude)).getPosition(), 10F);
        else {
            int padding = 0;
            LatLngBounds bounds = builder.build();
            camUpd = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        }

        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(true);
        googleMap.setPadding(80, 250, 80, 350);
        try {
            googleMap.animateCamera(camUpd);
        } catch (Exception e) {
        }

        map.setInfoWindowAdapter(new MapInfoAdapter(MainActivity.this, establishments, map));
        map.setOnMarkerClickListener(this);
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


    /**
     * Called when the user clicks a marker.
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {
        marker.showInfoWindow();


        // Return true to indicate that we have consumed the event and that we don't want
        // for the default behavior to occur.
        return true;
    }

    private void switchLoadingGif() {
        pl.droidsonroids.gif.GifImageView world = findViewById(R.id.world);
        world.setVisibility(Math.abs(world.getVisibility() - View.GONE));
    }

    private void switchHygieneAndDate(boolean on) {
        Button hygiene = findViewById(R.id.hygiene_sort);
        Button date = findViewById(R.id.date_sort);

        if (on) {
            hygiene.setVisibility(View.VISIBLE);
            date.setVisibility(View.VISIBLE);
        } else {
            hygiene.setVisibility(View.GONE);
            date.setVisibility(View.GONE);
        }
    }


    private void switchLocation(boolean on) {
        Button location = findViewById(R.id.location_sort);
        if (on)
            location.setVisibility(View.VISIBLE);
        else location.setVisibility(View.GONE);
    }

    public void onHygieneSortClick(View view) {
        Collections.sort(establishments, new Comparator<Establishments>() {
            @Override
            public int compare(Establishments e1, Establishments e2) {
                String v1 = e1.RatingValue;
                String v2 = e2.RatingValue;
                if (checkStringToInt(v1) && checkStringToInt(v2))
                    return Integer.valueOf(e2.RatingValue) - Integer.valueOf(e1.RatingValue);
                else if (!checkStringToInt(v1) && !checkStringToInt(v2))
                    return 0;
                else if (!checkStringToInt(v1))
                    return 1;
                else return -1;
            }
        });
        estAdpt.notifyDataSetChanged();
    }

    // TODO check if this actually works
    public void onLocationSortClick(View view) {
        Collections.sort(establishments, new Comparator<Establishments>() {
            @Override
            public int compare(Establishments e1, Establishments e2) {
                return (e1.Distance > e2.Distance) ? 1 : ((e1.Distance == e2.Distance) ? 0 : -1);
            }
        });
        estAdpt.notifyDataSetChanged();
    }

    // TODO check if this actually works
    public void onDateSortClick(View view) {
        Collections.sort(establishments, new Comparator<Establishments>() {
            @Override
            public int compare(Establishments e1, Establishments e2) {

                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                Date date1 = null, date2 = null;
                try {
                    date1 = format.parse(e1.RatingDate);
                } catch (ParseException e) {
                    return -1;
                }
                try {
                    date2 = format.parse(e2.RatingDate);
                } catch (ParseException e) {
                    return 1;
                }
                return date1.compareTo(date2);
            }
        });
        estAdpt.notifyDataSetChanged();
    }

    private boolean checkStringToInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void noResultsToast() {
        Toast toast = new Toast(getBaseContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.toast, null);
        toast.setView(view);
        toast.show();
    }

}

