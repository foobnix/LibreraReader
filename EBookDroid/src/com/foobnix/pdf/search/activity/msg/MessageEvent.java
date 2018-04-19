package com.foobnix.pdf.search.activity.msg;

public class MessageEvent {
    public static final String LOAD_TEXT_FOR_THIS_PAGE = "LOAD_TEST_FOR_PAGE";
    public static final String MESSAGE_PERFORM_CLICK = "MESSAGE_PERFORM_CLICK";
    public static final String MESSAGE_SELECTED_TEXT = "MESSAGE_SELECT_TEXT";
    public static final String MESSAGE_GOTO_PAGE_BY_LINK = "MESSAGE_GOTO_PAGE_BY_LINK";
    public static final String MESSAGE_GOTO_PAGE_SWIPE = "MESSAGE_GOTO_PAGE_SWIPE";
    public static final String MESSAGE_DOUBLE_TAP = "MESSAGE_DOUBLE_TAP";
    public static final String MESSAGE_CLOSE_BOOK = "MESSAGE_CLOSE_BOOK";
    public static final String MESSAGE_CLOSE_BOOK_APP = "MESSAGE_CLOSE_BOOK_APP";

    private String msgType = "";
    private int page;
    private float x;
    private float y;
    private String body;

    public MessageEvent(String msg) {
        this.msgType = msg;
    }

    public MessageEvent(String msg, int page) {
        this.msgType = msg;
        this.page = page;
    }

    public MessageEvent(String msg, int page, String body) {
        this.msgType = msg;
        this.page = page;
        this.body = body;
    }

    public MessageEvent(String msg, String body) {
        this.msgType = msg;
        this.body = body;
    }

    public MessageEvent(String msg, float x, float y) {
        this.msgType = msg;
        this.x = x;
        this.y = y;

    }

    public MessageEvent() {
    }

    public String getMessage() {
        return msgType;
    }

    public void setMessage(String message) {
        this.msgType = message;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

}
