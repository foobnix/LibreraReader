package com.foobnix.model;

public class SimpleMeta implements MyPath.RelativePath {

    public static int STATE_NORMAL = 0;
    public static int STATE_DELETE = 1;


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

    public static SimpleMeta SyncSimpleMeta(SimpleMeta s) {
        return new SimpleMeta(MyPath.getSyncPath(s.getPath()), s.time);
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

    @Override
    public String toString() {
        return "SimpleMeta:" + path + ":" + time;

    }
}
