package com.example.jokubas.restauranthygienechecker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
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
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jokubas.restauranthygienechecker.JSON_classes.Establishments;
import com.example.jokubas.restauranthygienechecker.JSON_classes.Response;
import com.example.jokubas.restauranthygienechecker.util.QueryData;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import cz.msebera.android.httpclient.Header;

/**
 * The Main activity class.
 */
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private static final String PAGE_NUMBER = "pageNumber=";
    private static final String DISTANCE = "distance";
    private static final String RATING = "rating";
    private static final double LONDON_LONGITUDE = -0.141099;
    private static final double LONDON_LATITUDE = 51.515419;
    private static String LAST_QUERY;
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
    private boolean isMapOn;
    private boolean isSearchLocalBased;
    private boolean loadingFlag;
    private boolean isSearchSimple;
    private int lastPageSize;
    private int lastPageSizeSimpleSearch;
    private boolean wasSimpleExecuted;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.map:
                    findViewById(R.id.chef).setVisibility(View.GONE);
                    switchHygieneAndDateSortButton(false);
                    switchLocationSortButton(false);
                    showMapFragment();
                    isMapOn = true;
                    changeBackgroundImageBasedOnView();
                    return true;
                case R.id.list:
                    if (establishments != null && establishments.size() > 0) {
                        switchHygieneAndDateSortButton(true);
                        switchLocationSortButton(true);
                    }
                    hideMapFragment();
                    isMapOn = false;
                    changeBackgroundImageBasedOnView();
                    return true;
            }
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // setup the default flags
        isMapOn = false;
        isSearchLocalBased = true;
        loadingFlag = false;
        isSearchSimple = false;
        wasSimpleExecuted = false;

        // get the intent data and the possible data fetched from the advanced search activity
        Intent intent = getIntent();
        QueryData queryData = (QueryData) intent.getSerializableExtra(SearchQueries.QUERY_DATA);

        // change the default UI visibility
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        // initialise the list view adapter
        initListViewAdapter();

        // set up simple search listener
        setSimpleSearchListener();

        // set up the listener for the bottom navigation bar
        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // set location listener to constantly update the location
        setLocationListener();

        // request the required permissions from the user
        checkAndRequestPermissions();
        requestUpdate();

        // init the map fragment so that google maps could be displayed
        map = null;
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView);
        mapFragment.getMapAsync(this);

        // if the data from the intent was fetched then query the API
        if (queryData != null) {
            queryAdvancedSearch(queryData);
        }

        // remaining default setup
        switchLoadingGif();
        switchHygieneAndDateSortButton(false);
        switchLocationSortButton(false);
        setOnScrollListener();

        setAutocompleteTextInput();
    }

    /**
     * Initialise the adapter for the establishments list.
     */
    private void initListViewAdapter() {

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
                if (inflater == null) return;
                // setting appropriate text fields based on establishment data
                View layout = inflater.inflate(R.layout.pop_up_new, null);
                ((TextView) layout.findViewById(R.id.name)).setText(e.BusinessName);
                ((TextView) layout.findViewById(R.id.type)).setText(e.BusinessType);

                StringBuilder address = new StringBuilder();
                if (!e.AddressLine1.equals("")) address.append(e.AddressLine1);
                if (!e.AddressLine2.equals("")) {
                    if(!address.toString().equals(""))address.append(", ");
                    address.append(e.AddressLine2);
                }
                if (!e.AddressLine3.equals("")){
                    if(!address.toString().equals(""))address.append(", ");
                    address.append(e.AddressLine3);
                }
                if (!e.AddressLine4.equals("")){
                    if(!address.toString().equals(""))address.append(", ");
                    address.append(e.AddressLine4);
                }
                ((TextView) layout.findViewById(R.id.address)).setText(address.toString());
                ((TextView) layout.findViewById(R.id.authority)).setText(e.LocalAuthorityName);
                ((TextView) layout.findViewById(R.id.authority_email)).setText(e.LocalAuthorityEmailAddress);

                // Ratings image is set based on the rating value of the establishment
                ImageView rating = layout.findViewById(R.id.rating);
                switch (e.RatingValue) {
                    case "0":
                        rating.setImageResource(R.drawable.score_0);
                        break;
                    case "1":
                        rating.setImageResource(R.drawable.score_1);
                        break;
                    case "2":
                        rating.setImageResource(R.drawable.score_2);
                        break;
                    case "3":
                        rating.setImageResource(R.drawable.score_3);
                        break;
                    case "4":
                        rating.setImageResource(R.drawable.score_4);
                        break;
                    case "5":
                        rating.setImageResource(R.drawable.score_5);
                        break;
                    default:
                        rating.setImageResource(R.drawable.score_no);
                        break;
                }

                // Get the devices screen density to calculate correct pixel sizes
                float density = MainActivity.this.getResources().getDisplayMetrics().density;

                // Create a focusable PopupWindow with the given layout and correct size
                final PopupWindow pw = new PopupWindow(layout, (int) density * 500, (int) density * 480, true);

                // Button to close the pop-up
                layout.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
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
                pw.showAtLocation(layout, Gravity.CENTER | Gravity.TOP, 0, 400);

            }
        };

        // set up on click listener for the list view
        establView.setOnItemClickListener(itemClickListener);
    }

    /**
     * Method for setting the autocomplete text view based on the values from
     * the provided file which contains a list of common establishments and
     * locations.
     */
    private void setAutocompleteTextInput() {

        // initialise the establishment
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(getAssets().open("suggestions.txt")));
        } catch (Exception e) {
            // failed to read file, abort this action
            return;
        }

        // read a file and add all the elements to the list of suggestions
        List<String> list = new LinkedList<>();
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                list.add(line);
            }
        } catch (IOException e) {
            // do nothing, continue with the list we currently have
        }

        // set up the adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, list);
        AutoCompleteTextView textView = findViewById(R.id.searchView);
        textView.setAdapter(adapter);

    }


    /**
     * Sets the listener for simple search view so that when the
     * search key in the keyboard layout is pressed this would be executed.
     */
    private void setSimpleSearchListener() {
        searchBarView = findViewById(R.id.searchView);
        searchBarView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    onSimpleSearchClick();
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Checks the location permissions.
     * If the permissions are not granted then dialog window is displayed.
     */
    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                requestLocPerms();
            }
        } else {
            attachLocManager();
        }
    }


    /**
     * Set the location listener to update the longitude and latitude
     */
    private void setLocationListener() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                latitude = location.getLatitude();
                longitude = location.getLongitude();
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
    }

    /**
     * Method called when the result from advanced search activity is returned
     * <p>
     * There are two types of advanced queries: location based and region-authority based.
     *
     * @param qData the data used for querying the API
     */
    private void queryAdvancedSearch(QueryData qData) {
        if (qData.useLocation) {
            isSearchLocalBased = true;
            readUrl(String.format(Locale.ENGLISH, SearchQueries.ADVANCED_SEARCH_RADIUS_URL,
                    qData.name, qData.maxDistanceLimit, qData.businessTypeId,
                    qData.ratingKey, longitude, latitude, DISTANCE, SearchQueries.DEFAULT_PAGE_SIZE, lastPageSize++));

        } else {
            isSearchLocalBased = false;
            readUrl(String.format(Locale.ENGLISH, SearchQueries.ADVANCED_SEARCH_URL,
                    qData.name, qData.businessTypeId, qData.ratingKey,
                    qData.localAuthorityId, RATING, SearchQueries.DEFAULT_PAGE_SIZE, lastPageSize++));
        }
    }

    /**
     * On simple search click.
     * <p>
     * Method is called when user clicks Simple search button
     */
    public void onSimpleSearchClick() {
        // record the initial page size
        lastPageSize = 1;
        lastPageSizeSimpleSearch = 1;

        // search is not location based
        isSearchLocalBased = false;
        isSearchSimple = true;
        wasSimpleExecuted = false;

        // enable none of the sort options before data is loaded
        setButtonPressed(SortOptions.NONE);

        // hide keyboard when button is pressed
        hideSoftKeyboard();

        // get the text(address) from the input view
        String address = searchBarView.getText().toString();

        // read URL and update the list of establishments accordingly
        readUrl(String.format(Locale.ENGLISH, SearchQueries.SIMPLE_SEARCH_NAME_URL, address, lastPageSize++));
    }

    /**
     * On local search click.
     * <p>
     * Executed when user presses the local search button.
     *
     * @param view the view
     */
    public void onLocalSearchClick(View view) {

        // if GPS is disabled abort with error message
        if (!checkGpsStatus()) {
            errorToast(R.string.no_gps);
            return;
        }

        // record the initial page size
        lastPageSize = 1;
        requestUpdate();

        // search is location based
        isSearchLocalBased = true;
        isSearchSimple = false;
        wasSimpleExecuted = false;

        setButtonPressed(SortOptions.NONE);

        // query the API based on location
        readUrl(String.format(Locale.ENGLISH, SearchQueries.LOCAL_SEARCH_URL,
                longitude, latitude, lastPageSize++));
    }

    /**
     * On advanced search click.
     * <p>
     * Executed when user presses the advanced search button
     *
     * @param view the view
     */
    public void onAdvancedSearchClick(View view) {
        // update the configs
        lastPageSize = 1;
        setButtonPressed(SortOptions.NONE);
        isSearchLocalBased = false;
        isSearchSimple = false;
        wasSimpleExecuted = false;

        // start a new intent
        startActivity(new Intent(MainActivity.this, AdvancedSearchActivity.class));
    }

    /**
     * Query the actual API endpoint based on the url query provided.
     *
     * @param urlString the URL string used for querying the API.
     */
    private void readUrl(String urlString) {
        // check if internet connection is turned on
        if (!isNetworkAvailable()) {
            errorToast(R.string.network_off);
            return;
        }


        LAST_QUERY = urlString;
        // turn on the loading gif
        switchLoadingGif();

        // set up the HTTP client and all the required headers
        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("x-api-version", "2");
        client.addHeader("Accept", "application/json");

        // query the API and wait for the results
        client.get(urlString,
                new TextHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String response) {
                        // turn off the gif
                        switchLoadingGif();

                        // parse the response and update the list of values
                        Gson gson = new Gson();
                        Response result = gson.fromJson(response, Response.class);
                        populateList(result);
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String response, Throwable t) {
                        // turn of the gif
                        switchLoadingGif();
                        errorToast(R.string.failed_query);
                    }
                });
    }

    /**
     * Populate a establishments list with the new data fetched from the API.
     *
     * @param response the response from the API call to be added.
     */
    private void populateList(Response response) {
        // if the data is new, clear previous results
        if (lastPageSize <= 2)
            establishments.clear();

        // add new results to the list
        establishments.addAll(response.establishments);

        if(isSearchSimple && establishments.size() != SearchQueries.DEFAULT_PAGE_SIZE &&
                lastPageSize + lastPageSizeSimpleSearch < 10 && !wasSimpleExecuted){
            Log.e("err","err");
            wasSimpleExecuted = true;
            // read URL and update the list of establishments accordingly
            readUrl(String.format(Locale.ENGLISH, SearchQueries.SIMPLE_SEARCH_ADDRESS_URL,
                    searchBarView.getText().toString(), lastPageSizeSimpleSearch++));
            lastPageSize = lastPageSizeSimpleSearch;
        }
        // remove duplicates from the list in case some exists
        Set<Establishments> hs = new HashSet<>(establishments);
        establishments.clear();
        establishments.addAll(hs);

        estAdpt.notifyDataSetChanged();

        // change the background image
        changeBackgroundImageBasedOnView();

        // based on the current view enable or disable sort buttons
        if (!isMapOn) {
            switchLocationSortButton(true);
            switchHygieneAndDateSortButton(true);
        }
        if (establishments.size() == 0) {
            errorToast(R.string.no_results);
            switchHygieneAndDateSortButton(false);
            switchLocationSortButton(false);
        }

        // update the map
        onMapReady(map);
        loadingFlag = false;
        setButtonPressed(SortOptions.NONE);

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == FINE_LOCATION_PERMISSION) {
            if (permissions.length == 1 &&
                    permissions[0].equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                attachLocManager();
            } else {
                errorToast(R.string.no_permission);
            }
        }
    }

    /**
     * Attach location manager.
     */
    public void attachLocManager() {
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    0, 0, locationListener);
        } catch (SecurityException err) {
        }
    }

    /**
     * Request loc perms.
     */
    public void requestLocPerms() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_PERMISSION);
    }


    /**
     * Request the location update to update our coordinates in the world.
     */
    private void requestUpdate() {
        // check if the permission is granted for the app
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // get the network coordinates first because it requires less time to update
            if (locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Location loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) {
                    latitude = loc.getLatitude();
                    longitude = loc.getLongitude();
                }
            }

            // get the GPS coordinates
            if (locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                // check if coordinates are not 0,0 because then it may indicate that
                // coordinates are not updated so keep the network ones
                if (loc != null && Math.abs(loc.getLatitude() - loc.getLongitude()) > 0.1) {
                    latitude = loc.getLatitude();
                    longitude = loc.getLongitude();
                }
            }
        }

    }

    /**
     * show the map fragment
     */
    private void showMapFragment() {
        onMapReady(map);
        try {
            FragmentTransaction ft = mapFragment.getFragmentManager().beginTransaction();
            ft.setCustomAnimations(android.R.animator.fade_in,
                    android.R.animator.fade_out);
            ft.show(mapFragment);
            ft.commit();
        } catch (Exception e) {
        }
    }

    /**
     * hide the map fragment
     */
    private void hideMapFragment() {
        try {
            FragmentTransaction ft = mapFragment.getFragmentManager().beginTransaction();
            ft.hide(mapFragment);
            ft.commit();
        } catch (Exception e) {
        }
    }

    /**
     * The main method controlling the google map
     *
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // if map is null it means that onMapReady was not called
        if (map == null) hideMapFragment();

        // initialise map
        map = googleMap;

        //clear all the items from map
        map.clear();

        // init builder
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        // start marker options and add the current location
        MarkerOptions myPosition = new MarkerOptions();
        if (checkGpsStatus() && Math.abs(longitude - latitude) > 0.1) {
            myPosition.position(new LatLng(latitude, longitude)).title("Your Current Location").
                    icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE));
            googleMap.addMarker(myPosition);
            builder.include(myPosition.getPosition());
        }

        // add all the establishments to the map
        for (Establishments e : establishments) {
            if (e.geocode.longitude == null || e.geocode.latitude == null) continue;
            MarkerOptions option = new MarkerOptions();
            googleMap.addMarker(option.position(new LatLng(Double.valueOf(e.geocode.latitude), Double.valueOf(e.geocode.longitude))).
                    title(e.BusinessName).
                    icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA)));
            builder.include(option.getPosition());
        }

        // update the camera based on the situation
        CameraUpdate camUpd;
        if (establishments.size() == 0 && (!checkGpsStatus() || Math.abs(longitude - latitude) < 0.1)) {
            camUpd = CameraUpdateFactory.newLatLngZoom(
                    new MarkerOptions().position(new LatLng(LONDON_LATITUDE, LONDON_LONGITUDE)).getPosition(), 10F);
        } else if (establishments.size() == 0) {
            camUpd = CameraUpdateFactory.newLatLngZoom(
                    new MarkerOptions().position(new LatLng(latitude, longitude)).getPosition(), 10F);
        } else {
            LatLngBounds bounds = builder.build();
            camUpd = CameraUpdateFactory.newLatLngBounds(bounds, 0);
        }

        // animate the camera zoom in and set specific parameters
        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(true);
        map.setPadding(80, 250, 80, 350);
        map.animateCamera(camUpd);
        map.setInfoWindowAdapter(new MapInfoAdapter(MainActivity.this, establishments, map));
        map.setOnMarkerClickListener(this);
    }


    /**
     * Hides the soft keyboard when executed
     */
    public void hideSoftKeyboard() {
        if (getCurrentFocus() != null) {
            InputMethodManager inputManager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (inputManager != null) {
                inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }


    /**
     * Called when the user clicks a marker on the google map.
     */
    @Override
    public boolean onMarkerClick(final Marker marker) {
        marker.showInfoWindow();

        // Return true to indicate that we have consumed the event and that we don't want
        // for the default behavior to occur.
        return true;
    }

    /**
     * Turn on/off the loading gif image when result is on its way
     */
    private void switchLoadingGif() {
        pl.droidsonroids.gif.GifImageView world = findViewById(R.id.world);
        world.setVisibility(Math.abs(world.getVisibility() - View.GONE));
    }

    /**
     * Turn off/on the hygiene and date sort buttons
     *
     * @param on -true if button should be turned on and false otherwise
     */
    private void switchHygieneAndDateSortButton(boolean on) {
        Button hygiene = findViewById(R.id.hygiene_sort);
        Button date = findViewById(R.id.date_sort);
        TextView sortByText = findViewById(R.id.sort_by_text);

        if (on) {
            hygiene.setVisibility(View.VISIBLE);
            date.setVisibility(View.VISIBLE);
            sortByText.setVisibility(View.VISIBLE);
        } else {
            hygiene.setVisibility(View.GONE);
            date.setVisibility(View.GONE);
            sortByText.setVisibility(View.GONE);
        }
    }

    /**
     * Turn off/on the location sort button
     *
     * @param on -true if button should be turned on and false otherwise
     */
    private void switchLocationSortButton(boolean on) {
        Button location = findViewById(R.id.location_sort);
        if (on && isSearchLocalBased)
            location.setVisibility(View.VISIBLE);
        else location.setVisibility(View.GONE);
    }

    /**
     * On hygiene sort click.
     * <p>
     * Executed when user clicks the button to sort the view based on the hygiene rating.
     *
     * @param view the view
     */
    public void onHygieneSortClick(View view) {
        // perform the sort based on the hygiene rating
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
                return -1;
            }
        });
        setButtonPressed(SortOptions.HYGIENE);
        estAdpt.notifyDataSetChanged();
    }

    /**
     * On location sort click.
     * <p>
     * Executed when user clicks the button to sort the view based on the location.
     *
     * @param view the view
     */
    public void onLocationSortClick(View view) {
        // perform the sort based on the location
        Collections.sort(establishments, new Comparator<Establishments>() {
            @Override
            public int compare(Establishments e1, Establishments e2) {
                return (e1.Distance > e2.Distance) ? 1 : ((e1.Distance == e2.Distance) ? 0 : -1);
            }
        });

        setButtonPressed(SortOptions.LOCATION);
        estAdpt.notifyDataSetChanged();
    }

    /**
     * On date sort click.
     * <p>
     * Executed when user clicks the button to sort the view based on the date.
     *
     * @param view the view
     */
    public void onDateSortClick(View view) {
        // perform the sort based on the date
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
                return date2.compareTo(date1);
            }
        });

        // set the button pressed
        setButtonPressed(SortOptions.DATE);
        estAdpt.notifyDataSetChanged();
    }

    /**
     * Checking whether string is an int or not.
     *
     * @param value the string value to be checked.
     * @return true if string is an int, false otherwise.
     */
    private boolean checkStringToInt(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Method for displaying the specific toast message based on
     * the string id passed as a parameter.
     *
     * @param errorMessage the id of the string to be displayed in the message.
     */
    private void errorToast(int errorMessage) {
        // create toast and set the specific settings
        Toast toast = new Toast(getBaseContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);

        // get the inflater and set the custom layout for the toast
        LayoutInflater inflater = (LayoutInflater) getBaseContext().
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater != null ? inflater.inflate(R.layout.toast, null) : null;
        if (view != null)
            ((TextView) view.findViewById(R.id.error_message)).setText(errorMessage);
        toast.setView(view);
        toast.show();
    }

    /**
     * Enabling or disabling the background view based on the view.
     * If the slider is on list view and number of establishments is 0
     * then enable it, otherwise disable the background view.
     */
    private void changeBackgroundImageBasedOnView() {
        if (!isMapOn && establishments.size() == 0) {
            findViewById(R.id.chef).setVisibility(View.VISIBLE);
        } else findViewById(R.id.chef).setVisibility(View.GONE);
    }

    /**
     * Checking whether the network is available for the application.
     *
     * @return true if internet is on, false otherwise.
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = null;
        if (connectivityManager != null) {
            activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        }
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Method for checking whether the app is granted the GPS and it is currently turned on.
     *
     * @return true if GPS set and false otherwise.
     */
    private boolean checkGpsStatus() {
        return (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) &&
                ((LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Method for updating the pressed button and disabling all the others.
     *
     * @param opt the option of the button to be set as pressed.
     */
    private void setButtonPressed(SortOptions opt) {
        findViewById(R.id.hygiene_sort).setBackgroundResource(R.drawable.button_sort);
        findViewById(R.id.date_sort).setBackgroundResource(R.drawable.button_sort);
        findViewById(R.id.location_sort).setBackgroundResource(R.drawable.button_sort);
        switch (opt) {
            case DATE:
                findViewById(R.id.date_sort).setBackgroundResource(R.drawable.button_sort_2);
                break;
            case HYGIENE:
                findViewById(R.id.hygiene_sort).setBackgroundResource(R.drawable.button_sort_2);
                break;
            case LOCATION:
                findViewById(R.id.location_sort).setBackgroundResource(R.drawable.button_sort_2);
                break;
            case NONE:
            default:
                break;
        }
    }

    /**
     * Method for loading additional items when user gets to the end of the current list.
     * The max number of loads is 10 not to overfill memory of the device.
     */
    private void loadAdditionalItems() {
        // Make sure that limit is not exceeded and there are more results to fetch.
        if (establishments.size() % SearchQueries.DEFAULT_PAGE_SIZE != 0 ||
                establishments.size() < SearchQueries.DEFAULT_PAGE_SIZE ||
                lastPageSize > 7) return;

        // update the last query with page size incremented
        LAST_QUERY = LAST_QUERY.substring(0, LAST_QUERY.lastIndexOf(PAGE_NUMBER));
        LAST_QUERY += PAGE_NUMBER + (lastPageSize++);
        readUrl(LAST_QUERY);

    }

    /**
     * Sets on scroll listener for the main list view with all the establishments.
     * <p>
     * This method is necessary in order to load more results when user gets to the end.
     */
    private void setOnScrollListener() {
        ((ListView) findViewById(R.id.establishments)).setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
                // if we are at the end and flag is correctly set, load additional
                if (i + i1 == i2 && i2 != 0) {
                    if (!loadingFlag) {
                        loadingFlag = true;
                        loadAdditionalItems();
                    }
                }

            }
        });
    }

    /**
     * The enum Sort options.
     */
    enum SortOptions {
        HYGIENE,
        DATE,
        LOCATION,
        NONE
    }


}

