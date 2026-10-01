package com.foobnix.tts;

import com.foobnix.android.utils.TtsSentenceSplitter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Identifies one page's speech, including offsets in the text sent to the engine. */
final class SpeechRequest {
    private static final AtomicLong nextId = new AtomicLong();
    private final String prefix = "page-" + nextId.incrementAndGet() + ":";
    private final Map<String, Integer> boundaries = new ConcurrentHashMap<>();
    final int pageBoundary;

    SpeechRequest(int pageBoundary) {
        this.pageBoundary = pageBoundary;
    }

    String id(String signal) { return prefix + signal; }

    String signal(String id) {
        return id != null && id.startsWith(prefix) ? id.substring(prefix.length()) : null;
    }

    void register(String signal, int boundary, int length) {
        if (boundary < length) boundaries.put(signal, boundary);
    }

    boolean reachesPage(String signal, int end) {
        Integer boundary = boundaries.get(signal);
        return boundary != null && end > boundary;
    }

    // Keep unsupported engines' fallback within the first sentence on the new page.
    static int firstSentenceEnd(String text, int boundary) {
        for (int i = boundary; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?') {
                int end = i + 1;
                while (end < text.length() && ".!?".indexOf(text.charAt(end)) >= 0) end++;
                end = TtsSentenceSplitter.afterClosingQuotes(text, end);
                if (end == text.length() || Character.isWhitespace(text.charAt(end))) return end;
            }
        }
        return text.length();
    }
}
