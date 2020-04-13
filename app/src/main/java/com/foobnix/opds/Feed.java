package com.foobnix.opds;

import java.util.ArrayList;
import java.util.List;

public class Feed {

    public String id;
    public String title;
    public String updated;
    public String icon;
    public String subtitle;

    public List<Link> links = new ArrayList<Link>();

    public List<Entry> entries = new ArrayList<Entry>();

    public boolean isNeedLoginPassword;

    public String homeUrl;



    public Feed(String homeUrl, List<Entry> entries) {
        this.homeUrl = homeUrl;
        this.entries = entries;
    }

    public Feed(String homeUrl) {
        this.homeUrl  = homeUrl;

    }

    public void updateLinksForUI() {
        Link search = hasSearchLink();
        if (search != null) {
            entries.add(0, new Entry("", search));
        }
    }

    private Link hasSearchLink() {
        for (Link link : links) {
            if (link.isSearchLink()) {
                return link;
            }
        }
        return null;
    }

}