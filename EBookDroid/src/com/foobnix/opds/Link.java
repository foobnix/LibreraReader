package com.foobnix.opds;
import org.xmlpull.v1.XmlPullParser;

public class Link {

    public String href;
    public String type;
    public String rel;
    public String title;

    public Link(XmlPullParser xpp) {
        href = xpp.getAttributeValue(null, "href");
        rel = xpp.getAttributeValue(null, "rel");
        type = xpp.getAttributeValue(null, "type");
        title = xpp.getAttributeValue(null, "title");
    }

}
