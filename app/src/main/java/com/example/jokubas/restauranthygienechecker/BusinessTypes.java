package com.example.jokubas.restauranthygienechecker;

import java.util.List;

/**
 * Created by jokubas on 25/02/18.
 */


public class BusinessTypes {

    public List<businessTypes> businessTypes;

    public class businessTypes {
        int BusinessTypeId;
        String BusinessTypeName;
        List<links> links;

        public class links{
            String rel;
            String href;
        }
    }

}