package com.eugene.bill.demo;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.util.Map;

public class Utils {

    private static Gson gson = new Gson();

    public static String toJson(Object o) {
        return gson.toJson(o);
    }

    public static Map<String, Object> fromJson(String o) {
        return gson.fromJson(o, LinkedTreeMap.class);
    }

    public static <T> T fromJson(String request, Class<T> detectionResponseClass) {
        return gson.fromJson(request, detectionResponseClass);
    }
}
