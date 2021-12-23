import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

public class LibreraSmartReflow {

    public static final int MIN_WORD_SIZE = 5;
    public static final int MIN_LINE_HEIGHT = 10;


    public static double[] RED = new double[]{255, 0, 0};
    public static double[] GREEN = new double[]{0, 255, 0};
    public static double[] BLUE = new double[]{0, 0, 255};
    public static double[] WHITE = new double[]{255, 255, 255};
    public static double[] YELLOW = new double[]{128, 255, 0};
    public static double[] MAROON = new double[]{128, 0, 0};


    public static final int WHITE_INT = 255;
    public static final int BLACK_INT = 0;

    public static int PADDING = 15;
    public boolean isDrawColums;
    public boolean isDrawLines;
    public boolean isDrawWords;
    public boolean isDrawWordsOffsetLeft;
    public boolean isDrawChars;
    public boolean isDrawResultUsingWords;


    boolean isDrawResult = true;


    String path;
    BufferedImage img;
    Raster imgData;

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    int width;
    int height;

    public LibreraSmartReflow(String path) throws IOException {
        process(path);
    }

    public Image getOriginalImage() {
        return img;
    }

    ArrayList<Column> columns = new ArrayList();
    ArrayList<Line> lines = new ArrayList();
    ArrayList<Word> words = new ArrayList<Word>();
    ArrayList<Word> wordsLong = new ArrayList<Word>();


    float averageTop = 0;
    int averageTopCount = 0;


    public void process(String path) throws IOException {

        this.path = path;
        if (!new File(path).exists()) {
            return;
        }

        img = ImageIO.read(new File(path));
        imgData = img.getData();

        width = img.getWidth();
        height = img.getHeight();

        PADDING = width / 200;
        System.out.println("PADDING " + PADDING + " k= " + (int) width / 15);

        columns.clear();
        lines.clear();
        words.clear();

        //detect colums
        boolean isColumn = true;

        int center = width / 2;
        for (int dx = -PADDING / 2; dx < PADDING / 2; dx++) {
            for (int y = 0; y < height; y++) {
                double p = onePixelAt(center + dx, y);
                if (p == BLACK_INT) {
                    isColumn = false;
                }
            }
            if (isColumn) {
                break;
            }
        }

        Column column = null;
        int number = 0;
        for (int x = 0; x < width; x++) {
            isColumn = false;
            for (int y = 0; y < height; y++) {
                double p = onePixelAt(x, y);
                if (p == BLACK_INT) {
                    isColumn = true;
                }
                if (isColumn && column == null) {
                    column = new Column();
                    column.number = number++;
                    column.x1 = x;
                    column.y1 = 0;
                    break;
                }
            }
            if (column != null && !isColumn) {
                column.x2 = x;
                column.y2 = height - 1;
                columns.add(column);
                column = null;
                log("Column added");

            }
        }
        log("Columns size", columns.size());

        for (Column col : columns) {
//            while (isLineWhite(col.y1, col.x1, col.x2)) {
//                col.y1++;
//            }
//
//            while (isLineWhite(col.y2, col.x1, col.x2)) {
//                col.y2--;
//            }
        }

        for (Column col : columns) {
            proccessColumn(col);
        }


        Word word = null;

        Word prevCh = new Word();
        for (Word ch : words) {
            if (ch.isFirstWord || ch.offsetLeft > PADDING) {
                if (word == null) {
                    word = new Word();

                    word.isFirstWord = ch.isFirstWord;
                    word.offsetLeft = ch.offsetLeft;
                    word.offsetTop = ch.offsetTop;

                    word.x1 = ch.x1;
                    word.y1 = ch.y1;
                } else if (word != null) {
                    word.y2 = prevCh.y2;
                    word.x2 = prevCh.x2;

                    wordsLong.add(word);

                    word = new Word();
                    word.x1 = ch.x1;
                    word.y1 = ch.y1;

                    word.isFirstWord = ch.isFirstWord;
                    word.offsetLeft = ch.offsetLeft;
                    word.offsetTop = ch.offsetTop;

                }
            } else if (ch.isLastWord) {
                word.y2 = ch.y2;
                word.x2 = ch.x2;
                wordsLong.add(word);
                word = null;
            }
            prevCh = ch;


        }

    }

