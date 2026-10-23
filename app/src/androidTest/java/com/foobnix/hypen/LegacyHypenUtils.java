package com.foobnix.hypen;

import com.foobnix.android.utils.TxtUtils;
import com.foobnix.model.AppState;
import com.foobnix.model.SimpleMeta;
import com.foobnix.pdf.info.model.BookCSS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HypenUtils.applyHypnes and tokenize exactly as they were before reading the settings once per
 * call (git 509ea0b13), copied verbatim so HypenUtilsCompareTest can run the old and the new on
 * the same text in one process. Its own word cache and tokenizer state, so neither run warms the
 * other. Test code only.
 */
class LegacyHypenUtils {

    static final String SHY = HypenUtils.SHY;
    static Map<String, String> cache = new HashMap<>();
    static boolean ignore = false;
    static boolean ignore1 = false;
    static DefaultHyphenator instance = new DefaultHyphenator(HyphenPattern.error);

    static void resetTokenizer() {
        ignore = false;
        ignore1 = false;
    }

    static String applyHypnes(final String input2, List<SimpleMeta> replacements) {
        if (input2 == null || input2.length() == 0) {
            return "";
        }
        //LOG.d("applyHypnesNewMy-input",input);

        String input = input2;
        final StringBuilder res = new StringBuilder();

        if (AppState.get().isEnableTextReplacement && replacements != null) {
            for (SimpleMeta it : replacements) {
                if (TxtUtils.isNotEmpty(it.name)) {
                    if (it.name.startsWith("*")) {
                        String regexp = it.name.substring(1);
                        input = TxtUtils.replaceAll(input, regexp, it.path);
                    } else {
                        input = input.replace(it.name, it.path);
                    }
                }
            }
        }

        tokenize(input, new HypenUtils.TokensListener() {

            @Override
            public void findOther(char ch) {
                res.append(ch);
            }

            @Override
            public void findText(String w) {


                if (AppState.get().isBionicMode) {
                    res.append(TxtUtils.toBionicWord(w));
                    return;
                }
                if (!BookCSS.get().isAutoHypens) {
                    res.append(w);
                    return;
                }

                if (w.length() <= 3) {
                    res.append(w);
                } else {
                    String obj = cache.get(w);
                    if (obj != null) {
                        res.append(obj);
                    } else {
                        try {
                            StringBuilder result = new StringBuilder();
                            instance.hyphenate(w, result, SHY);
                            String join = result.toString();
                            res.append(join);
                            cache.put(w, join);
                        } catch (Exception e) {
                            res.append(w);
                            cache.put(w, w);
                        }
                    }
                }
            }

        });

        String out = res.toString();
        //LOG.d("applyHypnesNewMy-out",out);

        return out;
    }

    public static void tokenize(String in, HypenUtils.TokensListener listener) {

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
                if (AppState.get().isExperimental && (res.length() >= 30 && res.length() % 30 == 0)) {
                    res.append(" ");
                }
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
