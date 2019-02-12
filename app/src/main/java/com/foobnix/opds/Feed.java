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

    public Feed(List<Entry> entries) {
        this.entries = entries;
    }

    public Feed() {

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