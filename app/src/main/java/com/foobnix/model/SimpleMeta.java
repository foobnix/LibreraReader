package com.foobnix.model;

public class SimpleMeta {

    public String path;
    public long time;

    public SimpleMeta() {

    }

    public SimpleMeta(String path) {
        this.path = path;
    }

    public SimpleMeta(String path, long time) {
        this.path = path;
        this.time = time;

    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && (((SimpleMeta) obj).path.equals(path));
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
