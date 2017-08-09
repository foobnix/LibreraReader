package com.foobnix.opds;

import java.util.ArrayList;
import java.util.List;

public class Entry {

    public String updated;
    public String id;
    public String title;
    public String content;
    public String author;
    public String category = "";

    public List<Link> links = new ArrayList<Link>();

    public String getTitle() {
        if (author != null) {
            return author + " - " + title;
        }
        return title;
    }

}
