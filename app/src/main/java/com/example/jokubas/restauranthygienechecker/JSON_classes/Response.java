package com.example.jokubas.restauranthygienechecker.JSON_classes;

import com.example.jokubas.restauranthygienechecker.JSON_classes.Establishments;

import java.util.List;


/**
 * Created by jokubas on 25/02/18.
 *
 *
 * Class to wrap the response from JSON returned by calling
 * Hygiene Rating API
 *
 */
public class Response {
    public List<Establishments> establishments;
}