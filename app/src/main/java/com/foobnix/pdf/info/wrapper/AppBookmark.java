package com.foobnix.pdf.info.wrapper;

public class AppBookmark {
    public String path;
    public String text;

    public float p;

    public AppBookmark() {

    }

    public AppBookmark(String path, String text, float percent) {
        super();
        this.path = path;
        this.text = text;
        this.p = percent;
    }

    public int getPage(int pages) {
        return Math.round(p * pages);
    }

    public String getText() {
        return text;
    }

    public String getPath() {
        return path;
    }

    public float getPercent() {
        return p;
    }

    @Override
    public int hashCode() {
        return (path + text + p).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        AppBookmark a = (AppBookmark) obj;
        return a.path.equals(path) && a.text.equals(text) && a.p == p;
    }
}
