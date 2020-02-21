package mobi.librera.smartreflow;

import java.util.ArrayList;
import java.util.List;

import mobi.librera.smartreflow.model.Column;
import mobi.librera.smartreflow.model.Line;
import mobi.librera.smartreflow.model.Word;

public class SmartReflow implements SmartReflowInterface {

    public static final int MIN_WORD_SIZE = 5;
    public static final int MIN_LINE_HEIGHT = 10;

    public static int PADDING = 15;

    public boolean isDrawColums;
    public boolean isDrawLines;
    public boolean isDrawWords;
    public boolean isDrawWordsOffsetLeft;
    public boolean isDrawChars;
    public boolean isDrawResultUsingWords = true;
    float averageTop = 0;
    int averageTopCount = 0;
    List<Column> columns = new ArrayList();
    List<Line> lines = new ArrayList();
    List<Word> words = new ArrayList<Word>();
    List<Word> wordsLong = new ArrayList<Word>();
    boolean isDrawResult = true;

    PlatformImage image;

    public SmartReflow(PlatformImage image) throws Exception {
        this.image = image;
        process(image);
    }

    public boolean isMultyColumn() {
        return columns.size() >= 2;
    }


    public List<String> getStatistics() {
        List<String> list = new ArrayList<>();
        list.add("Columns :" + columns.size());
        list.add("Lines   :" + lines.size());
        list.add("Words   :" + words.size());
        list.add("Padding :" + PADDING);
        int sum = 0;
        for (Word w : wordsLong) {
            sum += w.offsetLeft;
        }
        list.add("Mid offset :" + sum / wordsLong.size());

        return list;
    }


    @Override
    public void process(PlatformImage img) throws Exception {

        ImageUtils.log("img", "w", img.getWidth(), "h", img.getHeight());
        PADDING = img.getWidth() / 200;
        System.out.println("PADDING " + PADDING + " k= " + (int) img.getWidth() / 15);

        columns.clear();
        lines.clear();
        words.clear();

        //detect colums
        boolean isColumn = true;

        int center = img.getWidth() / 2;
        for (int dx = -PADDING / 2; dx < PADDING / 2; dx++) {
            for (int y = 0; y < img.getHeight(); y++) {
                if (img.isBlackPixel(center + dx, y)) {
                    isColumn = false;
                }
            }
            if (isColumn) {
                break;
            }
        }

        Column column = null;
        int number = 0;

        for (int x = 0; x < img.getWidth(); x++) {
            isColumn = false;
            for (int y = 0; y < img.getHeight(); y++) {
                if (img.isBlackPixel(x, y)) {
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
                column.y2 = img.getHeight() - 1;
                columns.add(column);
                column = null;
            }
        }
        System.out.println("Columns size" + columns.size());

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
            proccessColumn(img, col);
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
        if (word != null) {
            wordsLong.add(prevCh);
        }

    }


    public void proccessColumn(PlatformImage img, Column col) {
        averageTopCount = 0;
        averageTop = 0;


        //detect lines
        Line findLine = null;
        for (int y = col.y1; y < col.y2; y++) {

            boolean isBank = isBlackHorizontal(img, y, col.x1, col.x2);

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
                boolean isBlank = isBlankVertical(img, x, line.y1, line.y2);


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
        averageTop = averageTop / averageTopCount;
    }


    @Override
    public void drawObjects(PlatformImage output) {
        output.create(image.getWidth(), image.getHeight());

        ImageUtils.log("Columns:", columns.size());
        ImageUtils.log("Lines:", lines.size());
        ImageUtils.log("Words:", words.size());

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                output.setPixel(x, y, image.getPixel(x, y));
            }
        }

        if (isDrawColums) {
            for (Column col : columns) {
                ImageUtils.drawRect(output, col.x1, col.y1, col.x2, col.y2, PlatformImage.GREEN);
            }
        }

        if (isDrawLines) {
            for (Line l : lines) {
                ImageUtils.drawRect(output, l.x1, l.y1, l.x2, l.y2, PlatformImage.RED);
            }
        }


        for (Word w : words) {
            if (isDrawChars) {
                ImageUtils.drawRect(output, w.x1, w.y1, w.x2, w.y2, PlatformImage.BLUE);
            }
            int dy = w.y1;
            ImageUtils.drawRect(output, w.x1 - w.offsetLeft, dy, w.x1, dy + 2, PlatformImage.YELLOW);

        }


        for (Word w : wordsLong) {
            ImageUtils.drawRect(output, w.x1, w.y1, w.x2, w.y2, PlatformImage.MAROON);
        }

    }


    @Override
    public void reflow(PlatformImage des) {
        if (columns.isEmpty()) {
            des.create(image.getWidth(), image.getHeight());
            for (int x = 0; x < des.getWidth(); x++) {
                for (int y = 0; y < des.getHeight(); y++) {
                    des.setPixel(x, y, image.getPixel(x, y));
                }
            }

            return;
        }

        ImageUtils.setBackgroundColor(des, PlatformImage.WHITE);


        int dx = PADDING;
        int dy = 0;

        Word prev = new Word();

        List<Word> input;


        boolean isDrawResult = true;
        //if (isDrawResultUsingWords) {
        if (isDrawResultUsingWords) {
            input = wordsLong;
        } else {
            input = words;
        }
        for (Word w : input) {
            if (w.height() > des.getHeight()) {
                //w.y2 = w.y2 / 2;
            }
            if (w.width() > des.getWidth()) {
                //w.x2 = w.x2
            }
            dx = dx + w.offsetLeft;

            if (w.height() != prev.height() && prev.height() > 0) {
                dy -= w.height() - prev.height();
            }

            //chapter
            if (w.isFirstWord) {
                //w.offsetTop = PADDING;
                //ImageUtils.log("w.offsetLeft", w.offsetLeft);
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

            try {
                drawWordAt(w, dx, dy, image, des);
                dx += w.width();
                prev = w;
            } catch (Exception e) {
                //e.printStackTrace();
                //continue;
                break;
            }
        }


    }

    public void drawWordAt(Word w, int dx, int dy, PlatformImage input, PlatformImage output) {
        for (int i = w.x1; i < w.x2; i++) {
            for (int j = w.y1; j < w.y2; j++) {
                int pixel[] = input.getPixel(i, j);
                final int x = dx + (i - w.x1);
                final int y = dy + (j - w.y1);

                output.setPixel(x, y, pixel);


            }
        }

    }


    public boolean isBlackHorizontal(PlatformImage img, int y, int x1, int x2) {
        boolean isBlank = true;

        for (int x = x1; x < x2; x++) {

            if (img.isBlackPixel(x, y)) {
                isBlank = false;
                break;
            }
        }
        return isBlank;
    }


    public boolean isBlankVertical(PlatformImage img, int x, int y1, int y2) {
        boolean isBlank = true;
        for (int y = y1; y < y2; y++) {

            if (img.isBlackPixel(x, y)) {
                isBlank = false;
                break;
            }
        }
        return isBlank;
    }




}
