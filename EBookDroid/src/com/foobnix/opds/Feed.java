package com.foobnix.opds;
import java.util.ArrayList;
import java.util.List;

public class Feed {

    public String id;
    public String title;
    public String updated;
    public String icon;

    public List<Link> links = new ArrayList<Link>();

    public List<Entry> entries = new ArrayList<Entry>();


}
