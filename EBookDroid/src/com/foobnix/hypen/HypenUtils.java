package com.foobnix.hypen;

import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import com.foobnix.pdf.info.model.BookCSS;

public class HypenUtils {

    private static final String SHY = "&shy;";
    private static DefaultHyphenator instance = new DefaultHyphenator(HyphenPattern.ru);

    public static void applyLanguage(String lang) {
        if (lang == null) {
            return;
        }
        try {
            if (lang.length() > 2) {
                lang = lang.substring(0, 2).toLowerCase(Locale.US);
            }

            HyphenPattern pattern = HyphenPattern.valueOf(lang);
            if (instance.pattern != pattern) {
                instance = new DefaultHyphenator(pattern);
            }
        } catch (Exception e) {
            BookCSS.get().isAutoHypens = false;
        }

    }

    public static String applyHypnes(String htmlEncode) {
        String res = applyHypnesNewMy(htmlEncode);
        return res;
    }

    private static String applyHypnesNewMy(final String input) {
        if (input == null || input.length() == 0) {
            return "";
        }


        final StringBuilder res = new StringBuilder();

        HtmlTokenizer tokenizer = new HtmlTokenizer(input, new TokensListener() {


            @Override
            public void findOther(char ch) {
                res.append(ch);
            }

            @Override
            public void findText(String w) {
                if (w.length() <= 3) {
                    res.append(w);
                } else {
                    String join = join(instance.hyphenate(w), SHY);
                    // LOG.d("Hypn2-", join);
                    res.append(join);
                }

            }

        });
        tokenizer.run();

        String out = res.toString();

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

    public static class HtmlTokenizer {

        private String in;
        private TokensListener listener;

        public HtmlTokenizer(String in, TokensListener listener) {
            this.in = in;
            this.listener = listener;
        }

        public void run() {

            StringBuilder res = new StringBuilder();
            boolean ignore = false;
            for (int i = 0; i < in.length(); i++) {
                char ch = in.charAt(i);


                if (ch == '<') {
                    ignore = true;
                }
                if (ch == '>') {
                    ignore = false;
                }

                if (ignore) {
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
}
