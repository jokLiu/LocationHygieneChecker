package com.example.jokubas.restauranthygienechecker.util;

/**
 * Created by jokubas on 01/03/18.
 */

public class SearchQueries {
    public static final String ADVANCED_SEARCH_URL = "http://api.ratings.food.gov.uk/Establishments?"+
            "name=%s&businessTypeId=%d&"+
            "schemeTypeKey=FHRS&ratingKey=%s&ratingOperatorKey=GreaterThanOrEqual&"+
            "localAuthorityId=%d&"+
            "sortOptionKey=%s&pageNumber=%d&pageSize=%d";

    public static final String ADVANCED_SEARCH_RADIUS_URL = "http://api.ratings.food.gov.uk/Establishments?"+
            "name=%s&maxDistanceLimit=%d&businessTypeId=%d&"+
            "schemeTypeKey=FHRS&ratingKey=%s&ratingOperatorKey=GreaterThanOrEqual&"+
            "sortOptionKey=%s&pageNumber=%d&pageSize=%d&" +
             "longitude=%f&latitude=%f";
    // TODO change size from 30 to 15
    public static final String LOCAL_SEARCH_URL = "http://api.ratings.food.gov.uk/Establishments?longitude=%f&latitude=%f&sortOptionKey=distance&pageSize=15";
    public static final String SIMPLE_SEARCH_URL = "http://api.ratings.food.gov.uk/Establishments?address=%s&pageSize=15&pageNumber=%d";

    public static final String GEOCODE_POSTCODE_TO_LATLANG_URL = "https://maps.googleapis.com/maps/api/geocode/json?components=postal_code:%s";

}
