package mobi.librera.smartreflow;

import java.util.ArrayList;
import java.util.List;

import mobi.librera.smartreflow.model.Line;
import mobi.librera.smartreflow.model.Rect;
import mobi.librera.smartreflow.model.Word;

public class ImageUtils {


    public static Logger platformLogger = new PrintLnLogger();

    public static void setBackgroundColor(PlatformImage image, int[] color) {
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setPixel(x, y, color);
            }
        }
    }

    public static void removeWhiteBegin(PlatformImage img, Rect line) {
        for (int x = line.x1; x < line.x2; x++) {
            for (int y = line.y1; y < line.y2; y++) {
                if (ImageUtils.isBlackPixel(img, x, y)) {
                    line.x1 = x;
                    return;
                }
            }
        }
    }

    public static void removeWhiteEnd(PlatformImage img, Rect line) {
        for (int x = line.x2; x > line.x1; x--) {
            for (int y = line.y1; y < line.y2; y++) {
                if (ImageUtils.isBlackPixel(img, x, y)) {
                    line.x2 = x;
                    return;
                }
            }
        }
    }

    public static boolean isBlackPixel(PlatformImage image, int x, int y) {
        return image.getPixel(x, y)[0] < 180;

    }

    public static boolean isWhiteHorizontal(PlatformImage img, int y, int x1, int x2) {
        for (int x = x1; x < x2; x += 1) {
            if (ImageUtils.isBlackPixel(img, x, y)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isWhiteVertical(PlatformImage img, int x, int y1, int y2) {
        for (int y = y1; y < y2; y += 1) {
            if (ImageUtils.isBlackPixel(img, x, y)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isBlankVertical(PlatformImage img, int x, int y1, int y2, int dx) {
        boolean isBlank = true;
        for (int y = y1; y < y2; y += dx) {

            if (img.isBlackPixel(x, y)) {
                isBlank = false;
                break;
            }
        }
        return isBlank;
    }

    public static void drawRect(PlatformImage image, Rect r, int[] color) {
        for (int i = r.x1; i < r.x2; i++) {
            image.setPixel(i, r.y1, color);
            image.setPixel(i, r.y2, color);
        }
        for (int i = r.y1; i < r.y2; i++) {
            image.setPixel(r.x1, i, color);
            image.setPixel(r.x2, i, color);
        }
    }

    public static void drawWord(Rect w, int dx, int dy, PlatformImage input, PlatformImage output) {
        for (int i = w.x1; i < w.x2; i++) {
            for (int j = w.y1; j < w.y2; j++) {
                int pixel[] = input.getPixel(i, j);
                final int x = dx + (i - w.x1);
                final int y = dy + (j - w.y1);
                if(x<output.getWidth() && y< output.getHeight()) {
                    output.setPixel(x, y, pixel);
                }
            }
        }

    }


    public static void drawRect(PlatformImage image, int x, int y, int x1, int y1, int[] color) {
        for (int i = x; i < x1; i++) {
            image.setPixel(i, y, color);
            image.setPixel(i, y1, color);
        }
        for (int i = y; i < y1; i++) {
            image.setPixel(x, i, color);
            image.setPixel(x1, i, color);
        }
    }

    public static void copyRect(PlatformImage input, PlatformImage output, int x, int y, int x1, int y1) {
        for (int i = x; i < x1; i++) {
            for (int j = y; j < y1; j++) {
                final int[] pixel = input.getPixel(i, j);
                output.setPixel(i, j, pixel);
            }
        }
    }

    public static void log(Object... strings) {
        StringBuilder s = new StringBuilder();
        for (Object str : strings) {
            s.append(str + "|");
        }
        platformLogger.log(s.toString());
    }

    public static List<Rect> splitHorizontal(PlatformImage img, Rect rect, int minLineHeight) {
        Rect line = null;
        List<Rect> rects = new ArrayList<>();
        boolean prevWhite = true;
        for (int y = rect.y1; y < rect.y2; y += 1) {
            boolean isWhite = ImageUtils.isWhiteHorizontal(img, y, rect.x1, rect.x2);

            if (prevWhite && !isWhite) {
                line = new Line(rect.x1, y);
            }
            if (isWhite && !prevWhite) {
                if (y - line.y1 > minLineHeight) {
                    line.x2 = rect.x2;
                    line.y2 = y;
                    rects.add(line);
                    line = null;
                }
            }
            prevWhite = isWhite;
        }
        if (line != null) {
            line.x2 = rect.x2;
            line.y2 = rect.y2;
            rects.add(line);

        }
        return rects;
    }

    public static List<Word> splitVertical(PlatformImage img, Rect rect, int minSpaceSize) {
        Word line = null;
        List<Word> rects = new ArrayList<>();

        boolean prevWhite = true;
        int countWhite = 0;
        for (int x = rect.x1; x < rect.x2; x += 1) {
            boolean isWhite = ImageUtils.isWhiteVertical(img, x, rect.y1, rect.y2);
            if (isWhite) {
                countWhite++;
                if (countWhite < minSpaceSize) {
                    continue;
                }
            }
            countWhite = 0;


            if (prevWhite && !isWhite) {
                line = new Word(x, rect.y1);

            }
            if (isWhite && !prevWhite) {
                line.y2 = rect.y2;
                line.x2 = x;
                rects.add(line);
                line = null;

            }
            prevWhite = isWhite;
        }
        if (line != null) {
            line.y2 = rect.y2;
            line.x2 = rect.x2;
            rects.add(line);
        }

        return rects;
    }


    public abstract static class Logger {
        public abstract void log(String str);
    }

    public static class PrintLnLogger extends Logger {

        @Override
        public void log(String str) {
            System.out.println(str);
        }
    }


}
