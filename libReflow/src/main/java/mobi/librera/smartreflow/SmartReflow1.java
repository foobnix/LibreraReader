package mobi.librera.smartreflow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import mobi.librera.smartreflow.model.Rect;
import mobi.librera.smartreflow.model.Word;

public class SmartReflow1 implements SmartReflowInterface {


    PlatformImage img;
    List<Rect> lines = new ArrayList<Rect>();
    List<Word> words = new ArrayList<Word>();

    int minLineHeight = 5;
    int minSpaceSize = 5;
    boolean isTwoColumns = true;

    @Override
    public void process(PlatformImage img) throws Exception {
        this.img = img;

        lines.clear();
        words.clear();


        int center = img.getWidth() / 2;
        for (int dx = -minSpaceSize; dx < minSpaceSize; dx++) {
            for (int y = 0; y < img.getHeight(); y++) {
                if (img.isBlackPixel(center + dx, y)) {
                    isTwoColumns = false;
                    break;
                }
            }
            if (isTwoColumns) {
                break;
            }
        }
        if (isTwoColumns) {
            lines.addAll(ImageUtils.splitHorizontal(img, new Rect(0, 0, img.getWidth() / 2, img.getHeight() - 1), minLineHeight));
            lines.addAll(ImageUtils.splitHorizontal(img, new Rect(img.getWidth() / 2, 0, img.getWidth() - 1, img.getHeight() - 1), minLineHeight));
        } else {
            lines.addAll(ImageUtils.splitHorizontal(img, new Rect(0, 0, img.getWidth() - 1, img.getHeight() - 1), minLineHeight));
        }


        //accurate lines
        for (Rect l : lines) {
            ImageUtils.removeWhiteBegin(img, l);
            ImageUtils.removeWhiteEnd(img, l);
        }

        for (Rect l : lines) {
            final List<Word> all = ImageUtils.splitVertical(img, l, minSpaceSize);
            if (!all.isEmpty()) {
                final Word first = all.get(0);

                first.isFirstWord = true;
            }
            words.addAll(all);
        }

        for (Rect l : words) {
            ImageUtils.removeWhiteBegin(img, l);
            ImageUtils.removeWhiteEnd(img, l);
        }
    }

    @Override
    public void drawObjects(PlatformImage out) {
        out.create(img.getWidth(), img.getHeight());
        ImageUtils.copyRect(img, out, 0, 0, img.getWidth(), img.getHeight());
        for (Rect r : lines) {
            ImageUtils.drawRect(out, r.x1, r.y1, r.x2, r.y2, PlatformImage.YELLOW);
        }
        for (Rect r : words) {
            ImageUtils.drawRect(out, r.x1, r.y1, r.x2, r.y2, PlatformImage.BLUE);
        }


    }

    @Override
    public void reflow(PlatformImage out) {
        if (isTwoColumns) {
            //out.create(img.getWidth() / 2, img.getHeight() * 2);
        } else {
            //out.create(img.getWidth() / 2, img.getHeight());
        }

        ImageUtils.setBackgroundColor(out, PlatformImage.WHITE);


        Word prev = words.get(0);

        int padding = 10;
        int indent = 5;
        int space = 10;


        int dx = padding;
        int dy = padding;

        for (Word w : words) {

            if (w.isFirstWord) {
                int dh = w.y1 - prev.y2;

                if (dh > prev.height() ) {
                    dy += dh + prev.height();
                    dx = padding;
                }
            }


            if (Math.abs(w.height() - prev.height()) > prev.height() / 2) {
                dx = padding;
                dy += prev.height();
            }

            if (dx + w.width() + padding > out.getWidth()) {
                dx = padding;
                dy += prev.height() + indent;
            }
            if (dy + w.height() + padding > out.getHeight()) {
                break;
            }

            ImageUtils.drawWord(w, dx, dy, img, out);
            dx = dx + w.width() + space;
            prev = w;
        }

    }

    @Override
    public List<String> getStatistics() {
        return Arrays.asList("Lines: " + lines.size());
    }


}
