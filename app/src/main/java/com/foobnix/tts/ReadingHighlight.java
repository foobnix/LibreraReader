package com.foobnix.tts;

import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;

import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.AnnotationsFinder;
import com.foobnix.pdf.search.activity.msg.InvalidateMessage;

import org.ebookdroid.droids.mupdf.codec.TextWord;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The sentence TTS is reading, for the page to mark.
 *
 * What is spoken has been through the TTS replacements, so it is found among the page's words by
 * the letters at its start, as an anchor is. From the last sentence back: a short one that is also
 * earlier on the page then cannot take the earlier place, and the one carried over from the page
 * before, which is not all here, is what is left at the top.
 */
public final class ReadingHighlight {

    private static final int PREFIX = 24;
    private static final Handler handler = new Handler(Looper.getMainLooper());

    private static List<List<RectF>> sentences = Collections.emptyList();
    private static volatile int page = -1;
    private static volatile List<RectF> shown = Collections.emptyList();

    private ReadingHighlight() {
    }

    /** @param text what is about to be spoken, split as TTSEngine splits it */
    public static synchronized void prepare(final int pageNumber, final TextWord[][] lines, final String text) {
        clear();
        if (lines == null || lines.length == 0 || text == null || !text.contains(TxtUtils.TTS_PAUSE)) {
            sentences = Collections.emptyList();
            return;
        }
        final List<TextWord> words = new ArrayList<TextWord>();
        final StringBuilder letters = new StringBuilder();
        final List<Integer> offsets = new ArrayList<Integer>();
        for (TextWord[] line : lines) {
            for (TextWord word : line) {
                if (word != null && word.w != null) {
                    words.add(word);
                    offsets.add(letters.length());
                    letters.append(TxtUtils.lettersOf(word.w));
                }
            }
        }
        final String[] parts = text.split(TxtUtils.TTS_PAUSE);
        final int[] starts = new int[parts.length];
        int end = letters.length();
        for (int i = parts.length - 1; i >= 0; i--) {
            final String part = TxtUtils.lettersOf(parts[i]);
            final String prefix = part.substring(0, Math.min(PREFIX, part.length()));
            starts[i] = prefix.isEmpty() ? -1 : letters.lastIndexOf(prefix, end - prefix.length());
            if (starts[i] >= 0) {
                end = starts[i];
            }
        }
        final List<List<RectF>> result = new ArrayList<List<RectF>>();
        int from = 0;
        for (int i = 0; i < parts.length; i++) {
            final int start = starts[i] >= 0 ? starts[i] : from;
            int next = letters.length();
            for (int j = i + 1; j < parts.length; j++) {
                if (starts[j] >= 0) {
                    next = starts[j];
                    break;
                }
            }
            final List<TextWord> run = new ArrayList<TextWord>();
            for (int w = 0; w < words.size(); w++) {
                if (offsets.get(w) >= start && offsets.get(w) < next) {
                    run.add(words.get(w));
                }
            }
            result.add(AnnotationsFinder.mergeLines(run));
            from = next;
        }
        sentences = result;
        page = pageNumber;
    }

    /** @param index the sentence, as TTSEngine numbers its parts */
    public static synchronized void show(final int index) {
        if (index >= 0 && index < sentences.size()) {
            shown = sentences.get(index);
            redraw();
        }
    }

    public static void clear() {
        if (!shown.isEmpty()) {
            shown = Collections.emptyList();
            redraw();
        }
    }

    /** What to mark on a page, 0-based. */
    public static List<RectF> on(final int pageNumber) {
        return pageNumber == page ? shown : Collections.<RectF>emptyList();
    }

    private static void redraw() {
        handler.post(() -> EventBus.getDefault().post(new InvalidateMessage()));
    }
}
