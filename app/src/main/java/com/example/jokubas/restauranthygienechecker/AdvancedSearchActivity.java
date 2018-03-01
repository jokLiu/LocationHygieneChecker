package com.example.jokubas.restauranthygienechecker;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;

public class AdvancedSearchActivity extends AppCompatActivity {

    private static final String BUSINESS_TYPES_URL = "http://api.ratings.food.gov.uk/BusinessTypes";
    private static final String REGIONS_URL = "http://api.ratings.food.gov.uk/Regions";
    private static final String AUTHORITIES_URL = "http://api.ratings.food.gov.uk/Authorities";
    private List<String> businessTypesSpinner = new ArrayList<>();
    private List<String> regionsSpinner = new ArrayList<>();
    private List<String> authoritiesSpinner = new ArrayList<>();
    private List<RegionsWrapper.Regions> regionsStorage;
    private List<BusinessTypes.businessTypes> businessTypesStorage;
    private List<AuthoritiesWrapper.Authorities> authoritiesStorage;
    private EditText businessNameView;
    private Spinner businessTypeView;
    private Spinner ratingLowView;
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

        // Views
        businessNameView = findViewById(R.id.business_name);
        businessTypeView = findViewById(R.id.type_of_business);
//        ratingLowView = findViewById(R.id.rating_low);
        ratingHighView = findViewById(R.id.rating_high);
        checkBox = findViewById(R.id.current_loc_check_box);
        radiusView = findViewById(R.id.radius_spinner);
        regionView = findViewById(R.id.region_spinner);
        authoritiesView = findViewById(R.id.authority_spinner);
        search = findViewById(R.id.search);
        try {
            readUrl(BUSINESS_TYPES_URL, QueryType.BUSINESS_TYPE);
            readUrl(REGIONS_URL, QueryType.REGIONS);
            readUrl(AUTHORITIES_URL, QueryType.AUTHORITIES);
        } catch (Exception ex) {
            Log.e("READ_URL", ex.getMessage());
        }

        regionView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                addAuthorities();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        manageCheckBox();

    }


    public void onClickSearch(View view) {

        // TODO check if the data was fetched from the internet
        QueryData dataToPass = new QueryData();
        // new intent
        Intent intent = new Intent(AdvancedSearchActivity.this, MainActivity.class);

        // business name
        dataToPass.name = businessNameView.getText().toString();

        // business type id, -1 indicates failure
        dataToPass.businessTypeId = -1;
        String type = businessTypesSpinner.get(businessTypeView.getSelectedItemPosition());
        for (BusinessTypes.businessTypes b : businessTypesStorage)
            dataToPass.businessTypeId = b.BusinessTypeName.equals(type) ? b.BusinessTypeId : dataToPass.businessTypeId;

        // rating value
        dataToPass.ratingKey = ratingHighView.getSelectedItem().toString();

        // distance
        dataToPass.maxDistanceLimit = Integer.valueOf(radiusView.getSelectedItem().toString());
        ;

        // authority id, -1 indicates failure
        dataToPass.localAuthorityId = -1;

        //use location
        if (checkBox.isChecked()) {
            // TODO CHECK WHETHER LOCATION ACTUALLY EXISTS AND IS ENABLED
            if (!checkGpsStatus()) {
                noResultsToast();
                Log.e("PRINTED", "PRINTED");
                return;
            }
            // if not show the toast
            dataToPass.useLocation = true;

        }
        // use authority and region
        else {
            // TODO exception when no input
            String authName = authoritiesSpinner.get(authoritiesView.getSelectedItemPosition());
            for (AuthoritiesWrapper.Authorities a : authoritiesStorage) {
                if (a.Name.equals(authName)) {
                    dataToPass.localAuthorityId = a.LocalAuthorityId;
                    break;
                }
            }
            dataToPass.useLocation = false;

        }

        intent.putExtra("query_data", dataToPass);
        startActivity(intent);
        finish();

    }

    public void onClickCheckBox(View view) {
        manageCheckBox();
    }

    private void manageCheckBox() {
        if (checkBox.isChecked()) {
            // TODO if current location is used make sure that user gave access to the GPS
            radiusView.setEnabled(true);
            regionView.setEnabled(false);
            authoritiesView.setEnabled(false);
        } else {
            radiusView.setEnabled(false);
            regionView.setEnabled(true);
            authoritiesView.setEnabled(true);
        }
    }

    private void readUrl(String urlString, final QueryType type) throws Exception {

        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("x-api-version", "2");
        client.addHeader("Accept", "application/json");
        client.get(urlString,
                new TextHttpResponseHandler() {
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
                        // TODO figure out later
                    }
                });
    }

    private void populateBusinessTypes(BusinessTypes types) {
        businessTypesStorage = types.businessTypes;
        for (BusinessTypes.businessTypes type : businessTypesStorage) {
            businessTypesSpinner.add(type.BusinessTypeName);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, businessTypesSpinner);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        businessTypeView.setAdapter(adapter);
    }

    private void populateRegions(RegionsWrapper regions) {
        regionsStorage = regions.regions;
        for (RegionsWrapper.Regions r : regionsStorage) {
            regionsSpinner.add(r.name);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, regionsSpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        regionView.setAdapter(adapter);
    }

    private void populateAuthorities(AuthoritiesWrapper auth) {
        authoritiesStorage = auth.authorities;
        for (AuthoritiesWrapper.Authorities a : authoritiesStorage) {
            authoritiesSpinner.add(a.Name);
        }
    }

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


    private boolean checkGpsStatus() {

        LocationManager locationManager = (LocationManager) getApplicationContext().
                getSystemService(Context.LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    private void noResultsToast() {
        Toast toast = new Toast(getBaseContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.toast, null);
        ((TextView)view.findViewById(R.id.error_message)).setText(R.string.not_enabled);

        toast.setView(view);
        toast.show();
    }

}
