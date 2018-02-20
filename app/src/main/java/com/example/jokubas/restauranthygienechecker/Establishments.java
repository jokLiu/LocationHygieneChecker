package com.example.jokubas.restauranthygienechecker;

/**
 * Created by jokubas on 20/02/18.
 */

public class Establishments {
    int FHRSID;
    String LocalAuthorityBusinessID;
    String BusinessName;
    String BusinessType;
    String BusinessTypeID;
    String AddressLine1;
    String AddressLine2;
    String AddressLine3;
    String AddressLine4;
    String PostCode;
    String Phone;
    String RatingValue;
    String RatingKey;
    String RatingDate;
    String LocalAuthorityCode;
    String LocalAuthorityName;
    String LocalAuthorityWebSite;
    String LocalAuthorityEmailAddress;
    String SchemeType;
    String RightToReply;
    double Distance;
    boolean NewRatingPending;

    public class Scores {
        int Hygiene;
        int Structural;
        int ConfidenceInManagement;
    }

    public class Geocode {
        String longitude;
        String latitude;
    }

    @Override
    public String toString(){
        return BusinessName;
    }
}