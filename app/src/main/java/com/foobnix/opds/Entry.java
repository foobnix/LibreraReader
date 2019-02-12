package com.foobnix.opds;

import java.util.ArrayList;
import java.util.List;

import com.foobnix.android.utils.TxtUtils;

public class Entry {

    public static final String MY_CATALOG = ";my-catalog";
    public String updated;
    public String id;
    public String title = "";
    public String content = "";
    public String author;
    public String authorUrl;
    public String category = "";
    public String summary = "";
    public String year;
    public String homeUrl;
    public String logo;

    public List<Link> links = new ArrayList<Link>();

    public Entry() {
    }

    public String appState;

    public String tempLogo;

    public Entry(String homeUrl, String title) {
        this(homeUrl, title, null, null, false);

    }

    public static Entry Home(String url, String title) {
        String homeUrl2 = url + (url.contains("?") ? SamlibOPDS.LIBRERA_MOBI.replace("?", "&") : SamlibOPDS.LIBRERA_MOBI);
        return new Entry(homeUrl2, title, url, null, false);
    }

    public Entry(String homeUrl, String title, String subtitle, String logo) {
        this(homeUrl, title, subtitle, logo, false);
    }

    public Entry(String homeUrl, String title, String subtitle, String logo, boolean isRemovable) {
        this.homeUrl = homeUrl;
        if (isRemovable) {
            setAppState(homeUrl, title, subtitle, logo);
        }

        this.title = title;

        if (logo != null) {
            tempLogo = logo;
            Link logoLink = new Link(logo, Link.TYPE_LOGO);
            links.add(logoLink);
        }
        if (subtitle != null) {
            links.add(new Link(homeUrl, Link.APPLICATION_ATOM_XML + MY_CATALOG, subtitle));
        }

        links.add(new Link(homeUrl));

    }

    public void setAppState(String homeUrl, String title, String subtitle, String logo) {
        appState = homeUrl + "," + TxtUtils.fixAppState(title) + "," + TxtUtils.fixAppState(subtitle) + "," + logo + ";";
        this.logo = logo;
    }

    public Entry(String title, Link... items) {
        this.title = title;
        for (Link link : items) {
            links.add(link);
        }
    }

    public String getTitle() {
        if (author != null) {
            return author + " - " + title;
        }
        return title;
    }

}
