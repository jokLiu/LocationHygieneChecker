package com.example.jokubas.restauranthygienechecker.JSON_classes;

import java.util.List;

/**
 * Created by jokubas on 25/02/18.
 */

public class RegionsWrapper {
    public class Regions{
        public int id;
        public String name;
        public String nameKey;
        public String code;
    }

    public List<Regions> regions;
}
