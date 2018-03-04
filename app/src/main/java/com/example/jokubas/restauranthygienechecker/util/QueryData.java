package com.example.jokubas.restauranthygienechecker.util;

import java.io.Serializable;

/**
 * Created by jokubas on 01/03/18.
 */

public class QueryData implements Serializable {
    public String name;
    public int businessTypeId;
    public String ratingKey;
    public int localAuthorityId;
    public int maxDistanceLimit;
    public boolean useLocation;
}
