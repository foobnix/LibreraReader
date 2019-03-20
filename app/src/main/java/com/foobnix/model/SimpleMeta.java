package com.foobnix.model;

public class SimpleMeta implements MyFile.RelativePath {

    public String path;
    public long time;

    public SimpleMeta(String path, long time) {
        this.path = MyFile.toRelative(path);
        this.time = time;
    }

    public SimpleMeta(String path) {
        this.path = MyFile.toRelative(path);
    }

    public String getPath() {
        return MyFile.toAbsolute(path);
    }

    public void setPath(String path) {
        this.path = MyFile.toRelative(path);
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
