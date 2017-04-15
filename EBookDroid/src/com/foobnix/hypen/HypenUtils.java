package com.foobnix.hypen;

import java.util.List;

import com.foobnix.android.utils.LOG;

public class HypenUtils {

    private static DefaultHyphenator instance = new DefaultHyphenator(HyphenPattern.en_us);

    public static void applyLanguage(String lang) {
        HyphenPattern pattern = HyphenPattern.valueOf(lang);
        LOG.d("applyLanguage", pattern.lang);

        if (instance.pattern != pattern) {
            instance = new DefaultHyphenator(pattern);
        }
    }

    public static String applyHypnes(String input) {
        if (input == null || input.length() == 0) {
            return "";
        }
        // if (input.startsWith("<") && input.endsWith(">")) {
        // return input;
        // }
        String split[] = input.split(" ");
        StringBuilder res = new StringBuilder();
        for (String w : split) {
            if (w.length() == 0) {
                res.append(" ");
                continue;
            }

            // if (w.contains("<") || w.contains("/>")) {
            // res.append(w); // skip html tags
            // continue;
            // }

            char last = w.charAt(w.length() - 1);
            boolean needRemove = false;
            if (!Character.isLetter(last)) {
                needRemove = true;
                w = w.substring(0, w.length() - 1);
            }

            List<String> hyphenate = instance.hyphenate(w);
            String result = join(hyphenate, "&shy;");
            if (needRemove) {
                result = result + String.valueOf(last);
            }
            res.append(result + " ");
        }

        return res.toString();
    }

    public static String join(List<String> list, String delimiter) {
        if (list == null || list.size() == 0) {
            return "";
        }
        StringBuilder result = new StringBuilder(list.get(0));
        for (int i = 1; i < list.size(); i++) {
            result.append(delimiter).append(list.get(i));
        }
        return result.toString().replace("|­|", "|");
    }
}
