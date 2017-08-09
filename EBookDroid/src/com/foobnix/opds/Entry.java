package com.foobnix.opds;
import java.util.ArrayList;
import java.util.List;

public class Entry {

    public String updated;
    public String id;
    public String title;
    public String content;

    public List<Link> links = new ArrayList<Link>();

}
