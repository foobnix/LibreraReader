package com.foobnix.opds;

import org.json.JSONObject;

import com.foobnix.android.utils.LOG;

public class OpdsItem {

    public String homeUrl;
    public String title;
    public String subtitle;
    public String logo;

    public OpdsItem(String homeUrl, String title, String subtitle, String logo) {
        this.homeUrl = homeUrl;
        this.title = title;
        this.subtitle = subtitle;
        this.logo = logo;
    }



    public JSONObject toJSON() {
        JSONObject o = new JSONObject();
        try {
            o.put("homeUrl", homeUrl);
            o.put("title", title);
            o.put("subtitle", subtitle);
            o.put("logo", logo);
        } catch (Exception e) {
            LOG.e(e);
        }
        return o;
    }

    public OpdsItem fromJSON(String json) {
        try {
            JSONObject o = new JSONObject(json);

            String homeUrl = o.optString("homeUrl");
            String title = o.optString("title");
            String subtitle = o.optString("subtitle");
            String logo = o.optString("logo");

            return new OpdsItem(homeUrl, title, subtitle, logo);
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }
}
