package com.example.jokubas.restauranthygienechecker.JSON_classes;

import java.util.List;


/**
 * Created by jokubas on 25/02/18.
 *
 *
 * Class to wrap the response from JSON returned by calling
 * Hygiene Rating API
 *
 */
public class AuthoritiesWrapper {
    public class Authorities {
        public int LocalAuthorityId;
        public int LocalAuthorityIdCode;
        public String Name;
        public String FriendlyName;
        public String Url;
        public String SchemeUrl;
        public String Email;
        public String RegionName;
    }

    public List<Authorities> authorities;
}
