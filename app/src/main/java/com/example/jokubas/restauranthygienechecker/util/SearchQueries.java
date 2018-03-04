package com.example.jokubas.restauranthygienechecker.util;

/**
 * Created by jokubas on 04/03/18.
 *
 * class holding all the URLs of the queries to the Hygiene API
 */
public class SearchQueries {
    public static final String ADVANCED_SEARCH_URL = "http://api.ratings.food.gov.uk/Establishments?"+
            "name=%s&businessTypeId=%d&"+
            "schemeTypeKey=FHRS&ratingKey=%s&ratingOperatorKey=GreaterThanOrEqual&"+
            "localAuthorityId=%d&"+
            "sortOptionKey=%s&pageSize=%d&pageNumber=%d";

    public static final String ADVANCED_SEARCH_RADIUS_URL = "http://api.ratings.food.gov.uk/Establishments?"+
            "name=%s&maxDistanceLimit=%d&businessTypeId=%d&"+
            "schemeTypeKey=FHRS&ratingKey=%s&ratingOperatorKey=GreaterThanOrEqual&"+
            "longitude=%f&latitude=%f&" +
            "sortOptionKey=%s&pageSize=%d&pageNumber=%d";

    public static final String LOCAL_SEARCH_URL = "http://api.ratings.food.gov.uk/Establishments?longitude=%f&latitude=%f&sortOptionKey=distance&pageSize=15&pageNumber=%d";
    public static final String SIMPLE_SEARCH_URL = "http://api.ratings.food.gov.uk/Establishments?address=%s&pageSize=15&pageNumber=%d";
    public static final String GEOCODE_POSTCODE_TO_LATLANG_URL = "https://maps.googleapis.com/maps/api/geocode/json?components=postal_code:%s";
    public static final String BUSINESS_TYPES_URL = "http://api.ratings.food.gov.uk/BusinessTypes";
    public static final String REGIONS_URL = "http://api.ratings.food.gov.uk/Regions";
    public static final String AUTHORITIES_URL = "http://api.ratings.food.gov.uk/Authorities";
    public static final String QUERY_DATA = "query_data";

    public static final int DEFAULT_PAGE_SIZE= 15;
}
