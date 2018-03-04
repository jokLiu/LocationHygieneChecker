package com.example.jokubas.restauranthygienechecker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Point;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.jokubas.restauranthygienechecker.JSON_classes.Establishments;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;

/**
 * Created by jokubas on 28/02/18.
 */

public class MapInfoAdapter  implements GoogleMap.InfoWindowAdapter {

    private Activity context;
    private ArrayList<Establishments> establishments;
    private GoogleMap map;

    public MapInfoAdapter(Activity context, ArrayList<Establishments> establishments, GoogleMap map){
        this.context = context;
        this.establishments = establishments;
        this.map = map;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @SuppressLint("ResourceType")
    @Override
    public View getInfoContents(Marker marker) {
        View layout = context.getLayoutInflater().inflate(R.layout.map_pop_up, null);
        Establishments selectedEst = null;
        for(Establishments e : establishments) {
            if(marker.getTitle().equals(e.BusinessName)){
                selectedEst = e;
                break;
            }
        }

        if(selectedEst == null) return null;

        ((TextView) layout.findViewById(R.id.name)).setText(selectedEst.BusinessName);
        ((TextView) layout.findViewById(R.id.type)).setText(selectedEst.BusinessType);
        ((TextView) layout.findViewById(R.id.address)).setText(selectedEst.AddressLine1);
        ((TextView) layout.findViewById(R.id.authority)).setText(selectedEst.LocalAuthorityName);
        ((TextView) layout.findViewById(R.id.authority_email)).setText(selectedEst.LocalAuthorityEmailAddress);
//        ((TextView) layout.findViewById(R.id.rating)).setText(selectedEst.RatingValue);

        ImageView rating = layout.findViewById(R.id.rating);
        switch (selectedEst.RatingValue){
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

        Projection projection = map.getProjection();
        LatLng markerPosition = marker.getPosition();

        Point markerPoint = projection.toScreenLocation(markerPosition);
        android.support.constraint.ConstraintLayout view = context.findViewById(R.id.container);
        Point targetPoint = new Point(markerPoint.x, markerPoint.y - (view.getHeight()*2/7));
        LatLng targetPosition = projection.fromScreenLocation(targetPoint);
        map.animateCamera(CameraUpdateFactory.newLatLng(targetPosition), 1000, null);

        return layout;
    }
}
