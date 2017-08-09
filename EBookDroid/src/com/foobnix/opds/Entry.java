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

    public Entry(String homeUrl, String title) {
        this.title = title;
        links.add(new Link(homeUrl));
    }

    public String getTitle() {
        if (author != null) {
            return author + " - " + title;
        }
        return title;
    }

}
