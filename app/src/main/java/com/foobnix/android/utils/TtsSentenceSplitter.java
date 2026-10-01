package com.foobnix.android.utils;

import java.util.regex.Pattern;

/** Keeps sentence-ending punctuation and closing quotation marks in the same utterance. */
public final class TtsSentenceSplitter {
    private static final Pattern SHORT_TITLES = Pattern.compile(
            "(?<![\\p{L}\\p{N}])((?i:Mr|Mrs|Ms|Dr|Prof|Sr|Jr|St|vs))\\.(?![\\p{L}\\p{N}])");
    // I is usually the pronoun at a sentence ending, rather than a name initial.
    private static final Pattern INITIAL = Pattern.compile(
            "(?<![\\p{L}\\p{N}])([\\p{Lu}&&[^I]])\\.(?=\\s+\\p{Lu})");

    /** Protect titles and initials without hiding sentence endings such as "up." or "me.". */
    public static String protectShortAbbreviations(String text) {
        text = SHORT_TITLES.matcher(text).replaceAll("$1{dot}");
        return INITIAL.matcher(text).replaceAll("$1{dot}");
    }

    private TtsSentenceSplitter() { }

    public static String insertPauses(String text, String delimiters, String pause) {
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            result.append(c);
            if (delimiters.indexOf(c) < 0) continue;
            // Keep consecutive punctuation together as well, e.g. "What?!".
            while (i + 1 < text.length() && delimiters.indexOf(text.charAt(i + 1)) >= 0) {
                result.append(text.charAt(++i));
            }
            int end = afterClosingQuotes(text, i + 1);
            result.append(text, i + 1, end);
            i = end - 1;
            result.append(pause).append(' ');
        }
        return result.toString();
    }

    public static int afterClosingQuotes(String text, int start) {
        int end = start;
        while (end < text.length() && isClosingQuote(text.charAt(end))) end++;
        return end;
    }

    private static boolean isClosingQuote(char c) {
        return c == '\'' || c == '"' || c == '\u2019' || c == '\u201D'
                || c == '\u00BB' || c == '\u203A';
    }
}
