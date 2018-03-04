package com.example.jokubas.restauranthygienechecker.JSON_classes;

import java.util.List;

/**
 * Created by jokubas on 25/02/18.
 */
public class AuthoritiesWrapper {
    /**
     * The type Authorities.
     */
    public class Authorities {
        /**
         * The Local authority id.
         */
        public int LocalAuthorityId;
        /**
         * The Local authority id code.
         */
        public int LocalAuthorityIdCode;
        /**
         * The Name.
         */
        public String Name;
        /**
         * The Friendly name.
         */
        public String FriendlyName;
        /**
         * The Url.
         */
        public String Url;
        /**
         * The Scheme url.
         */
        public String SchemeUrl;
        /**
         * The Email.
         */
        public String Email;
        /**
         * The Region name.
         */
        public String RegionName;
    }

    /**
     * The Authorities.
     */
    public List<Authorities> authorities;
}
