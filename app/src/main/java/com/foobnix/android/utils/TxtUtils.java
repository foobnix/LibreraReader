package com.foobnix.android.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.text.Spanned;
import android.text.SpannedString;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.core.util.Pair;

import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.model.MyPath;
import com.foobnix.pdf.info.Android6;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;
import com.foobnix.ui2.AppDB;

import org.ebookdroid.LibreraApp;
import org.librera.LinkedJSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.Character.UnicodeBlock;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TxtUtils {

    public static final String TTS_PAUSE = "ttsPAUSE";
    public static final String TTS_NEXT = "ttsNEXT";
    public static final String TTS_SKIP = "ttsSKIP";
    public static final String TTS_PAUSE_VIEW = "[-]\n";
    public static final String TTS_STOP = "ttsSTOP";

    public static final String NON_BREAKE_SPACE = "\u00A0";
    public static final char NON_BREAKE_SPACE_CHAR = NON_BREAKE_SPACE.charAt(0);
    public static final Pattern EMAIL_PATTERN = Pattern.compile(
            "(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*:(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)(?:,\\s*(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*))*)?;\\s*)");
    public static final Pattern SIMPLE_EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9_\\+-]+(\\.[A-Za-z0-9_\\+-]+)*@[a-z0-9]+(\\.[a-z0-9]+)*\\.([a-z]{2,4})$");
    public static String LONG_DASH1 = "\u2013";
    public static String LONG_DASH2 = "\u2014";
    public static String SMALL_DASH = "-";
    public static LinkedHashMap<String, String> dictRegEx = new LinkedHashMap<>();
    public static String dictHash = "";
    public static boolean hasDB = false;
    static List<String> partsDivs = Arrays.asList(".", "!", ";", "?", ":", "...", LONG_DASH1, LONG_DASH2);
    static List<String> shortList = new ArrayList<>();
    static List<String> dividers = Arrays.asList(" - ", " _ ", "_-_", "+-+");
    static List<String> trash = Arrays.asList("-", "—", "_", "  ");
    int a = 1;

    //<a> asdfsadf </a>
    public static String getStringInTag(String string, String tag) {
        String start = "<" + tag + ">";
        String end = "</" + tag + ">";
        int i1 = string.indexOf(start);
        int i2 = string.indexOf(end);

        if (i1 >= 0 && i2 > i1) {
            return string.substring(i1 + start.length(), i2);
        }

        return "";
    }

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

    public static String lastWord(String line) {
        if (TxtUtils.isEmpty(line)) {
            return "";
        }
        line = line.trim();
        int indexOf = line.lastIndexOf(" ");
        if (indexOf == -1) {
            return line;
        }
        return line.substring(indexOf + 1);
    }

    public static String smallPathFormat(String txt) {
        if (TxtUtils.isEmpty(txt)) {
            return "[]";
        }
        txt = Uri.decode(txt);
        return txt.replace(MyPath.INTERNAL_ROOT, "...");
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
            if (AppState.get().pageNumberFormat == AppState.PAGE_NUMBER_FORMAT_PERCENT) {
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
        try {
            float f = (float) current * 100 / max;
            return String.format("%.1f", f) + "%";
        } catch (Exception e) {
            LOG.e(e);
            return "-1";
        }
    }

    public static String percentFormatInt(float f) {
        return Math.round(f * 100) + "%";
    }

    public static String deltaPageMax(int current) {
        if (AppState.get().pageNumberFormat == AppState.PAGE_NUMBER_FORMAT_PERCENT) {
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

        if (item.contains(",")) {

            String[] split = item.split(",");

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

    public static void addFilteredTags(String item, List<String> result) {
        if (TxtUtils.isEmpty(item)) {
            return;
        }

        if (item.contains(",")) {

            String[] split = item.split(",");

            for (String txt : split) {
                if (TxtUtils.isNotEmpty(txt)) {
                    txt = txt.trim();
                    txt = TxtUtils.firstUppercase(txt);
                    txt = StringDB.filter(txt);
                    if (!result.contains(txt)) {
                        result.add(txt);
                    }
                }
            }
        } else {
            String trim = item.trim();
            if (TxtUtils.isNotEmpty(trim)) {
                trim = TxtUtils.firstUppercase(trim);
                trim = StringDB.filter(trim);
                if (!result.contains(trim)) {
                    result.add(trim);
                }
            }
        }

    }

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

        return new String[]{firstPart, secondPart};

    }

    public static String replacePDFEndLine(String pageHTML) {
        pageHTML = pageHTML.replace("-<end-line>", "");
        pageHTML = pageHTML.replace("- <end-line>", "");
        pageHTML = pageHTML.replace("<end-line>", " ");

        pageHTML = pageHTML.replace("<pause>", "");
        pageHTML = pageHTML.replace("<end-block>", "");
        //pageHTML = replaceAll(pageHTML, "<pause-font-size-[0-9,.]*>", "");
        return pageHTML;
    }

    public static String replaceEndLine(String pageHTML) {
        pageHTML = pageHTML.replace("-<end-line>" + TTS_PAUSE + TTS_PAUSE, "");
        pageHTML = pageHTML.replace("-<end-line>" + TTS_PAUSE, "");
        pageHTML = pageHTML.replace("-<end-line>", "");
        pageHTML = pageHTML.replace("- <end-line>", "");
        pageHTML = pageHTML.replace("<end-line>" + TTS_PAUSE + TTS_PAUSE, " ");
        pageHTML = pageHTML.replace("<end-line>" + TTS_PAUSE, " ");
        pageHTML = pageHTML.replace("<end-line>", " ");
        pageHTML = pageHTML.replace("<end-block>", "");//TTS_PAUSE  end-block works unexpected

        //Dips.spToPx(size)
        //(Dips.dpToPx(BookCSS.get().fontSizeSp)
        //pageHTML = replaceAll(pageHTML, "<pause-font-size-[0-9,.]*>$", "");
        //pageHTML = replaceAll(pageHTML, "<pause-font-size-[0-9,.]*>", TTS_PAUSE);
        return pageHTML;
    }

    public static String replaceHTMLforTTS(String pageHTML) {
        try {
            return replaceHTMLforTTSAll(pageHTML);
        } catch (Throwable e) {
            return pageHTML;
        }
    }

    public static String replaceHTMLforTTSAll(String pageHTML) {
        if (pageHTML == null) {
            return "";
        }
        LOG.d("pageHTML [before]", pageHTML);
        if (AppState.get().isAccurateFontSize) {
            pageHTML = pageHTML.toLowerCase();
            LOG.d("pageHTML [isAccurateFontSize]", pageHTML);

        }

        pageHTML = pageHTML.replace("<pause>", TTS_PAUSE);
        pageHTML = pageHTML.replace("<b><end-line><i>", TTS_PAUSE).replace("<i><end-line><b>", TTS_PAUSE);
        pageHTML = pageHTML.replace("<b><p><i>", TTS_PAUSE).replace("</b></i></p>", TTS_PAUSE).replace("<i><p><b>", TTS_PAUSE).replace("</i></p></b>", TTS_PAUSE);
        pageHTML = pageHTML.replace("<b>", "").replace("</b>", "").replace("<i>", "").replace("</i>", "").replace("<tt>", "").replace("</tt>", "");

        pageHTML = pageHTML.replace("...", " " + TTS_PAUSE);
        pageHTML = pageHTML.replace("…", " " + TTS_PAUSE);
        pageHTML = pageHTML.replace(">" + TxtUtils.LONG_DASH1, ">" + TTS_PAUSE);
        pageHTML = pageHTML.replace(">" + TxtUtils.LONG_DASH2, ">" + TTS_PAUSE);
        pageHTML = pageHTML.replace("   ", " " + TTS_PAUSE + " ");

        LOG.d("pageHTML [1]", pageHTML);


        pageHTML = pageHTML.replace("<p>", " ").replace("</p>", " ");
        pageHTML = pageHTML.replace("&nbsp;", " ").replace("&lt;", " ").replace("&gt;", "").replace("&amp;", " ").replace("&quot;", "\"");
        pageHTML = pageHTML.replace("[image]", "");

        LOG.d("pageHTML [2", pageHTML);

        pageHTML = pageHTML.replace("<end-line>.", ".");

        if (AppState.get().isShowFooterNotesInText && AppSP.get().hypenLang != null) {
            try {
                String string = getLocaleStringResource(new Locale(AppSP.get().hypenLang), R.string.foot_notes, LibreraApp.context);
                pageHTML = replaceAll(pageHTML, "[\\[{][0-9]+[\\]}]", TTS_PAUSE + " " + TTS_PAUSE + string + TTS_PAUSE);
            } catch (Exception e) {
                LOG.e(e);
            }
        } else {
            pageHTML = replaceAll(pageHTML, "[\\[{]\\d+[\\]}]", "");//replace[1] or{22} or [32] or {3}
        }
        pageHTML = pageHTML.replaceAll("(\\p{Alpha}{3,})\\d+", "$1");//replace1


        LOG.d("pageHTML [3]", pageHTML);

        if (AppState.get().isEnalbeTTSReplacements) {
            try {
                LinkedJSONObject obj = new LinkedJSONObject(AppState.get().lineTTSReplacements3);

                final Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = obj.optString(key);
                    if (key.startsWith("<") && key.endsWith(">")) {
                        pageHTML = pageHTML.replace(key, value);
                        LOG.d("System-replace", key, value);
                    }

                }
            } catch (Exception e) {
                LOG.e(e);
            }
        }

        pageHTML = replaceEndLine(pageHTML);


        pageHTML = replaceAll(pageHTML, "(\\w+)-\\s+", "$1");
        LOG.d("pageHTML [after] ", pageHTML);

        LOG.d("pageHTML [4]", pageHTML);

        if (AppState.get().isEnalbeTTSReplacements) {


            if (TxtUtils.isNotEmpty(BookCSS.get().dictPath)) {
                loadReplayceDict();

                for (String key : dictRegEx.keySet()) {
                    try {
                        String value = dictRegEx.get(key);

                        if (key.startsWith("*")) {
                            key = key.substring(1);
                            pageHTML = replaceAll(pageHTML, key, value);
                            LOG.d("pageHTML-dict-replaceAll", key, value, pageHTML);
                        } else {
                            pageHTML = pageHTML.replace(key, value);
                            LOG.d("pageHTML-dict-replace", key, value, pageHTML);
                        }


                    } catch (Exception e) {
                        LOG.e(e);
                    }
                }


            }

            try {
                LinkedJSONObject obj = new LinkedJSONObject(AppState.get().lineTTSReplacements3);

                final Iterator<String> keys = obj.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = obj.getString(key);

                    if (key.endsWith("256")) {
                        continue;
                    }
                    if (key.startsWith("#")) {
                        continue;
                    }

                    if (key.startsWith("*")) {
                        key = key.substring(1);
                        try {
                            pageHTML = replaceAll(pageHTML, key, value);
                        } catch (Exception e) {
                            LOG.e(e);
                        }
                    } else {
                        pageHTML = pageHTML.replace(key, value);
                    }
                    LOG.d("pageHTML [8]", pageHTML);

                }


                LOG.d("pageHTML [8]", pageHTML);


            } catch (Exception e) {
                LOG.e(e);
            }


            LOG.d("pageHTML [after replacments] ", pageHTML);

        }

        pageHTML = pageHTML.replace(" ,", ",");
        pageHTML = pageHTML.replace(",,", ",");

        pageHTML = pageHTML.replace(".,", ".");
        pageHTML = pageHTML.replace(",.", ".");
        pageHTML = pageHTML.replace("..", ".");

        if (AppState.get().ttsReadBySentences) {
            loadShotList();
            LOG.d("pageHTML [8a]", pageHTML);
            for (String r : shortList) {
                if (r.startsWith("!")) {
                    String line = r.replace("!", "");
                    String r1 = line;
                    String r2 = line.replace(".", " ");
                    pageHTML = pageHTML.replace(" " + r1, " " + r2);
                }
            }
            LOG.d("pageHTML [8b]", pageHTML);
            for (String r : shortList) {
                if (!r.startsWith("!")) {
                    String r1 = r;
                    String r2 = r.replace(".", "{dot}");
                    pageHTML = pageHTML.replace(" " + r1, " " + r2);
                }
            }
            LOG.d("pageHTML [8c]", pageHTML);


            pageHTML = replaceAll(pageHTML, " (\\p{Alpha}{1,3})\\.(\\p{Alpha}{1,3})\\.(\\p{Alpha}{1,3})\\.(\\p{Alpha}{1,3})\\.", " $1{dot}$2{dot}$3{dot}$4{dot}");
            pageHTML = replaceAll(pageHTML, " (\\p{Alpha}{1,3})\\.(\\p{Alpha}{1,3})\\.(\\p{Alpha}{1,3})\\.", " $1{dot}$2{dot}$3{dot}");
            pageHTML = replaceAll(pageHTML, " (\\p{Alpha}{1,3})\\.(\\p{Alpha}{1,3})\\.", " $1{dot}$2{dot}");
            pageHTML = replaceAll(pageHTML, " (\\p{Alpha}{1,3})\\. (\\p{Alpha}{1,3})\\.", " $1{dot} $2{dot}");
            pageHTML = replaceAll(pageHTML, " (\\p{Alpha}{1,2})\\.", " $1{dot}");

            pageHTML = replaceAll(pageHTML, "(\\p{Alpha}+)\\.(\\p{Alpha}+)", "$1{dot}$2");
            pageHTML = replaceAll(pageHTML, "(\\p{Alpha}+)\\.(\\p{Alpha}+)", "$1{dot}$2");

            pageHTML = replaceAll(pageHTML, " (\\p{Digit}*)\\.(\\p{Digit}+)", " $1{dot}$2"); //skip numbers 3.3 .343


            LOG.d("pageHTML [8f]", pageHTML);

            for (int i = 0; i < AppState.get().ttsSentecesDivs.length(); i++) {
                String s = String.valueOf(AppState.get().ttsSentecesDivs.charAt(i));
                pageHTML = pageHTML.replace(s, s + TTS_PAUSE + " ");
            }


            LOG.d("pageHTML [9]", pageHTML);

            pageHTML = pageHTML.replace("{dot}", ".");
        }

        pageHTML = pageHTML.replaceAll("[\\s]*(" + TTS_PAUSE + ")*[\\s]*" + TTS_PAUSE + "[\\s]*", TTS_PAUSE).trim();

        return pageHTML;
    }

    public static String replaceAll(String input, String regex, String replacement) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                return Pattern.compile(regex, Pattern.UNICODE_CASE).matcher(input).replaceAll(replacement);
            } else {
                return Pattern.compile(regex).matcher(input).replaceAll(replacement);
            }
        } catch (Exception e) {
            LOG.e(e);
            return Pattern.compile(regex).matcher(input).replaceAll(replacement);
        }
    }

    public static void loadShotList() {
        try {
            if (!shortList.isEmpty()) {
                return;
            }

            final InputStream open = LibreraApp.context.getAssets().open("dict/Librera_Сокращения.txt");
            BufferedReader input = new BufferedReader(new InputStreamReader(open));
            String line;
            while ((line = input.readLine()) != null) {
                if (TxtUtils.isNotEmpty(line)) {
                    line = line.trim();
                    shortList.add(line);
                    if (line.startsWith("!")) {
                        shortList.add(TxtUtils.secondUppercase(line));
                    } else {
                        shortList.add(TxtUtils.firstUppercase(line));
                    }

                    LOG.d("loadShotList-line", line);
                }


            }
            input.close();
        } catch (IOException e) {
            LOG.e(e);
        }
    }


    public static void loadReplayceDict() {
        if (dictHash.equals(BookCSS.get().dictPath)) {
            return;
        }
        dictHash = BookCSS.get().dictPath;
        hasDB = false;
        dictRegEx.clear();

        final List<String> dicts = StringDB.asList(BookCSS.get().dictPath);

        for (String dict : dicts) {

            if (TxtUtils.isEmpty(dict)) {
                continue;
            }

            if (dict.endsWith(".db")) {
                AppDB.get().openDictDB(LibreraApp.context, dict);
                hasDB = true;
                continue;

            }


            LOG.d("pageHTML-dict", dict);
            LOG.d("pageHTML-dict", dict);
            try {
                processDict(new FileInputStream(dict), new ReplaceRule() {
                    @Override
                    public void replace(String from, String to) {

                        dictRegEx.put(from, to);
                    }

                    @Override
                    public void replaceAll(String from, String to) {

                        dictRegEx.put("*" + from, to);
                    }
                });
            } catch (FileNotFoundException e) {
                LOG.e(e);
            }
        }


    }

    public static void processDict(InputStream io, ReplaceRule rule) {
        try {
            LOG.d("processDict");
            BufferedReader input = new BufferedReader(new InputStreamReader(io));
            String line;
            while ((line = input.readLine()) != null) {
                if (TxtUtils.isEmpty(line)) {
                    continue;
                } else if (line.startsWith("#")) {
                    continue;
                } else if (line.endsWith("256")) {
                    continue;
                } else if (line.startsWith("*\"")) {
                    String parts[] = line.split("\" \"");
                    String r1 = parts[0].substring(2);
                    String r2 = parts[1].substring(0, parts[1].lastIndexOf("\""));
                    LOG.d("pageHTML-replaceAll", r1, r2);

                    rule.replaceAll(r1, r2);

                } else if (line.startsWith("\"")) {
                    String parts[] = line.split("\" \"");
                    String r1 = parts[0].substring(1);
                    String r2 = parts[1].substring(0, parts[1].lastIndexOf("\""));
                    LOG.d("pageHTML-replace", r1, r2);
                    rule.replace(r1, r2);
                }
            }
            input.close();
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public static String getLocaleStringResource(Locale requestedLocale, int resourceId, Context context) {
        String result;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) { // use latest api
            Configuration config = new Configuration(context.getResources().getConfiguration());
            config.setLocale(requestedLocale);
            result = context.createConfigurationContext(config).getText(resourceId).toString();
        } else { // support older android versions
            Resources resources = context.getResources();
            Configuration conf = resources.getConfiguration();
            Locale savedLocale = conf.locale;
            conf.locale = requestedLocale;
            resources.updateConfiguration(conf, null);

            // retrieve resources from desired locale
            result = resources.getString(resourceId);

            // restore original locale
            conf.locale = savedLocale;
            resources.updateConfiguration(conf, null);
        }

        return result;
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

    public static String fixFileName(String fileName) {
        fileName = fileName.replaceAll("[\\/:*?\"'<>|]", "_");
        return fileName;

    }

    public static String fixFilePath(String fileName) {
        fileName = fileName.replaceAll("[':*?\"<>|]", "_");
        return fileName;

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
        try {
            return input.replaceAll("^" + from, to);
        } catch (Exception e) {
            return input;
        }
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
                    res.append(",");
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

    @TargetApi(24)
    public static Spanned fromHtml(final String text) {
        try {
            return Html.fromHtml(text);
        } catch (Exception e) {
            try {
                return Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY);
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

    public static String secondUppercase(String str) {
        if (isEmpty(str) || str.length() <= 2) {
            return str;
        }
        return str.substring(0, 1) + str.substring(1, 2).toUpperCase() + str.substring(2);
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

    public static String substringSmart(String str, int len) {
        if (str.length() > len) {
            int index = str.indexOf(" ", len);
            if (index > 0) {
                return str.substring(0, index);
            } else {
                return str.substring(0, len);
            }
        }
        return str;

    }

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

    public static String filterTitle(String title) {
        if (title == null) {
            return "";
        }

        for (String it : trash) {
            title = title.replace(it, " ");
        }
        return title.trim();

    }

    public static TextView underlineTextView(View view) {
        TextView textView = (TextView) view;
        String text = textView.getText().toString();
        textView.setText(underline(text));
        return textView;
    }

    public static TextView underline(TextView textView, String text) {
        textView.setText(underline(text));
        return textView;
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
                if (TxtUtils.isNotEmpty(string)) {
                    LOG.d("Find note for id", string);
                    string = string.trim().replaceAll("^[0-9]+ ", "");
                    return string;
                }
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
            return m.group(0).trim();
        }
        return "";

    }

    public static String filterString(String txt) {
        if (TxtUtils.isEmpty(txt)) {
            return txt;
        }

        String replaceAll = txt.trim().replace("   ", " ").replace("  ", " ").replaceAll("\\s", " ").trim();
        replaceAll = replaceAll(replaceAll, "(\\w+)(-\\s)", "$1").trim();

        if (!replaceAll.contains(" ")) {
            String regexp = "[^\\w\\[\\]\\{\\}’']+";
            replaceAll = replaceAll(replaceAll, regexp + "$", "").replaceAll("^" + regexp, "");
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

    public static String joinList(String delim, Collection<?> items) {
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
        return join(delim, true, items);
    }

    public static String joinTrim(String delim, Object... items) {
        return join(delim, false, items).replace(NON_BREAKE_SPACE, " ").trim();
    }

    public static String join(String delim, boolean withNull, Object... items) {
        if (items == null || items.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object it : items) {
            if (withNull && it == null) {
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

    public static boolean isEmailValidRFC(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isEmailValid(String email) {
        return SIMPLE_EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isListNotEmpty(Collection<?> objects) {
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
                    TypedArray out = parent.getContext().getTheme().obtainStyledAttributes(new int[]{android.R.attr.textColorLink});
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

    public static String getMp3TimeString(long millis) {
        StringBuffer buf = new StringBuffer();

        int hours = (int) (millis / (1000 * 60 * 60));
        int minutes = (int) ((millis % (1000 * 60 * 60)) / (1000 * 60));
        int seconds = (int) (((millis % (1000 * 60 * 60)) % (1000 * 60)) / 1000);

        if (hours > 0) {
            buf.append(String.format("%02d", hours)).append(":");
        }
        ;

        buf.append(String.format("%02d", minutes)).append(":").append(String.format("%02d", seconds));

        return buf.toString();
    }

    public static void updateinks(ViewGroup parent, int color) {
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ViewGroup) {
                updateinks((ViewGroup) child, color);
            } else if (child instanceof TextView) {
                final TextView it = (TextView) child;
                if (it.getTextColors().getDefaultColor() != Color.WHITE) {
                    it.setTextColor(color);
                }
            } else if (child instanceof CheckBox) {
                ((CheckBox) child).setTextColor(color);
            }
        }
    }

    public static void setInkTextView(View... parents) {
        if (AppState.get().appTheme != AppState.THEME_INK) {
            return;
        }
        for (View parent : parents) {
            if (parent instanceof ViewGroup) {
                updateinks((ViewGroup) parent, Color.BLACK);
            } else if (parent instanceof TextView) {
                ((TextView) parent).setTextColor(Color.BLACK);
            }
        }
    }

    public static interface ReplaceRule {
        void replace(String from, String to);

        void replaceAll(String from, String to);


    }


}
