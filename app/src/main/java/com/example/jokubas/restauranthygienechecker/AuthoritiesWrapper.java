package com.example.jokubas.restauranthygienechecker;

import java.util.List;

/**
 * Created by jokubas on 25/02/18.
 */

public class AuthoritiesWrapper {
    public class Authorities {
        int LocalAuthorityId;
        int LocalAuthorityIdCode;
        String Name;
        String FriendlyName;
        String Url;
        String SchemeUrl;
        String Email;
        String RegionName;
    }

    List<Authorities> authorities;
}
