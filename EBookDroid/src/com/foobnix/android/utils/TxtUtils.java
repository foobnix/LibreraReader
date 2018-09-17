package com.foobnix.android.utils;

import java.io.UnsupportedEncodingException;
import java.lang.Character.UnicodeBlock;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.sys.TempHolder;

import android.annotation.TargetApi;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.support.v4.util.Pair;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class TxtUtils {

    public static final String TTS_PAUSE = "<pause>";
    public static String LONG_DASH1 = "\u2013";
    public static String LONG_DASH2 = "\u2014";
    public static String SMALL_DASH = "-";

    public static final String NON_BREAKE_SPACE = "\u00A0";
    public static final char NON_BREAKE_SPACE_CHAR = NON_BREAKE_SPACE.charAt(0);

    public static String trim(String string) {
        if (string == null) {
            return null;
        }
        return string.replaceAll("\\s", " ").trim();
    }

    public static boolean contains(String line, List<String> items) {
        for (String s : items) {
            if (line.contains(s)) {
                return true;
            }
        }
        return false;
    }

    public static String lastTwoPath(String txt) {
        if (TxtUtils.isEmpty(txt)) {
            return "[]";
        }
        txt = Uri.decode(txt);

        int fist = txt.lastIndexOf("/");
        if (fist >= 0) {
            int second = txt.lastIndexOf("/", fist - 1);
            if (second >= 0) {
                return "[..." + txt.substring(second) + "]";
            } else {
                return "[..." + txt.substring(fist) + "]";
            }
        }
        return "[" + txt + "]";
    }

    public static String encode1251(String string) {
        if (Charset.forName("8859_1").newEncoder().canEncode(string)) {
            return encode(string, "8859_1", "Windows-1251");
        } else {
            return string;
        }
    }

    public static String toLowerCase(String str) {
        if (str == null) {
            return str;
        }
        return str.toLowerCase(Locale.US);

    }

    public static String encode(String string, String from, String to) {
        try {
            return new String(string.getBytes(from), to);
        } catch (UnsupportedEncodingException e) {
            return string;
        }
    }

    public static String deltaPage(int current) {
        return deltaPage(current, 0);
    }

    public static String deltaPage(int current, int max) {
        if (max != 0) {
            if (AppState.get().readingProgress == AppState.READING_PROGRESS_PERCENT_NUMBERS) {
                float f = (float) current * 100 / max;
                return String.format("%.1f%%", f);
            }
            if (AppState.get().readingProgress == AppState.READING_PROGRESS_PERCENT) {
                float f = (float) current * 100 / max;
                return String.format("%.1f%%", f);
            }
        }

        if (TempHolder.get().pageDelta == 0) {
            return "" + current;
        }
        return "[" + (current + TempHolder.get().pageDelta) + "]";
    }

    public static String getProgressPercent(int current, int max) {
        float f = (float) current * 100 / max;
        return String.format("%.1f", f) + "%";
    }

    public static String deltaPageMax(int current) {
        if (AppState.get().readingProgress == AppState.READING_PROGRESS_PERCENT) {
            return "100%";
        }
        if (TempHolder.get().pageDelta == 0) {
            return "" + current;
        }
        return "[" + (current + TempHolder.get().pageDelta) + "]";
    }

    public static void addFilteredGenreSeries(String item, List<String> result, boolean simpleAdd) {
        if (TxtUtils.isEmpty(item)) {
            return;
        }
        if (simpleAdd) {
            item = TxtUtils.firstUppercase(item.trim());
            if (!result.contains(item)) {
                result.add(item);
            }
            return;
        }

        if (item.contains(",") || item.contains(";")) {

            String[] split = item.split("[,;]");

            for (String txt : split) {
                if (TxtUtils.isNotEmpty(txt)) {
                    txt = txt.trim();
                    txt = TxtUtils.firstUppercase(txt);
                    if (!result.contains(txt)) {
                        result.add(txt);
                    }
                }
            }
        } else {
            String trim = item.trim();
            if (TxtUtils.isNotEmpty(trim)) {
                trim = TxtUtils.firstUppercase(trim);
                if (!result.contains(trim)) {
                    result.add(trim);
                }
            }
        }

    }

    int a = 1;
    static List<String> partsDivs = Arrays.asList(".", "!", ";", "?", ":", "...", LONG_DASH1, LONG_DASH2);

    public static String[] getParts(String text) {
        int max = -1;
        for (String ch : partsDivs) {
            int last = text.lastIndexOf(ch);
            if (last > max) {
                max = last;
            }
        }
        if (max == -1) {
            max = text.lastIndexOf(",");
        }

        String firstPart = max > 0 ? text.substring(0, max + 1) : text;
        String secondPart = max > 0 ? text.substring(max + 1) : "";

        return new String[] { firstPart, secondPart };

    }

    public static String replaceEndLine(String pageHTML) {
        pageHTML = pageHTML.replace("-<end-line>", "");
        pageHTML = pageHTML.replace("- <end-line>", "");
        pageHTML = pageHTML.replace("<end-line>", " ");
        return pageHTML;
    }

    public static String replaceHTMLforTTS(String pageHTML) {
        if (pageHTML == null) {
            return "";
        }
        LOG.d("pageHTML [before]", pageHTML);

        pageHTML = pageHTML.replace("b><end-line><i>", TTS_PAUSE).replace("i><end-line><b>", TTS_PAUSE);
        pageHTML = pageHTML.replace("b><p><i>", TTS_PAUSE).replace("i><p><b>", TTS_PAUSE);
        pageHTML = pageHTML.replace("<b>", "").replace("</b>", "").replace("<i>", "").replace("</i>", "").replace("<tt>", "").replace("</tt>", "");

        pageHTML = pageHTML.replace("...", TTS_PAUSE);
        pageHTML = pageHTML.replace("…", TTS_PAUSE);
        pageHTML = pageHTML.replace("!", "!" + TTS_PAUSE);
        pageHTML = pageHTML.replace("?", "?" + TTS_PAUSE);
        pageHTML = pageHTML.replace(">" + TxtUtils.LONG_DASH1, ">" + TTS_PAUSE);
        pageHTML = pageHTML.replace(">" + TxtUtils.LONG_DASH2, ">" + TTS_PAUSE);
        pageHTML = pageHTML.replace("   ", TTS_PAUSE);

        pageHTML = pageHTML.replace("<p>", "").replace("</p>", " ");
        pageHTML = pageHTML.replace("&nbsp;", " ").replace("&lt;", " ").replace("&gt;", "").replace("&amp;", " ").replace("&quot;", " ");
        pageHTML = pageHTML.replace("'", "");
        pageHTML = pageHTML.replace("*", "");
        pageHTML = pageHTML.replace("[image]", "");

        pageHTML = replaceEndLine(pageHTML);
        pageHTML = pageHTML.replaceAll("(?u)(\\w+)(-\\s)", "$1");
        LOG.d("pageHTML [after] ", pageHTML);
        return pageHTML;
    }

    public static String sanitizeFilename(String name) {
        return name.replaceAll("[:\\\\/*?|<>]", "_");
    }

    public static String fixAppState(TextView text) {
        return text.getText().toString().trim().replace(",", "");
    }

    public static String fixAppState(String text) {
        if (text == null) {
            return "";
        }
        return text.trim().replace(",", "").replace(";", "");
    }

    public static String getHostUrl(String url) {
        int indexOf = url.indexOf("/", 10);
        if (indexOf > 0) {
            return url.substring(0, indexOf);
        }
        return url;

    }

    public static String getHostLongUrl(String url) {
        int indexOf = url.lastIndexOf("/");
        if (indexOf > 10) {
            return url.substring(0, indexOf);
        }
        return url;

    }

    public static String replaceLast(String input, String from, String to) {
        return input.replaceAll(from + "$", to);
    }

    public static String replaceFirst(String input, String from, String to) {
        return input.replaceAll("^" + from, to);
    }

    public static String getFileMetaBookName(FileMeta fileMeta) {
        if (TxtUtils.isNotEmpty(fileMeta.getAuthor())) {
            return fileMeta.getAuthor() + " " + LONG_DASH1 + " " + fileMeta.getTitle();
        } else {
            return fileMeta.getTitle();
        }

    }

    public static String replaceLastFirstNameSplit(String name) {
        if (TxtUtils.isEmpty(name)) {
            return "";
        }
        name = name.trim();
        if (name.contains(";") || name.contains(",")) {
            StringBuilder res = new StringBuilder();
            String[] split = name.split("[;,]");
            for (String sub : split) {
                if (TxtUtils.isNotEmpty(sub)) {
                    res.append(replaceLastFirstName(sub));
                    res.append(TTS_PAUSE);
                }
            }
            return TxtUtils.replaceLast(res.toString(), ",", "").trim();
        }

        return replaceLastFirstName(name);

    }

    private static String replaceLastFirstName(String name) {
        if (TxtUtils.isEmpty(name)) {
            return "";
        }
        name = name.trim();

        if (!name.contains(" ") || name.endsWith(".")) {
            return name;
        }
        String[] split = name.split(" ");
        StringBuilder res = new StringBuilder();
        res.append(split[split.length - 1]);
        for (int i = 0; i <= split.length - 2; i++) {
            res.append(" ");
            res.append(split[i]);
        }
        return res.toString().trim();

    }

    public static String space() {
        return AppState.get().selectingByLetters ? "" : " ";
    }

    @TargetApi(24)
    public static Spanned underline(final String text) {
        try {
            return Html.fromHtml("<u>" + text + "</u>");
        } catch (Exception e) {
            try {
                return Html.fromHtml("<u>" + text + "</u>", Html.FROM_HTML_MODE_LEGACY);
            } catch (Exception e1) {
                return new SpannedString(text);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isCJK2(int ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        if (Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS.equals(block) || //
                Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS.equals(block) || //
                Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A.equals(block) //
        ) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= 19) {
            return Character.isIdeographic(ch);
        }

        return false;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isCJK(int ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        List<Character.UnicodeBlock> blocks = Arrays.asList(//
                //
                UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS, //
                UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS, //
                UnicodeBlock.CJK_COMPATIBILITY_FORMS, //
                UnicodeBlock.CJK_RADICALS_SUPPLEMENT, //
                UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION, //
                UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A, //
                UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B, //
                UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS, //
                UnicodeBlock.HIRAGANA, //
                UnicodeBlock.KATAKANA//
        );
        if (blocks.contains(block)) {
            return true;
        }

        if (Build.VERSION.SDK_INT >= 19) {
            return Character.isIdeographic(ch);
        }

        return false;
    }

    public static String firstUppercase(String str) {
        if (isEmpty(str) || str.length() <= 1) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static String firstLowerCase(String str) {
        if (isEmpty(str) || str.length() <= 1) {
            return str;
        }
        return str.substring(0, 1).toLowerCase(Locale.US) + str.substring(1);
    }

    public static String filterDoubleSpaces(String str) {
        if (str == null) {
            return str;
        }
        return str.replace("   ", " ").replace("  ", " ");
    }

    public static String substring(String str, int len) {
        if (str.length() > len) {
            return str.substring(0, len);
        }
        return str;

    }

    static List<String> dividers = Arrays.asList(" - ", " _ ", "_-_", "+-+");

    public static Pair<String, String> getTitleAuthorByPath(String name) {
        LOG.d("getTitleAuthorByPath", name);
        String author = "";
        String title = "";
        try {

            if (name.contains(".")) {// remove ext
                name = name.substring(0, name.lastIndexOf("."));
            }

            int indexOf = -1;

            for (String it : dividers) {
                if (name.contains(it)) {
                    indexOf = name.indexOf(it);
                    break;
                }
            }
            if (indexOf > 0) {
                author = name.substring(0, indexOf);
                title = name.substring(indexOf + 3);
            }

            if (TxtUtils.isEmpty(author)) {
                int i = name.lastIndexOf("(");
                int j = name.lastIndexOf(")");
                if (j > 0 && i > 0 && j - i > 10) {
                    author = name.substring(i + 1, j);

                    if (TxtUtils.isEmpty(title)) {
                        title = name.substring(0, i);
                    }
                }
            }

            author = firstUppercase(author.trim());

            if (TxtUtils.isEmpty(title)) {
                title = filterTitle(firstUppercase(name.trim()));
            } else {
                title = filterTitle(firstUppercase(title.trim()));
            }

        } catch (Exception e) {
            LOG.e(e);
        }
        return new Pair<String, String>(title, author);
    }

    static List<String> trash = Arrays.asList("-", "—", "_", "  ");

    public static String filterTitle(String title) {
        if (title == null) {
            return "";
        }

        for (String it : trash) {
            title = title.replace(it, " ");
        }
        return title.trim();

    }

    public static TextView underlineTextView(TextView textView) {
        String text = textView.getText().toString();
        textView.setText(underline(text));
        return textView;
    }

    public static TextView underline(TextView textView, String text) {
        textView.setText(underline(text));
        return textView;
    }

    public static Spanned bold(String text) {
        return Html.fromHtml("<u>" + text + "</u>");
    }

    public static char getLastChar(String line) {
        if (line == null || line.isEmpty()) {
            return 0;
        }
        return line.charAt(line.length() - 1);
    }

    public static boolean isLastCharEq(String line, char[] chars) {
        if (line == null || line.isEmpty()) {
            return false;
        }
        char last = line.charAt(line.length() - 1);
        for (char ch : chars) {
            if (ch == last) {
                return true;
            }
        }
        return false;
    }

    public static String getFirstLetter(String name) {
        if (TxtUtils.isNotEmpty(name)) {
            return String.valueOf(name.charAt(0));
        } else {
            return "";
        }
    }

    public static boolean isFooterNote(String text) {
        if (text == null) {
            return false;
        }
        Pattern p = Pattern.compile("[\\[{][0-9]+[}\\]]");
        text = text.trim();
        return text.length() < 30 && (p.matcher(text).find());
    }

    public static String escapeHtml(CharSequence text) {
        StringBuilder out = new StringBuilder();
        withinStyle(out, text, 0, text.length());
        return out.toString();
    }

    private static void withinStyle(StringBuilder out, CharSequence text, int start, int end) {
        for (int i = start; i < end; i++) {
            char c = text.charAt(i);

            if (c == '<') {
                out.append("&lt;");
            } else if (c == '>') {
                out.append("&gt;");
            } else if (c == '&') {
                out.append("&amp;");
            } else if (c >= 0xD800 && c <= 0xDFFF) {
                if (c < 0xDC00 && i + 1 < end) {
                    char d = text.charAt(i + 1);
                    if (d >= 0xDC00 && d <= 0xDFFF) {
                        i++;
                        int codepoint = 0x010000 | c - 0xD800 << 10 | d - 0xDC00;
                        out.append("&#").append(codepoint).append(";");
                    }
                }
            } else if (c > 0x7E || c < ' ') {
                out.append("&#").append((int) c).append(";");
            } else if (c == ' ') {
                while (i + 1 < end && text.charAt(i + 1) == ' ') {
                    out.append("&nbsp;");
                    i++;
                }

                out.append(' ');
            } else {
                out.append(c);
            }
        }
    }

    public static boolean isNumber(String text) {
        return text != null && text.matches("\\d+");
    }

    public static boolean isLineStartEndUpperCase(String line) {
        try {
            if (line == null || line.length() <= 4) {
                return false;
            }
            line = line.trim();
            if (line.length() <= 4) {
                return false;
            }
            boolean a1 = Character.isUpperCase(line.charAt(0));
            boolean a2 = Character.isUpperCase(line.charAt(1));
            boolean n1 = Character.isUpperCase(line.charAt(line.length() - 2));
            boolean n2 = Character.isUpperCase(line.charAt(line.length() - 3));
            return a1 && a2 && n1 && n2;
        } catch (Exception e) {
            LOG.e(e);
        }
        return false;
    }

    public static String getFooterNote(String input, Map<String, String> footNotes) {
        if (input == null) {
            return "";
        }
        if (footNotes == null) {
            return "";
        }

        try {
            String id = getFooterNoteNumber(input);

            if (TxtUtils.isNotEmpty(id)) {
                String string = footNotes.get(id);
                LOG.d("Find note for id", string);
                string = string.trim().replaceAll("^[0-9]+ ", "");
                return string;
            }

        } catch (Exception e) {
            LOG.e(e);
            return "";
        }

        return "";

    }

    public static String getFooterNoteNumber(String input) {
        if (input == null) {
            return "";
        }
        String patternString = "[\\[{]([0-9]+)[\\]}]";

        Matcher m = Pattern.compile(patternString).matcher(input);

        if (m.find()) {
            return m.group(0);
        }
        return "";

    }

    public static String filterString(String txt) {
        if (TxtUtils.isEmpty(txt)) {
            return txt;
        }

        String replaceAll = txt.trim().replace("   ", " ").replace("  ", " ").replaceAll("\\s", " ").trim();
        replaceAll = replaceAll.replaceAll("(?u)(\\w+)(-\\s)", "$1").trim();

        if (!replaceAll.contains(" ")) {
            String regexp = "[^\\w\\[\\]\\{\\}’']+";
            replaceAll = replaceAll.replaceAll(regexp + "$", "").replaceAll("^" + regexp, "");
        }
        return replaceAll.trim();
    }

    public static String nullToEmpty(Object txt) {
        if (txt == null) {
            return "";
        }
        return txt instanceof String ? ((String) txt).trim() : txt.toString();
    }

    public static String nullNullToEmpty(String txt) {
        if (txt == null || txt.trim().equals("null")) {
            return "";
        }
        return txt.trim();
    }

    public static boolean isEmpty(String txt) {
        return txt == null || txt.trim().length() == 0;
    }

    public static boolean isNotEmpty(String txt) {
        return !isEmpty(txt);
    }

    public static String joinList(String delim, List<?> items) {
        if (items == null || items.size() == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object it : items) {
            sb.append(it);
            sb.append(delim);
        }
        String string = sb.toString();
        if (string.length() > 1) {
            return string.substring(0, string.length() - delim.length());
        } else {
            return string;
        }
    }

    public static String join(String delim, Object... items) {
        if (items == null || items.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object it : items) {
            if (it == null) {
                sb.append("@null");
            } else {
                sb.append(it);
            }
            sb.append(delim);
        }
        String string = sb.toString();
        if (string.length() > 1) {
            return string.substring(0, string.length() - delim.length());
        } else {
            return string;
        }
    }

    /**
     * Replace string "My name is @firstName @lastName"
     * 
     * @param str
     * @param keys
     * @return
     */
    public static String format$(String str, Object... keys) {
        if (str == null) {
            return null;
        }
        for (Object key : keys) {
            int start = str.indexOf("$");
            int end = str.indexOf(" ", start);

            if (end == -1) {
                end = str.length();
            }

            String tag = str.substring(start, end);
            str = str.replace(tag, key.toString());
        }
        return str;
    }

    public static final Pattern EMAIL_PATTERN = Pattern.compile(
            "(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*:(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)(?:,\\s*(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*))*)?;\\s*)");

    public static final Pattern SIMPLE_EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9_\\+-]+(\\.[A-Za-z0-9_\\+-]+)*@[a-z0-9]+(\\.[a-z0-9]+)*\\.([a-z]{2,4})$");

    public static boolean isEmailValidRFC(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isEmailValid(String email) {
        return SIMPLE_EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isListNotEmpty(List<?> objects) {
        return objects != null && objects.size() >= 1;
    }

    public static boolean isListEmpty(List<?> objects) {
        return objects == null || objects.size() <= 0;
    }

    public static void bold(TextView text) {
        text.setTypeface(Typeface.DEFAULT_BOLD, Typeface.BOLD);

    }

    public static String ellipsize(String title, int size) {
        if (TxtUtils.isEmpty(title)) {
            return "";
        }
        if (title.length() <= size) {
            return title;
        }

        String substring = title.substring(0, size);
        if (substring.endsWith(" ")) {
            return substring;
        }
        return substring + " ...";

    }

    public static void updateAllLinks(View parent) {
        try {
            if (parent instanceof ViewGroup) {
                if (AppState.get().isUiTextColor) {
                    TxtUtils.updateAllLinks((ViewGroup) parent, AppState.get().uiTextColor);
                } else {
                    TypedArray out = parent.getContext().getTheme().obtainStyledAttributes(new int[] { android.R.attr.textColorLink });
                    int systemLinkColor = out.getColor(0, 0);

                    TxtUtils.updateAllLinks((ViewGroup) parent, systemLinkColor);
                }
            } else {
                LOG.d("updateAllLinks parent is not ViewGroup");
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static void setLinkTextColor(TextView txt) {
        if (AppState.get().isUiTextColor) {
            txt.setTextColor(AppState.get().uiTextColor);
        }

    }

    public static void updateAllLinks(ViewGroup parent, int color) {
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ViewGroup) {
                updateAllLinks((ViewGroup) child, color);
            }
            if (child instanceof TextView) {
                if ("textLink".equals(child.getTag())) {
                    ((TextView) child).setTextColor(color);
                }
            }
        }

    }

    public static int visibleIf(boolean isVisible) {
        return isVisible ? View.VISIBLE : View.GONE;
    }

}
