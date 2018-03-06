package com.example.jokubas.restauranthygienechecker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jokubas.restauranthygienechecker.JSON_classes.AuthoritiesWrapper;
import com.example.jokubas.restauranthygienechecker.JSON_classes.BusinessTypes;
import com.example.jokubas.restauranthygienechecker.JSON_classes.RegionsWrapper;
import com.example.jokubas.restauranthygienechecker.util.QueryData;
import com.example.jokubas.restauranthygienechecker.util.QueryType;
import com.example.jokubas.restauranthygienechecker.util.SearchQueries;
import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

/**
 * The type Advanced search activity.
 */
public class AdvancedSearchActivity extends AppCompatActivity {


    private List<String> businessTypesSpinner = new ArrayList<>();
    private List<String> regionsSpinner = new ArrayList<>();
    private List<String> authoritiesSpinner = new ArrayList<>();

    private List<RegionsWrapper.Regions> regionsStorage;
    private List<BusinessTypes.businessTypes> businessTypesStorage;
    private List<AuthoritiesWrapper.Authorities> authoritiesStorage;
    private EditText businessNameView;
    private Spinner businessTypeView;
    private Spinner ratingHighView;
    private CheckBox checkBox;
    private Spinner radiusView;
    private Spinner regionView;
    private Spinner authoritiesView;
    private Button search;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.advanced_search);

        // initialise all the views for later usage
        businessNameView = findViewById(R.id.business_name);
        businessTypeView = findViewById(R.id.type_of_business);
        ratingHighView = findViewById(R.id.rating_high);
        checkBox = findViewById(R.id.current_loc_check_box);
        radiusView = findViewById(R.id.radius_spinner);
        regionView = findViewById(R.id.region_spinner);
        authoritiesView = findViewById(R.id.authority_spinner);
        search = findViewById(R.id.search);

        // query the main components needed for the initial view
        try {
            readUrl(SearchQueries.BUSINESS_TYPES_URL, QueryType.BUSINESS_TYPE);
            readUrl(SearchQueries.REGIONS_URL, QueryType.REGIONS);
            readUrl(SearchQueries.AUTHORITIES_URL, QueryType.AUTHORITIES);
        } catch (Exception ex) {
        }

        // when item from region view is selected, the authorities items have
        // to be displayed belonging to the particular region
        regionView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                addAuthorities();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        // depending on whether checkBox is checked or not
        // we enable or disable appropriate views
        manageCheckBox();

        setAutocompleteTextInput();
        setEditorListener();
    }

    /**
     * On click search.
     * Method is called when the user presses "search" button
     *
     * @param view the view
     */
    public void onClickSearch(View view) {
        // intent from the current activity back to the main activity
        Intent intent = new Intent(AdvancedSearchActivity.this, MainActivity.class);

        // Check whether the internet is turned on
        if (!isNetworkAvailable()) {
            errorToast(R.string.network_off);
            startActivity(intent);
            finish();
            return;
        }

        // Query data to be passed between different activities
        QueryData dataToPass = new QueryData();


        // set the business name to the data to be passed
        dataToPass.name = businessNameView.getText().toString();

        // set business type id, -1 indicates failure
        dataToPass.businessTypeId = "";
        int bTypeId = -1;
        String type = businessTypesSpinner.get(businessTypeView.getSelectedItemPosition());
        for (BusinessTypes.businessTypes b : businessTypesStorage)
            bTypeId = b.BusinessTypeName.equals(type) ?
                    b.BusinessTypeId : bTypeId;

        if(bTypeId  > 0)
            dataToPass.businessTypeId = String.valueOf(bTypeId);

        // set rating value
        dataToPass.ratingKey = ratingHighView.getSelectedItem().toString();

        // set the maximum distance from the users location
        dataToPass.maxDistanceLimit = Integer.valueOf(radiusView.getSelectedItem().toString());

        // authority id, -1 indicates failure
        dataToPass.localAuthorityId = -1;

        // if the checkBox is checked, it means that user is going to use
        // ones own location for finding the establishments
        if (checkBox.isChecked()) {

            // check whether the GPS status is actually enabled and we can use it
            // if it is not the case display error message and abort
            if (!checkGpsStatus()) {
                errorToast(R.string.not_enabled);
                return;
            }

            // otherwise set the flag to indicate that the current location
            // is going to be used for querying the API endpoint
            dataToPass.useLocation = true;

        }
        // if checkBox is not checked, it means that user is going to use
        // region and local authority from the spinners
        else {

            // check whether authority was selected, if not display
            // the error message and abort further actions
            int position = authoritiesView.getSelectedItemPosition();
            if (position < 0) {
                errorToast(R.string.no_authority);
                return;
            }

            // if authority is selected then fetch the details about it
            // and add it to the data to be passed back to the main activity
            String authName = authoritiesSpinner.get(position);
            for (AuthoritiesWrapper.Authorities a : authoritiesStorage) {
                if (a.Name.equals(authName)) {
                    dataToPass.localAuthorityId = a.LocalAuthorityId;
                    break;
                }
            }

            // set the flag to indicate that the authority and region
            // are going to be used for querying the API endpoint
            dataToPass.useLocation = false;

        }

        // put the data to be passed into the intent and start the activity
        intent.putExtra(SearchQueries.QUERY_DATA, dataToPass);
        startActivity(intent);
        finish();

    }

    private void setEditorListener(){
        ((AutoCompleteTextView)findViewById(R.id.business_name)).
                setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_NONE) {
                    hideSoftKeyboard();
                    return true;
                }

                return false;
            }

        });
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
        AutoCompleteTextView textView = findViewById(R.id.business_name);
        textView.setAdapter(adapter);

    }


    /**
     * On click check box.
     * <p>
     * Method is called when user clicks on check box and changes its state.
     *
     * @param view the view
     */
    public void onClickCheckBox(View view) {
        manageCheckBox();
    }

    /**
     * Based on the checkbox certain fields of the view
     * are disabled and others are enabled.
     * <p>
     * If checkBox is checked then region and authorities views are disabled.
     * Otherwise radius view is disabled.
     */
    private void manageCheckBox() {
        if (checkBox.isChecked()) {
            radiusView.setEnabled(true);
            regionView.setEnabled(false);
            authoritiesView.setEnabled(false);
        } else {
            radiusView.setEnabled(false);
            regionView.setEnabled(true);
            authoritiesView.setEnabled(true);
        }
    }

    /**
     * Method for reading JSON from the Hygiene API
     *
     * @param urlString URL to be used for fetching JSON data
     * @param type      the type of the message
     */
    private void readUrl(String urlString, final QueryType type) {

        // create HTTP client
        AsyncHttpClient client = new AsyncHttpClient();

        // setting the headers for Food Hygiene API
        client.addHeader("x-api-version", "2");
        client.addHeader("Accept", "application/json");
        client.get(urlString, new TextHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, String response) {
                Gson gson = new Gson();
                switch (type) {
                    case BUSINESS_TYPE:
                        BusinessTypes result = gson.fromJson(response, BusinessTypes.class);
                        populateBusinessTypes(result);
                        break;
                    case REGIONS:
                        RegionsWrapper regions = gson.fromJson(response, RegionsWrapper.class);
                        populateRegions(regions);
                        break;
                    case AUTHORITIES:
                        AuthoritiesWrapper auth = gson.fromJson(response, AuthoritiesWrapper.class);
                        populateAuthorities(auth);
                        break;
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String response, Throwable t) {
                // display error message in case something went wrong
                errorToast(R.string.failed_query);
            }
        });
    }

    /**
     * Populate the spinner of business types based on the data passed in.
     *
     * @param types the types of all businesses
     */
    private void populateBusinessTypes(BusinessTypes types) {
        // populate the spinner list
        businessTypesStorage = types.businessTypes;
        for (BusinessTypes.businessTypes type : businessTypesStorage)
            businessTypesSpinner.add(type.BusinessTypeName);


        // set the adapter for the spinner view with the list
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, businessTypesSpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        businessTypeView.setAdapter(adapter);
    }

    /**
     * Populate the spinner of regions based on the data passed in.
     *
     * @param regions the all possible regions
     */
    private void populateRegions(RegionsWrapper regions) {
        // populate the spinner list
        regionsStorage = regions.regions;
        for (RegionsWrapper.Regions r : regionsStorage)
            regionsSpinner.add(r.name);

        // set the adapter for the spinner view with the list
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, regionsSpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        regionView.setAdapter(adapter);
    }

    /**
     * Populate the spinner of authorities based on the data passed in.
     *
     * @param auth the all authorities of a particular region
     */
    private void populateAuthorities(AuthoritiesWrapper auth) {
        authoritiesStorage = auth.authorities;
        for (AuthoritiesWrapper.Authorities a : authoritiesStorage)
            authoritiesSpinner.add(a.Name);
    }

    /**
     * Adds the authorities to the adapter based on the result returned
     * by the API call (depending on the region selected)
     */
    private void addAuthorities() {
        if (authoritiesStorage == null) return;
        String rName = regionsSpinner.get(regionView.getSelectedItemPosition());
        authoritiesSpinner.clear();
        for (AuthoritiesWrapper.Authorities a : authoritiesStorage) {
            if (a.RegionName.equals(rName))
                authoritiesSpinner.add(a.Name);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, authoritiesSpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        authoritiesView.setAdapter(adapter);
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
     * Hides the soft keyboard
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


    private void addKeyboardCloseListener() {
        (findViewById(R.id.business_name)).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                Log.e("CALLSD", "DSADASD" + view.getId() + b);
                if (view.getId() == R.id.business_name && b) {
                    Log.e("CALLSD", "DSADASDfasdfsa");
                    hideSoftKeyboard();
                }
            }
        });
    }


    /**
     * Checking whether the network is available for the application.
     *
     * @return true if internet is on, false otherwise.
     */
    private boolean isNetworkAvailable() {
        Log.e("string", "casdfasdf");
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = null;
        if (connectivityManager != null) {
            activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        }
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }


}
