package com.foobnix.opds;

import org.xmlpull.v1.XmlPullParser;

public class Link {

    public static final String ATOM_XML = "application/atom+xml";
    public static final String IMG_LINK_JPG = "image/jpeg";
    public static final String IMG_LINK_PNG = "image/png";
    public static final String WEB_LINK = "text/html";
    public static final String DISABLED = "disabled/";

    public String href;
    public String type = "";
    public String rel;
    public String title;

    public String parentTitle;

    public Link(String href) {
        type = ATOM_XML;
        this.href = href;
    }

    public Link(XmlPullParser xpp) {
        href = xpp.getAttributeValue(null, "href");
        rel = xpp.getAttributeValue(null, "rel");
        type = xpp.getAttributeValue(null, "type");
        title = xpp.getAttributeValue(null, "title");
    }


    public boolean isOpdsLink() {
        return type.startsWith(ATOM_XML);
    }

    public boolean isDisabled() {
        return type.startsWith(DISABLED);
    }

    public boolean isImageLink() {
        return IMG_LINK_JPG.equals(type) || IMG_LINK_PNG.equals(type) || "image/gif".equals(type) || "image/jpg".equals(type);
    }

    public boolean isWebLink() {
        return WEB_LINK.equals(type);
    }

    public String getDownloadName() {
        if ("application/epub+zip".equals(type)) {
            return parentTitle + ".epub";
        }
        if ("application/fb-ebook".equals(type)) {
            return parentTitle + ".fb2";

        }
        if ("application/x-mobipocket-ebook".equals(type)) {
            return parentTitle + ".mobi";
        }
        return parentTitle + type.replace("application/", ".").replace("disabled/", ".").replace("+", ".");
    }

}
