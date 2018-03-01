package com.example.jokubas.restauranthygienechecker;

import java.io.Serializable;

/**
 * Created by jokubas on 01/03/18.
 */

public class QueryData implements Serializable {
    String name;
    int businessTypeId;
    String ratingKey;
    int localAuthorityId;
    int maxDistanceLimit;
    boolean useLocation;
}
