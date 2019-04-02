package com.foobnix.model;

public class SimpleMeta implements MyPath.RelativePath {

    public String path;
    public long time;


    public SimpleMeta() {

    }

    public SimpleMeta(String path, long time) {
        this.path = MyPath.toRelative(path);
        this.time = time;
    }

    public SimpleMeta(String path) {
        this.path = MyPath.toRelative(path);
    }

    public String getPath() {
        return MyPath.toAbsolute(path);
    }

    public void setPath(String path) {
        this.path = MyPath.toRelative(path);
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && ((SimpleMeta) obj).path.equals(path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
