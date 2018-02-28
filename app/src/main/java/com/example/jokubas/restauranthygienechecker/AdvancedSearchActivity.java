package com.example.jokubas.restauranthygienechecker;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

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
    // TODO check maxDistanceLimit
    private static final String ADVANCED_SEARCH_URL = "http://api.ratings.food.gov.uk/Establishments?"+
            "name=%s&businessTypeId=%d&"+
            "schemeTypeKey=FHRS&ratingKey=%s&ratingOperatorKey=GreaterThanOrEqual&"+
            "localAuthorityId=%d&"+
            "sortOptionKey=%s&pageNumber=%d&pageSize=%d";

    private static final String ADVANCED_SEARCH_RADIUS_URL = "http://api.ratings.food.gov.uk/Establishments?"+
            "name=%s&"+
            "maxDistanceLimit=%d&businessTypeId=%d&"+
            "schemeTypeKey=FHRS&ratingKey=%s&ratingOperatorKey=GreaterThanOrEqual&"+
            "sortOptionKey=%s&pageNumber=%d&pageSize=%d&";//+
           // "longitude=%f&latitude=%f&";
    private List<String> businessTypesSpinner = new ArrayList<>();
    private List<String> regionsSpinner = new ArrayList<>();
    private List<String> authoritiesSpinner = new ArrayList<>();
    private List<RegionsWraper.Regions> regionsStorage;
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
        ratingLowView = findViewById(R.id.rating_low);
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
        String apiQuery = "";
        Intent intent = new Intent(AdvancedSearchActivity.this, MainActivity.class);
        String name = businessNameView.getText().toString();
        int typeID = 0;
        String type = businessTypesSpinner.get(businessTypeView.getSelectedItemPosition());
        for(BusinessTypes.businessTypes b : businessTypesStorage)
            typeID = b.BusinessTypeName.equals(type) ? b.BusinessTypeId : typeID;

        String rating = ratingHighView.getSelectedItem().toString();
        int radius = -1;
        int authID = -1;
        //use location
        // TODO CHECK WHETHER LOCATION ACTUALLY EXISTS AND IS ENABLED
        if(checkBox.isChecked()){
            radius = Integer.valueOf(radiusView.getSelectedItem().toString());
            intent.putExtra("check", false);
            apiQuery = String.format(ADVANCED_SEARCH_RADIUS_URL,name,radius, typeID,rating, "distance",1,20);
            Log.e("QUERY",apiQuery);
            intent.putExtra("check", false);
        }
        // use authority and region
        else{
            String authName = authoritiesSpinner.get(authoritiesView.getSelectedItemPosition());
            for(AuthoritiesWrapper.Authorities a : authoritiesStorage) {
                authID = a.Name.equals(authName) ? a.LocalAuthorityId : authID;
//                break;
            }
            apiQuery = String.format(ADVANCED_SEARCH_URL,name,typeID,rating, authID, "rating",1,20);
            Log.e("QUERY",apiQuery);
            intent.putExtra("check", true);
        }

        intent.putExtra("query", apiQuery);
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
            regionView.setEnabled(false);
        } else {
            radiusView.setEnabled(false);
            regionView.setEnabled(true);
            regionView.setEnabled(true);
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
                                RegionsWraper regions = gson.fromJson(response, RegionsWraper.class);
                                populateRegions(regions);
                                break;
                            case AUTHORITIES:
                                Log.v("Response", response);
                                AuthoritiesWrapper auth = gson.fromJson(response, AuthoritiesWrapper.class);
                                populateAuthorities(auth);
                                break;
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String response, Throwable t) {
                        // TODO figure out later
//                        Log.v("JSON",response);
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

    private void populateRegions(RegionsWraper regions) {
        regionsStorage = regions.regions;
        for (RegionsWraper.Regions r : regionsStorage) {
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


}
