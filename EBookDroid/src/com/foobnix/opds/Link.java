package com.foobnix.opds;

import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;

public class Link {

    public static final String APPLICATION_ATOM_XML = "application/atom+xml";
    public static final String APPLICATION_ATOM_XML_PROFILE = "application/atom+xml;profile";
    public static final String IMG_LINK_JPG = "image/jpeg";
    public static final String IMG_LINK_PNG = "image/png";
    public static final String WEB_LINK = "text/html";
    public static final String DISABLED = "disabled/";
    public static final String TYPE_LOGO = "MY_LOGO";
    public static final String REL_THUMBNAIL1 = "http://opds-spec.org/image/thumbnail";
    public static final String REL_THUMBNAIL2 = "http://opds-spec.org/thumbnail";

    public String href;
    public String type = "";
    public String rel;
    public String title;

    public String parentTitle;
    public String filePath;

    static Map<String, String> map = new HashMap<String, String>();
    {
        map.put("text/html", "web");
        map.put("application/pdf", "pdf");
        map.put("application/djvu", "djvu");
        map.put("application/epub+zip", "epub");
        map.put("application/fb-ebook", "fb2");
        map.put("application/x-mobipocket-ebook", "mobi");
    }

    public Link(String href) {
        type = APPLICATION_ATOM_XML;
        this.href = href;
    }

    public Link(String href, String type) {
        this.type = type;
        this.href = href;
    }

    public Link(String href, String type, String title) {
        this.type = type;
        this.href = href;
        this.title = title;
    }

    public Link(XmlPullParser xpp) {
        href = xpp.getAttributeValue(null, "href");
        rel = xpp.getAttributeValue(null, "rel");
        type = xpp.getAttributeValue(null, "type");
        title = xpp.getAttributeValue(null, "title");
    }

    public boolean isThumbnail() {
        return rel != null && (rel.equals(REL_THUMBNAIL1) || rel.equals(REL_THUMBNAIL2));
    }

    public boolean isSearchLink(){
        return "search".equals(rel) && APPLICATION_ATOM_XML.equals(type);
    }


    public boolean isOpdsLink() {
        return type.startsWith(APPLICATION_ATOM_XML);
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

    public String getDownloadDisplayFormat() {
        for (String item : map.keySet()) {
            if (item.equals(type)) {
                return map.get(item);
            }
        }

        if (type.contains("+zip")) {
            return type.replace("application/", "").replace("+", ".");
        }

        return null;

    }

    public String getDownloadName() {
        String name = parentTitle.replace("/", "");
        String ext = getDownloadDisplayFormat();
        if (ext != null) {
            return name + "." + ext;
        }

        return name + type.replace("application/", ".").replace("+", ".");
    }

}
