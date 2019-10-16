package com.foobnix.pdf.search.activity.msg;

public class OpenDirMessage {

    private final String path;

    public OpenDirMessage(String path) {
        super();
        this.path = path;
    }

    public String getPath() {
        return path;
    }

}
