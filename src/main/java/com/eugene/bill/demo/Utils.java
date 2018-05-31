package com.eugene.bill.demo;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.util.Map;

public class Utils {

    public static String toJson(Object o) {
        return new Gson().toJson(o);
    }

    public static Map<String, Object> fromJson(String o) {
        return new Gson().fromJson(o, LinkedTreeMap.class);
    }

}
