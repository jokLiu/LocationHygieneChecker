package com.example.jokubas.restauranthygienechecker;

import java.util.List;

/**
 * Created by jokubas on 20/02/18.
 */

public class Response {


    public List<Establishments> establishments;

    @Override
    public String toString(){
        return establishments.get(0).BusinessName;
    }

}