    public void proccessColumn(Column col) {
        averageTopCount = 0;
        averageTop = 0;


        //detect lines
        Line findLine = null;
        for (int y = col.y1; y < col.y2; y++) {

            boolean isBank = isBlankHorizontal(y, col.x1, col.x2);

            if (!isBank && findLine == null) {
                findLine = new Line();
                findLine.columnNumber = col.number;
                findLine.y1 = y;
                findLine.x1 = col.x1;
            } else if (isBank && findLine != null) {
                findLine.y2 = y;
                findLine.x2 = col.x2;
                if (findLine.height() > MIN_LINE_HEIGHT) {
                    lines.add(findLine);
                    findLine = null;
                }
            }

        }
        if (findLine != null) {
            findLine.y2 = col.y2;
            findLine.x2 = col.x2;
            lines.add(findLine);
        }

        //dect words
        Line prevLine = new Line();
        prevLine.y2 = col.y1;

        for (Line line : lines) {
            if (line.columnNumber != col.number) {
                continue;
            }

            Word word = null;
            Word prevWord = new Word();
            prevWord.x2 = col.x1;

            boolean isFirstWord = true;
            for (int x = col.x1; x < col.x2; x++) {
                boolean isBlank = isBlankVertical(x, line.y1, line.y2);


                if (!isBlank && word == null) {
                    word = new Word();
                    word.x1 = x;
                    word.y1 = line.y1;

                    word.isFirstWord = isFirstWord;


                    word.offsetLeft = word.x1 - prevWord.x2;
                    word.offsetTop = line.y1 - prevLine.y2;

                    if (isFirstWord) {
                        averageTop += word.offsetTop;
                        averageTopCount++;
                    }

                    isFirstWord = false;

                } else if (isBlank && word != null) {
                    word.x2 = x;
                    word.y2 = line.y2;

                    if (word.width() > MIN_WORD_SIZE || word.height() > MIN_WORD_SIZE) {
                        words.add(word);
                        prevWord = word;
                    }
                    word = null;
                }
            }
            if (word != null) {
                word.x2 = col.x2;
                word.y2 = line.y2;
                words.add(word);
            }
            words.get(words.size() - 1).isLastWord = true;
            prevLine = line;
        }
        log("averageTop", averageTop, averageTopCount);
        averageTop = averageTop / averageTopCount;
    }

    public static void log(Object... args) {
        StringBuilder out = new StringBuilder();
        for (Object arg : args) {
            out.append(arg);
            out.append("|");
        }
        System.out.println(out.toString());

    }

    public Image getReflowed(int width, int height) {
        if (img == null) {
            return null;
        }

        BufferedImage res = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        WritableRaster des = res.getData().createCompatibleWritableRaster();

        for (int x = 0; x < des.getWidth(); x++) {
            for (int y = 0; y < des.getHeight(); y++) {
                des.setPixel(x, y, WHITE);
            }
        }

        int dx = PADDING;
        int dy = 0;

        Word prev = new Word();

        ArrayList<Word> input;
        if (isDrawResultUsingWords) {
            input = wordsLong;
        } else {
            input = words;
        }
        for (Word w : input) {
            dx = dx + w.offsetLeft;

            if (w.height() != prev.height() && prev.height() > 0) {
                dy -= w.height() - prev.height();
            }

            //chapter
            if (w.isFirstWord) {
                log("w.offsetLeft", w.offsetLeft);
            }
            if (w.isFirstWord && (w.offsetLeft > PADDING * 2 || w.offsetTop > PADDING + 5)) {
                dx = w.offsetLeft + PADDING;
                dy += w.height() + w.offsetTop;
            } else if (w.isFirstWord) {
                dx += Math.max(w.offsetLeft, PADDING);
            }

            //new line
            if (w.width() + dx > des.getWidth() - PADDING) {
                dx = PADDING;
                dy += w.height() + averageTop;
            }
            drawWordAt(w, dx, dy, des);
            dx += w.width();

            prev = w;

        }

        BufferedImage res2 = new BufferedImage(width, dy + PADDING * 3, BufferedImage.TYPE_INT_RGB);

        res2.setData(des);

        return res2;
    }

