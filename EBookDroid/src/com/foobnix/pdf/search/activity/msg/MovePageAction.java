package com.foobnix.pdf.search.activity.msg;

public class MovePageAction {

    public static int LEFT = 1;
    public static int RIGHT = 2;
    public static int UP = 3;
    public static int DOWN = 4;
    public static int ZOOM_PLUS = 5;
    public static int ZOOM_MINUS = 6;
    public static int CENTER = 7;

    private final int action;
    private int page;

    public MovePageAction(int action, int page) {
        super();
        this.action = action;
        this.page = page;
    }

    public int getAction() {
        return action;
    }

    public int getPage() {
        return page;
    }

}
