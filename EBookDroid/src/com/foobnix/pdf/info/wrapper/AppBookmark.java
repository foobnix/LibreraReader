package com.foobnix.pdf.info.wrapper;

import com.foobnix.android.utils.LOG;

public class AppBookmark {
    private String path;
    private String text;
    private int page;
    private long date;
    private String title;
    private float percent = 0;


    public AppBookmark(String path, String text, int page, String title, long date, float percent) {
        this(path, text, page, title, percent);
        this.date = date;
    }

    public AppBookmark(String path, String text, int page, String title, float percent) {
        super();
        this.path = path;
        this.text = text;
        if (text != null) {
            this.text = text.replace("~", "").replace("  ", " ").replace("  ", " ");
        }
        this.page = page;
        this.percent = percent;
        this.title = title;
        this.date = System.currentTimeMillis();
    }

    public static String decode(AppBookmark bookmark) {
        String format = String.format("%s~%s~%s~%s~%s~%s", bookmark.getPath(), bookmark.getText(), bookmark.getPage(), bookmark.getTitle(), bookmark.getDate(), bookmark.getPercent());
        LOG.d("AppBookmark", format);
        return format;
    }

    public static AppBookmark encode(String line) {
        if (line != null && !line.equals("")) {
            String[] l = line.split("~");
            if (l.length == 5) {
                return new AppBookmark(l[0], l[1], Integer.parseInt(l[2]), l[3], Long.parseLong(l[4]), 0);
            } else if (l.length == 6) {
                return new AppBookmark(l[0], l[1], Integer.parseInt(l[2]), l[3], Long.parseLong(l[4]), Float.parseFloat(l[5]));
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public float getPercent() {
        return percent;
    }

    public void setPercent(float percent) {
        this.percent = percent;
    }

}
