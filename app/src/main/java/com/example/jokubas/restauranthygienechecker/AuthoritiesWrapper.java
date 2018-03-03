package com.example.jokubas.restauranthygienechecker;

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
        int LocalAuthorityId;
        /**
         * The Local authority id code.
         */
        int LocalAuthorityIdCode;
        /**
         * The Name.
         */
        String Name;
        /**
         * The Friendly name.
         */
        String FriendlyName;
        /**
         * The Url.
         */
        String Url;
        /**
         * The Scheme url.
         */
        String SchemeUrl;
        /**
         * The Email.
         */
        String Email;
        /**
         * The Region name.
         */
        String RegionName;
    }

    /**
     * The Authorities.
     */
    List<Authorities> authorities;
}
