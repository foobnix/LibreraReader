package com.foobnix.pdf.info.model;

import com.foobnix.ext.Fb2Extractor;
import com.foobnix.pdf.info.PageUrl;

public class OutlineLinkWrapper {

    public final int level;
    private final String title;
    public String targetUrl;
    public int targetPage = -1;
    public long docHandle;

    public String linkUri;

    public OutlineLinkWrapper(final String title, final String link, final int level, long docHandle, String linkUri) {
        this.title = title;
        this.level = level;
        this.docHandle = docHandle;

        this.linkUri = linkUri;

        if (link != null) {
            if (link.startsWith("#")) {
                try {
                    targetPage = Integer.parseInt(link.substring(1).replace(" ", ""));
                    targetPage = PageUrl.realToFake(targetPage);

                } catch (final Exception e) {
                    e.printStackTrace();
                    targetPage = -1;
                }
            } else if (link.startsWith("http:")) {
                targetUrl = link;
            }
        }
    }

    public static int getPageNumber(String link) {
        if (link != null) {
            if (link.startsWith("#")) {
                try {
                    int page = Integer.parseInt(link.substring(1).replace(" ", ""));
                    return PageUrl.realToFake(page);

                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return -1;

    }

    public String getTitleRaw() {
        return title;

    }

    public String getTitleAsString() {
        String t = title;
        if (t.contains("$")) {
            t = t.substring(0, title.indexOf("$"));
        }
        t = t.replace(Fb2Extractor.DIVIDER, "");
        t = t.replaceAll("^\\s+", "");
        return t;
    }


}
