package com.example.jokubas.restauranthygienechecker.util;

import java.io.Serializable;

/**
 * Created by jokubas on 01/03/18.
 *
 * Query data to be passed between advanced search and
 * main activities.
 * It is used to pass the data which was retrieved from the view
 * when the user was performing the advanced search
 */
public class QueryData implements Serializable {
    public String name;
    public String businessTypeId;
    public String ratingKey;
    public int localAuthorityId;
    public int maxDistanceLimit;
    public boolean useLocation;
}
