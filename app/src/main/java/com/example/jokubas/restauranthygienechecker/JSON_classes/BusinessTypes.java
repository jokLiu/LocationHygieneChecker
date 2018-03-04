package com.example.jokubas.restauranthygienechecker.JSON_classes;

import java.util.List;

/**
 * Created by jokubas on 25/02/18.
 */


public class BusinessTypes {

    public List<businessTypes> businessTypes;

    public class businessTypes {
        public int BusinessTypeId;
        public String BusinessTypeName;
        public List<links> links;

        public class links {
            String rel;
            String href;
        }
    }

}