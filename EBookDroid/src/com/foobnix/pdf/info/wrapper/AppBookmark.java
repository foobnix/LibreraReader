package com.foobnix.pdf.info.wrapper;

import com.foobnix.android.utils.LOG;

public class AppBookmark {
    private static final String DIV = "~";
    private String path;
    private String text;
    private String title;
    private int page;
    private long date;
    private float percent = 0;

    public AppBookmark(String path, String text, int page, String title, long date, float percent) {
        this(path, text, page, title, percent);
        this.date = date;
    }

    public AppBookmark(String path, String text, int page, String title, float percent) {
        super();
        this.path = fixText(path);
        this.text = fixText(text);
        this.title = fixText(title);

        if (text != null) {
            this.text = text.replace("  ", " ").replace("  ", " ");
        }
        this.page = page;
        this.percent = percent;
        this.date = System.currentTimeMillis();
    }

    private static String fixText(String path) {
        if (path == null) {
            return "";
        }
        return path.replace(DIV, "-");
    }

    public static String decode(AppBookmark bookmark) {
        String format = String.format("%s~%s~%s~%s~%s~%s", fixText(bookmark.getPath()), fixText(bookmark.getText()), bookmark.getPage(), fixText(bookmark.getTitle()), bookmark.getDate(), bookmark.getPercent());
        LOG.d("AppBookmark", format);
        return format;
    }

    public static AppBookmark encode(String line) {
        try {
            if (line != null && !line.equals("")) {
                String[] l = line.split(DIV);
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
        } catch (Exception e) {
            LOG.e(e);
            return null;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = fixText(path);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = fixText(text);
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
        this.title = fixText(title);
    }

    public float getPercent() {
        return percent;
    }

    public void setPercent(float percent) {
        this.percent = percent;
    }

}
