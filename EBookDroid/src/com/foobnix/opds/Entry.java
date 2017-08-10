package com.foobnix.opds;

import java.util.ArrayList;
import java.util.List;

import com.foobnix.android.utils.TxtUtils;

public class Entry {

    public String updated;
    public String id;
    public String title = "";
    public String content = "";
    public String author;
    public String category = "";

    public List<Link> links = new ArrayList<Link>();

    public Entry() {
    }

    public String appState;

    public String tempLogo;

    public Entry(String homeUrl, String title) {
        this(homeUrl, title, null, null);

    }


    public Entry(String homeUrl, String title, String subtitle, String logo) {

        setAppState(homeUrl, title, subtitle, logo);

        this.title = title;

        if (logo != null) {
            tempLogo = logo;
            Link logoLink = new Link(logo, Link.TYPE_LOGO);
            links.add(logoLink);
        }
        if (subtitle != null) {
            links.add(new Link(homeUrl, Link.APPLICATION_ATOM_XML + ";subtitle", subtitle));
        }

        links.add(new Link(homeUrl));

    }


    public void setAppState(String homeUrl, String title, String subtitle, String logo) {
        appState = homeUrl + "," + TxtUtils.fixAppState(title) + "," + TxtUtils.fixAppState(subtitle) + "," + logo + ";";
    }

    public Entry(String title, Link link) {
        this.title = title;
        links.add(link);
    }

    public String getTitle() {
        if (author != null) {
            return author + " - " + title;
        }
        return title;
    }

}