    public Image getObjects() {
        if (img == null) {
            return null;
        }
        BufferedImage objectsImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        WritableRaster outData = objectsImage.getData().createCompatibleWritableRaster();


        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                outData.setPixel(x, y, filterPixel(pixelAt(x, y, imgData)));
            }
        }

        if (isDrawColums) {
            for (Column col : columns) {
                drawRect(col.x1, col.y1, col.x2, col.y2, GREEN, outData);
            }
        }

        if (isDrawLines) {
            for (Line l : lines) {
                drawRect(l.x1, l.y1, l.x2, l.y2, RED, outData);
            }
        }

        if (isDrawChars || isDrawWordsOffsetLeft) {
            for (Word w : words) {
                if (isDrawChars) {
                    drawRect(w.x1, w.y1, w.x2, w.y2, BLUE, outData);
                }
                int dy = w.y1;
                if (isDrawWordsOffsetLeft) {
                    drawRect(w.x1 - w.offsetLeft, dy, w.x1, dy + 2, YELLOW, outData);
                }
            }
        }
        if (isDrawWords) {
            for (Word w : wordsLong) {
                drawRect(w.x1, w.y1, w.x2, w.y2, MAROON, outData);
            }
        }


        objectsImage.setData(outData);

        return objectsImage;
    }

    public void drawWordAt(Word w, int dx, int dy, WritableRaster des) {
        try {
            for (int i = w.x1; i < w.x2; i++) {
                for (int j = w.y1; j < w.y2; j++) {
                    double pixel[] = pixelAt(i, j, imgData);
                    des.setPixel(dx + (i - w.x1), dy + (j - w.y1), pixel);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void drawArea(int x, int y, int x1, int y1, Raster in, WritableRaster des) {
        for (int i = x; i < x1; i++) {
            for (int j = y; j < y1; j++) {
                des.setPixel(i, j, pixelAt(i, j, in));
            }
        }
    }

    public void drawRect(int x, int y, int x1, int y1, double[] color, WritableRaster des) {
        for (int i = x; i < x1; i++) {
            des.setPixel(i, y, color);
            des.setPixel(i, y1, color);
        }
        for (int i = y; i < y1; i++) {
            des.setPixel(x, i, color);
            des.setPixel(x1, i, color);
        }

    }


    public boolean isBlankHorizontal(int y, int x1, int x2) {
        boolean isBlank = true;

        for (int x = x1; x < x2; x++) {

            double p = onePixel(pixelAt(x, y, imgData));

            if (p == BLACK_INT) {
                isBlank = false;
                break;
            }
        }
        return isBlank;
    }


    public boolean isBlankVertical(int x, int y1, int y2) {
        boolean isBlank = true;
        for (int y = y1; y < y2; y++) {
            double p = onePixel(pixelAt(x, y, imgData));

            if (p == BLACK_INT) {
                isBlank = false;
                break;
            }
        }
        return isBlank;
    }

    static class Line extends Rect {
        int columnNumber;
    }

    static class Column extends Rect {
        int number;
    }

    static class Word extends Rect {
        public int offsetLeft;
        public int offsetTop;
        public boolean isFirstWord;
        public boolean isLastWord;
    }


    static class Rect {
        public int x1;
        public int y1;
        public int x2;
        public int y2;

        public int width() {
            return x2 - x1;
        }

        public int height() {
            return y2 - y1;
        }
    }


    public double[] pixelAt(int x, int y, Raster data) {
        double[] pixel = new double[4];
        data.getPixel(x, y, pixel);
        return pixel;
    }

    public double[] filterPixel(double[] pixel) {
        double p = onePixel(pixel);
        pixel[0] = pixel[1] = pixel[2] = p;
        return pixel;
    }

    public double onePixel(double[] pixel) {
        double p = pixel[0];
        p = p > 180 ? WHITE_INT : BLACK_INT;
        return p;
    }

    public double onePixelAt(int x, int y) {
        double[] pixel = new double[4];
        imgData.getPixel(x, y, pixel);
        return onePixel(pixel);
    }

    public boolean isLineWhite(int dy, int x1, int x2) {
        boolean isWhite = true;
        for (int x = x1; x < x2; x++) {
            double p = onePixelAt(x, dy);
            if (p == BLACK_INT) {
                return false;
            }
        }
        return true;
    }


}
