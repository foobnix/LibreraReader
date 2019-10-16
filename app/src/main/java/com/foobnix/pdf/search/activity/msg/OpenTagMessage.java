package com.foobnix.pdf.search.activity.msg;

public class OpenTagMessage {

    private final String name;

    public OpenTagMessage(String tag) {
        super();
        if (tag.contains("(")) {
            tag = tag.substring(0, tag.indexOf("("));
        }
        this.name = tag;
    }

    public String getTagName() {
        return name;
    }

}
