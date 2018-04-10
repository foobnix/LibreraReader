package com.foobnix.opds;

import java.util.HashMap;
import java.util.Map;

import org.xmlpull.v1.XmlPullParser;

import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.ExtUtils;

public class Link {

    public static final String APPLICATION_ATOM_XML = "application/atom+xml";
    public static final String APPLICATION_ATOM_XML_PROFILE = "application/atom+xml;profile";
    public static final String APPLICATION_ATOM_XML_SUBLINE = "application/atom+xml;subline";
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
        map.put("text/download", "txt");
        map.put("application/rtf", "rtf");
        map.put("application/msword", "doc");
        map.put("application/doc", "doc");
        map.put("application/docx", "docx");
        map.put("application/pdf", "pdf");
        map.put("application/pdb", "pdb");
        map.put("application/djvu", "djvu");
        map.put("application/epub+zip", "epub");
        map.put("application/epub", "epub");
        map.put("application/fb-ebook", "fb2");
        map.put("application/fb2+xml", "fb2");
        map.put("application/fb-ebook+zip", "fb2.zip");
        map.put("application/x-sony-bbeb", "lrf");
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
        return rel != null && rel.contains("thumbnail") && ExtUtils.isImageMime(type);
    }

    public boolean isSearchLink() {
        return "search".equals(rel) && APPLICATION_ATOM_XML.equals(type);
    }

    public boolean isOpdsLink() {
        return type.startsWith(APPLICATION_ATOM_XML);
    }

    public boolean isDisabled() {
        return type.startsWith(DISABLED) || type.equals("image/");
    }

    public boolean isImageLink() {
        return ExtUtils.isImageMime(type);
    }

    public boolean isWebLink() {
        return WEB_LINK.equals(type);
    }

    public String getDownloadDisplayFormat() {
        if (type == null) {
            return null;
        }
        for (String item : map.keySet()) {
            if (item.equals(type)) {
                return map.get(item);
            }
        }

        if (type.contains("+zip") || type.contains("+rar")) {
            return type.replace("application/", "").replace("+", ".");
        }

        return null;

    }

    public String getDownloadName() {
        if (parentTitle == null) {
            return "";
        }
        String name = TxtUtils.sanitizeFilename(parentTitle);
        String ext = getDownloadDisplayFormat();
        if (ext != null) {
            return name + "." + ext;
        }
        if (type == null) {
            return name;
        }

        return name + type.replace("application/", ".").replace("+", ".");
    }

}
