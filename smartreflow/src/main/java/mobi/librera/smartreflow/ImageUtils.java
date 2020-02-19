package mobi.librera.smartreflow;

public class ImageUtils {


   public static Logger platformLogger = new PrintLnLogger();

    public static void setBackgroundColor(PlatformImage image, int[] color) {
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                image.setPixel(x, y, color);
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
