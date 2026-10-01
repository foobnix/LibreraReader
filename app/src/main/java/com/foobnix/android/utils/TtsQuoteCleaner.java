package com.foobnix.android.utils;

import java.util.Locale;

/** Removes detached opening quotes while retaining closing quotes and word apostrophes. */
public final class TtsQuoteCleaner {
    private TtsQuoteCleaner() { }

    public static String clean(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '\'' && c != '\u2018' && c != '\u2019') {
                result.append(c);
                continue;
            }
            boolean afterPause = i >= TxtUtils.TTS_PAUSE.length()
                    && text.regionMatches(i - TxtUtils.TTS_PAUSE.length(), TxtUtils.TTS_PAUSE,
                            0, TxtUtils.TTS_PAUSE.length());
            boolean closingQuote = isClosingDialogueQuote(text, i);
            // Replacement rules may insert whitespace between punctuation and its closing quote.
            if (closingQuote) {
                while (result.length() > 0 && Character.isWhitespace(result.charAt(result.length() - 1))) {
                    result.setLength(result.length() - 1);
                }
            }
            if ((!afterPause && i > 0 && Character.isLetterOrDigit(text.charAt(i - 1)))
                    || closingQuote || isLeadingApostrophe(text, i + 1)) {
                result.append(c);
            }
        }
        return result.toString();
    }

    private static boolean isClosingDialogueQuote(String text, int index) {
        if (text.charAt(index) == '\u2018') return false;
        int previous = index - 1;
        while (previous >= 0 && (Character.isWhitespace(text.charAt(previous))
                || "'\"’”»›".indexOf(text.charAt(previous)) >= 0)) previous--;
        if (previous < 0 || ".!?;:,".indexOf(text.charAt(previous)) < 0) return false;
        int next = index + 1;
        return next == text.length() || Character.isWhitespace(text.charAt(next))
                || "'\"’”»›".indexOf(text.charAt(next)) >= 0
                || text.startsWith(TxtUtils.TTS_PAUSE, next);
    }

    private static boolean isLeadingApostrophe(String text, int start) {
        if (start == text.length()) return false;
        if (Character.isDigit(text.charAt(start))) return true; // '90s
        int end = start;
        while (end < text.length() && Character.isLetter(text.charAt(end))) end++;
        String word = text.substring(start, end).toLowerCase(Locale.ROOT);
        return word.equals("tis") || word.equals("twas") || word.equals("twere")
                || word.equals("twill") || word.equals("twould") || word.equals("em")
                || word.equals("cause") || word.equals("bout") || word.equals("til")
                // A contraction may begin on this page and follow its stem on the previous one.
                || word.equals("m") || word.equals("re") || word.equals("ve")
                || word.equals("ll") || word.equals("d") || word.equals("s") || word.equals("t");
    }
}
