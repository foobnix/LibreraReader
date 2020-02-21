package mobi.librera.smartreflow.model;

public class Rect {
    public int x1;
    public int y1;
    public int x2;
    public int y2;

    public Rect() {

    }

    public Rect(int x, int y) {
        this.x1 = x;
        this.y1 = y;
    }

    public Rect(int x1, int y1, int x2, int y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public int width() {
        return x2 - x1;
    }

    public int height() {
        return y2 - y1;
    }
}