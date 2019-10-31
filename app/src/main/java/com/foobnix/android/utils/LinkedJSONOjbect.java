package com.foobnix.android.utils;

import java.util.Iterator;
import java.util.LinkedHashMap;

public class LinkedJSONOjbect {

    LinkedHashMap<String, String> map = new LinkedHashMap<>();

    public LinkedJSONOjbect(String in) {

        LOG.d("LinkedJSONOjbect", "in", in);

        in = in.replace("'","\"");

        in = in.substring(1);
        in = in.substring(0, in.length() - 1);

        LOG.d("LinkedJSONOjbect", "in", in);



        String items[] = in.split("\",\"");
        for (String line : items) {
            String keyValue[] = line.split("\":\"");
            String key = keyValue[0];
            String value = keyValue[1];

            map.put(key, value);
            LOG.d("LinkedJSONOjbect", "put", key, value);

        }

    }

    public LinkedJSONOjbect() {

    }

    public void put(String from, String to) {
        map.put(from, to);
    }

    public String toString() {
        StringBuilder res = new StringBuilder();
        res.append("{");
        for (String key : map.keySet()) {
            res.append("\"" + key + "\"");
            res.append(":");
            res.append("\"" + map.get(key) + "\"");
            res.append(",");
        }
        res.append("}");
        return res.toString().replace(",}", "}");

    }

    public Iterator<String> keys() {
        return map.keySet().iterator();
    }

    public String getString(String key) {
        return map.get(key);
    }
}
