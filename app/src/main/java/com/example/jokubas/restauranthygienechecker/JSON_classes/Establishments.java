package com.example.jokubas.restauranthygienechecker.JSON_classes;

/**
 * Created by jokubas on 20/02/18.
 */

public class Establishments {
    public int FHRSID;
    public String LocalAuthorityBusinessID;
    public String BusinessName;
    public String BusinessType;
    public String BusinessTypeID;
    public String AddressLine1;
    public String AddressLine2;
    public String AddressLine3;
    public String AddressLine4;
    public String PostCode;
    public String Phone;
    public String RatingValue;
    public String RatingKey;
    public String RatingDate;
    public String LocalAuthorityCode;
    public String LocalAuthorityName;
    public String LocalAuthorityWebSite;
    public String LocalAuthorityEmailAddress;
    public String SchemeType;
    public String RightToReply;
    public double Distance;
    public boolean NewRatingPending;

    public class Scores {
        public  int Hygiene;
        public  int Structural;
        public int ConfidenceInManagement;
    }

    public class Geocode {
        public String longitude;
        public String latitude;
    }
    public Scores scores;
    public Geocode geocode;

    @Override
    public String toString(){
        return BusinessName;
    }
}