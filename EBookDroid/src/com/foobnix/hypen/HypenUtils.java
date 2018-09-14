package com.foobnix.hypen;

import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import com.foobnix.android.utils.TxtUtils;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.AppState;

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
        return applyHypnesOld(htmlEncode);
    }

    private static String applyHypnesOld(String input) {
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

    private static String applyHypnesNewNotUse(String input) {
        if (input == null || input.length() == 0) {
            return "";
        }

        // input = input.replace("\u00A0", " ");

        StringBuilder res = new StringBuilder();
        String tokens[] = input.split("(?=<)|(?<=>)");

        for (int j = 0; j < tokens.length; j++) {
            String token = tokens[j];

            if (token.length() <= 3) {
                res.append(token);
                continue;
            }

            if (token.contains("<") || token.contains(">") || token.contains("=")) {
                res.append(token);
                continue;
            }

            String[] words = token.split(" ");
            for (int n = 0; n < words.length; n++) {
                String w = words[n];
                if (w.length() <= 3) {
                    res.append(w + " ");
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
                res.append(result + " ");
            }

        }
        return res.toString().replace(" <", "<").replace("> ", ">");
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

}
