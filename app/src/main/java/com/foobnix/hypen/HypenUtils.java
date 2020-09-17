package com.foobnix.hypen;

import com.foobnix.android.utils.LOG;

import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

public class HypenUtils {

    public static final String SHY = "&shy;";
    private static DefaultHyphenator instance = new DefaultHyphenator(HyphenPattern.error);

    public static void applyLanguage(String lang) {
        if (lang == null) {
            return;
        }
        try {
            if (lang.length() > 2) {
                lang = lang.substring(0, 2).toLowerCase(Locale.US);
            }
            if ("sp".equals(lang)) {
                lang = "es";
            }

            HyphenPattern pattern = HyphenPattern.error;

            HyphenPattern[] values = HyphenPattern.values();
            for (HyphenPattern p : values) {
                if (p.lang.equals(lang)) {
                    pattern = p;
                }
            }
            if (instance.pattern != pattern) {
                instance = new DefaultHyphenator(pattern);
            }
        } catch (Exception e) {
            LOG.e(e);
            instance = new DefaultHyphenator(HyphenPattern.error);
        }
        LOG.d("My-pattern-lang", instance.pattern.lang);

    }

    public static String applyHypnes(String htmlEncode) {
        String res = applyHypnesNewMy(htmlEncode);
        return res;
    }

    private static String applyHypnesNewMy(final String input) {
        if (input == null || input.length() == 0) {
            return "";
        }
        //LOG.d("applyHypnesNewMy-input",input);

        final StringBuilder res = new StringBuilder();

        tokenize(input, new TokensListener() {

            @Override
            public void findOther(char ch) {
                res.append(ch);
            }

            @Override
            public void findText(String w) {
                if (w.length() <= 3) {
                    res.append(w);
                } else {
                    try {
                        List<String> hyphenate = instance.hyphenate(w);
                        String join = join(hyphenate, SHY);
                        res.append(join);
                    } catch (Exception e) {
                        res.append(w);
                        LOG.e(e, "Exception findText", w);
                    }
                }
            }

        });

        String out = res.toString();
        //LOG.d("applyHypnesNewMy-out",out);

        return out;
    }

    public static String applyHypnesOld2(String input) {
        if (input == null || input.length() == 0) {
            return "";
        }
        // hack
        input = input.replace("<", " <").replace(">", "> ");

        StringTokenizer split = new StringTokenizer(input, " ", true);
        StringBuilder res = new StringBuilder();

        while (split.hasMoreTokens()) {
            String w = split.nextToken();

            if (w.equals(" ")) {
                res.append(" ");
                continue;
            }

            if (w.length() <= 3) {
                res.append(w);
                continue;
            }

            if (w.contains("<") || w.contains(">") || w.contains("=") || w.contains("&")) {
                res.append(w);
                continue;
            }

            char first = w.charAt(0);
            boolean startWithOther = false;
            if (!Character.isLetter(first)) {
                startWithOther = true;
                w = w.substring(1, w.length());
            }

            int endIndex = -1;
            String last = "";
            for (int i = w.length() / 2 + 1; i < w.length(); i++) {
                if (!Character.isLetter(w.charAt(i))) {
                    endIndex = i;
                    last = w.substring(endIndex);
                    w = w.substring(0, endIndex);
                    break;
                }
            }

            String result = null;
            if (w.contains("-")) {
                int find = w.indexOf("-");
                String p1 = w.substring(0, find);
                String p2 = w.substring(find + 1, w.length());
                result = join(instance.hyphenate(p1), SHY) + "-" + join(instance.hyphenate(p2), SHY);
                if (p2.contains("-")) {
                    result = result.replace("-" + SHY, "-");
                }

            } else {
                result = join(instance.hyphenate(w), SHY);
            }

            if (startWithOther) {
                result = String.valueOf(first) + result;
            }

            if (endIndex > 1) {
                result = result + last;
            }
            res.append(result);

        }

        String result = res.toString();
        result = result.replace(" <", "<").replace("> ", ">");
        return result;
    }

    public static String join(List<String> list, String delimiter) {
        if (list == null || list.size() == 0) {
            return "";
        }
        StringBuilder result = new StringBuilder(list.get(0));
        for (int i = 1; i < list.size(); i++) {
            result.append(delimiter).append(list.get(i));
        }
        String string = result.toString();
        return string;
        // return string.replace(SHY + SHY, SHY);
    }

    public static interface TokensListener {
        void findText(String text);

        void findOther(char ch);
    }

    static boolean ignore = false;
    static boolean ignore1 = false;

    public static void resetTokenizer() {
        ignore = false;
        ignore1 = false;
    }

    public static void tokenize(String in, TokensListener listener) {

        StringBuilder res = new StringBuilder();
        for (int i = 0; i < in.length(); i++) {
            char ch = in.charAt(i);

            if (ch == '<') {
                ignore1 = true;
            }
            if (ch == '>') {
                ignore1 = false;
            }
            if (ch == '&') {
                ignore = true;
            }
            if (ch == ';') {
                ignore = false;
            }

            if (ch == '_') {
                ignore = false;
            }

            if (ignore || ignore1) {
                if (res.length() > 0) {
                    listener.findText(res.toString());
                    res.setLength(0);
                }

                listener.findOther(ch);
                continue;
            }

            if (Character.isLetter(ch)) {
                res.append(ch);
            } else {
                if (res.length() > 0) {
                    listener.findText(res.toString());
                    res.setLength(0);
                }
                listener.findOther(ch);
            }

        }
        if (res.length() > 0) {
            listener.findText(res.toString());
            res.setLength(0);
        }

    }
}
