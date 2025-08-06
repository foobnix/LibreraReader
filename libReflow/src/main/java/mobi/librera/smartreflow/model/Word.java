package mobi.librera.smartreflow.model;

public class Word extends Rect {
    public int offsetLeft;
    public int offsetTop;
    public boolean isFirstWord;
    public boolean isLastWord;

    public Word() {
    }

    public Word(int x, int y) {
        super(x, y);
    }


}