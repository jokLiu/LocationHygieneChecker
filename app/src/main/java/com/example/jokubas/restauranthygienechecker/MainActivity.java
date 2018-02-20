package com.example.jokubas.restauranthygienechecker;

import android.Manifest;
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
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.gson.Gson;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.TextHttpResponseHandler;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {

    private final int FINE_LOCATION_PERMISSION = 1;
    String json = "";
    private TextView mTextMessage;
    private ArrayList<Establishments> establishments = new ArrayList<>();
    // longitude and latitude has to be inserted
    private String localSearchURL = "http://api.ratings.food.gov.uk/Establishments?longitude=%f&latitude=%f&sortOptionKey=distance&pageSize=15";
    private double longitude;
    private double latitude;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private ArrayAdapter estAdpt;


    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.map:
                    mTextMessage.setText(R.string.title_home);
                    return true;
                case R.id.list:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
            }
            return false;
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // adapter for the list view of the returned responses
        estAdpt = new ArrayAdapter(this, android.R.layout.simple_selectable_list_item, establishments);
        final ListView establView = findViewById(R.id.establishments);
        establView.setAdapter(estAdpt);
        final AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Log.e("View", ((Establishments) estAdpt.getItem(position)).toString());
                Establishments e = (Establishments) estAdpt.getItem(position);
//                 //We need to get the instance of the LayoutInflater, use the context of this activity
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

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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


    }


    public void onLocalSearchClick(View view) {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Log.v("Print", String.format(localSearchURL, longitude, latitude));
        try {
            readUrl(String.format(localSearchURL, longitude, latitude));
        } catch (Exception e) {
//            Log.e("Exception", e.getMessage());
            // TODO handle error state or no response being returned
        }
    }

    private void readUrl(String urlString) throws Exception {

        AsyncHttpClient client = new AsyncHttpClient();
        client.addHeader("x-api-version", "2");
        client.addHeader("Accept", "application/json");
        client.get(urlString,
                new TextHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String response) {
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


}

