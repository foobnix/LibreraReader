package com.foobnix.pdf.search.activity.msg;

import com.foobnix.android.utils.LOG;

public class MessagePageXY {

    public static int TYPE_SELECT_TEXT = 0;
    public static int TYPE_SHOW = 1;
    public static int TYPE_HIDE = 2;

    private int type;
    private final int page;
    private final float x, y, x1, y1;

    public MessagePageXY(int type) {
        this(type, -1, -1, -1, -1, -1);

    }

    public MessagePageXY(int type, int page, float x, float y, float x1, float y1) {
        this.type = type;
        this.page = page;

        this.x = x;
        this.y = y;

        this.x1 = x1;
        this.y1 = y1;

        LOG.d("MessagePageXY", x, y, x1, y1);
    }

    public int getPage() {
        return page;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getX1() {
        return x1;
    }

    public float getY1() {
        return y1;
    }

    public int getType() {
        return type;
    }

}
