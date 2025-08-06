package mobi.librera.smartreflow.model;

public class Line extends Rect {

    public int columnNumber;

    public Line() {

    }

    public Line(int x, int y) {
        super(x, y);
    }

    public Line(int x, int y, int x1, int y1) {
        super(x, y, x1, y1);
    }
}