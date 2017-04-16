package com.foobnix.hypen;

import java.util.List;

public class HypenUtils {

    private static final String SHY = "&shy;";
    private static DefaultHyphenator instance = new DefaultHyphenator(HyphenPattern.en_us);

    public static void applyLanguage(String lang) {
        HyphenPattern pattern = HyphenPattern.valueOf(lang);

        if (instance.pattern != pattern) {
            instance = new DefaultHyphenator(pattern);
        }
    }

    public static String applyHypnes(String input) {
        if (input == null || input.length() == 0) {
            return "";
        }
        input = input.replace("<", " <").replace(">", "> ").replace("—", "-");

        String split[] = input.split(" ");
        StringBuilder res = new StringBuilder();
        for (String w : split) {
            if (w.length() <= 3) {
                res.append(w + " ");
                continue;
            }

            if (w.contains("-") || w.contains("<") || w.contains(">")) {
                res.append(w + " ");
                continue;
            }

            char last = w.charAt(w.length() - 1);
            char first = w.charAt(0);

            boolean startWithOther = false;
            if (!Character.isLetter(first)) {
                startWithOther = true;
                w = w.substring(1, w.length());
            }

            boolean endWithOther = false;
            if (w.length() != 0 && !Character.isLetter(last)) {
                endWithOther = true;
                w = w.substring(0, w.length() - 1);
            }

            List<String> hyphenate = instance.hyphenate(w);
            String result = join(hyphenate, SHY);

            if (startWithOther) {
                result = String.valueOf(first) + result;
            }

            if (endWithOther) {
                result = result + String.valueOf(last);
            }

            res.append(result + " ");
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
        return string.replace(SHY + SHY, SHY);
    }
}
