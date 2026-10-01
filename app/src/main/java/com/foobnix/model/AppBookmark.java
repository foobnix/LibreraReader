package com.foobnix.model;

import android.net.Uri;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.SafDocumentIdentity;

import java.io.File;

public class AppBookmark implements MyPath.RelativePath {
    public String path;
    public String text;
    public String pt; // page text, to find the bookmark page when the book is re-laid out (font size change)

    public float p;
    public long t;
    public boolean isF = false;

    transient public File file;

    public AppBookmark() {

    }

    public AppBookmark(String path, String text, float percent) {
        super();
        setPath(path);
        this.text = text;
        this.p = percent;
        t = System.currentTimeMillis();

    }

    public int getPage(int pages) {
        LOG.d("MyMath getPage",p, pages);
        return Math.max(1,Math.round(p * pages));
    }

    public String getText() {
        return text;
    }

    public String getPath() {
        String absolute = MyPath.toAbsolute(path);
        return ExtUtils.isExteralSD(absolute)
                ? SafDocumentIdentity.canonical(Uri.parse(absolute)).toString() : absolute;
    }

    public void setPath(String path) {
        this.path = MyPath.toRelative(ExtUtils.isExteralSD(path)
                ? SafDocumentIdentity.canonical(Uri.parse(path)).toString() : path);
    }

    public float getPercent() {
        return p;
    }

    public long getTime() {
        return t;
    }

    @Override
    public int hashCode() {
        return (path + text + p).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        AppBookmark a = (AppBookmark) obj;
        return a.t == t;
    }


}
