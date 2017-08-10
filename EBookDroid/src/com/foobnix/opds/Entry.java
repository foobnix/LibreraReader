package com.foobnix.opds;

import java.util.ArrayList;
import java.util.List;

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

    public String tempLogo;

    public Entry(String homeUrl, String title) {
        this(homeUrl, title, null, null);

    }

    public Entry(String homeUrl, String title, String subtitle, String logo) {
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
