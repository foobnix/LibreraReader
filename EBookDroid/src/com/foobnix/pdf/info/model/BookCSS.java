package com.foobnix.pdf.info.model;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.android.utils.Strings;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.ExportSettingsManager;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.FontExtractor;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.ui2.AppDB;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Environment;

public class BookCSS {

    public static final String LINK_COLOR_NIGHT = "#876f52";
    public static final String LINK_COLOR_DAY = "#8c1908";
    public static final String LINK_COLOR_UNIVERSAL = "#0066cc";

    public static final String FONTS_DIR = "Fonts";

    public static final int TEXT_ALIGN_JUSTIFY = 0;
    public static final int TEXT_ALIGN_LEFT = 1;
    public static final int TEXT_ALIGN_RIGHT = 2;
    public static final int TEXT_ALIGN_CENTER = 3;

    public static int STYLES_DOC_AND_USER = 0;
    public static int STYLES_ONLY_DOC = 1;
    public static int STYLES_ONLY_USER = 2;

    public static final String ARIAL = "Arial";
    public static final String COURIER = "Courier";
    public static final String DEFAULT_FONT = "Times New Roman";

    private static final Object TAG = "BookCSS";

    public static List<String> fontExts = Arrays.asList(".ttf", ".otf");

    public static String DEFAULT_FOLDER = "";

    public int documentStyle = STYLES_DOC_AND_USER;
    public int marginTop;
    public int marginRight;
    public int marginBottom;
    public int marginLeft;

    public int emptyLine;

    public int lineHeight;
    public int paragraphHeight;
    public int textIndent;
    public int fontWeight;
    public String customCSS2;

    public int textAlign;

    public String fontFolder;

    public String displayFontName;
    public String normalFont;
    public String boldFont;
    public String boldItalicFont;
    public String italicFont;
    public String headersFont;
    public String capitalFont;

    public boolean isAutoHypens;
    public String hypenLang;
    public String linkColorDay;
    public String linkColorNight;

    public boolean isCapitalLetter = false;
    public int capitalLetterSize = 20;
    public String capitalLetterColor = "#ff0000";

    public void resetToDefault(Context c) {
        textAlign = TEXT_ALIGN_JUSTIFY;

        marginTop = 9;
        marginBottom = 6;

        marginRight = 10;
        marginLeft = 10;

        emptyLine = 1;

        lineHeight = 13;
        paragraphHeight = 0;
        textIndent = 10;
        fontWeight = 400;

        fontFolder = DEFAULT_FOLDER;
        displayFontName = DEFAULT_FONT;
        normalFont = DEFAULT_FONT;
        boldFont = DEFAULT_FONT;
        italicFont = DEFAULT_FONT;
        boldItalicFont = DEFAULT_FONT;
        headersFont = DEFAULT_FONT;
        capitalFont = DEFAULT_FONT;

        documentStyle = STYLES_DOC_AND_USER;
        isAutoHypens = true;
        hypenLang = "en";

        linkColorDay = LINK_COLOR_UNIVERSAL;
        linkColorNight = LINK_COLOR_UNIVERSAL;
        customCSS2 = //
                "code, pre,pre>* {white-space: pre; font-size: 0.6em !important;} /* pre, normal*/ \n" + //
                        "svg {display:block} \n" + //
                        "figure>* {font-size: 0.7em} \n" + //
                        "tr {display:block}\n" + //
                        "td>* {display:inline}\n" + //
                        "blockquote {margin-left:0.5em !important}\n" + //
                        "sup>* {font-size:0.83em;vertical-align:super;font-weight:bold}\n" + //
                        // "letter {font-size: 2em; color:red; font-weight: bold;}\n" + //
                        // "title>p{font-size:1.2em; color:red !important}\n" + //
                        ""//
        ;

        LOG.d("BookCSS", "resetToDefault");

    }

    public void load(Context c) {
        if (c == null) {
            return;
        }
        DEFAULT_FOLDER = FontExtractor.getFontsDir(c, FONTS_DIR).getPath();
        resetToDefault(c);

        SharedPreferences sp = c.getSharedPreferences(ExportSettingsManager.PREFIX_BOOK_CSS, Context.MODE_PRIVATE);
        Objects.loadFromSp(this, sp);
    }

    public void save(Context c) {
        if (c == null) {
            return;
        }
        SharedPreferences sp = c.getSharedPreferences(ExportSettingsManager.PREFIX_BOOK_CSS, Context.MODE_PRIVATE);
        Objects.saveToSP(BookCSS.get(), sp);
    }

    public int position(String fontName) {
        try {
            List<String> allFonts = getAllFonts();
            return allFonts.indexOf(fontName);
        } catch (Exception e) {
            return 0;
        }

    }

    public void checkBeforeExport(Context c) {
        if (fontFolder != null && fontFolder.equals(DEFAULT_FOLDER)) {
            fontFolder = null;
            save(c);
            fontFolder = DEFAULT_FOLDER;
        }

    }

    public void allFonts(String fontName) {
        normalFont = fontName;
    }

    public static class FontPack {
        public String dispalyName = "";
        public String fontFolder;

        public String normalFont;
        public String boldFont;
        public String italicFont;
        public String boldItalicFont;
        public String headersFont;
        public String capitalFont;

        public FontPack(String name, String path) {
            fontFolder = path;
            dispalyName = name;
            normalFont = path + "/" + name;
            boldFont = path + "/" + name;
            italicFont = path + "/" + name;
            boldItalicFont = path + "/" + name;
            headersFont = path + "/" + name;
            capitalFont = path + "/" + name;
        }

        public FontPack(String name) {
            dispalyName = name;
            normalFont = name;
            boldFont = name;
            italicFont = name;
            boldItalicFont = name;
            headersFont = name;
            capitalFont = name;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof FontPack && dispalyName.equals(((FontPack) obj).dispalyName);
        }

    }

    public void resetAll(FontPack pack) {
        LOG.d("resetAll", pack.dispalyName, pack.fontFolder);

        displayFontName = pack.dispalyName;

        normalFont = DEFAULT_FONT;
        boldFont = DEFAULT_FONT;
        italicFont = DEFAULT_FONT;
        boldItalicFont = DEFAULT_FONT;
        headersFont = DEFAULT_FONT;
        capitalFont = DEFAULT_FONT;

        if (displayFontName != null && !displayFontName.contains(".")) {
            normalFont = displayFontName;
            boldFont = displayFontName;
            italicFont = displayFontName;
            boldItalicFont = displayFontName;
            headersFont = displayFontName;
            capitalFont = displayFontName;
            return;
        }

        List<String> all = new ArrayList<String>();

        all.add(ARIAL);
        all.add(COURIER);
        all.add(DEFAULT_FONT);

        all.addAll(getAllFontsFromFolder(pack.fontFolder));

        String dispalyName = pack.dispalyName.replace(".ttf", "").replace(".otf", "").trim().toLowerCase(Locale.US);

        for (String fullName : all) {
            String fontName = ExtUtils.getFileName(fullName).replace(".ttf", "").replace(".otf", "").trim().toLowerCase(Locale.US);

            if (fontName.startsWith(dispalyName) || fontName.equals(dispalyName)) {

                if (fontName.equals(dispalyName) || fontName.contains("regular") || fontName.contains("normal") || fontName.contains("light") || fontName.contains("medium") || fontName.endsWith("me")) {
                    if (!fontName.contains("regularitalic")) {
                        normalFont = capitalFont = fullName;
                    }
                } else if (fontName.contains("bolditalic") || fontName.contains("boldoblique") || fontName.contains("boldit") || fontName.contains("boit") || fontName.contains("bold it") || fontName.contains("bold italic")) {
                    boldItalicFont = fullName;

                } else if (fontName.contains("bold") || fontName.endsWith("bo") || fontName.endsWith("bd") || fontName.contains("bolt")) {
                    headersFont = boldFont = fullName;

                } else if (fontName.contains("italic") || fontName.endsWith("it") || fontName.endsWith("oblique")) {
                    italicFont = fullName;
                } else {
                    normalFont = fullName;
                }
            }
        }
        LOG.d("resetAll 2", normalFont);

    }

    public List<String> getAllFonts() {
        List<String> all = new ArrayList<String>();
        if (AppState.get().lastBookPath != null) {
            all.addAll(getAllFontsFromFolder(new File(AppState.get().lastBookPath).getParent()));
        }
        all.add(ARIAL);
        all.add(COURIER);
        all.add(DEFAULT_FONT);

        all.addAll(getAllFontsFromFolder(fontFolder));

        all.addAll(getAllFontsFromFolder(new File(Environment.getExternalStorageDirectory(), "fonts").getPath()));
        all.addAll(getAllFontsFromFolder(new File(Environment.getExternalStorageDirectory(), "Fonts").getPath()));
        if (Strings.equals(fontFolder, DEFAULT_FOLDER)) {
            all.addAll(getAllFontsFromFolder(new File("/system/fonts").getPath()));
        }

        return all;
    }

    public List<FontPack> getAllFontsPacks() {
        List<FontPack> all = new ArrayList<FontPack>();
        if (AppState.get().lastBookPath != null) {
            all.addAll(getAllFontsFiltered(new File(AppState.get().lastBookPath).getParent()));
        }
        all.add(new FontPack(ARIAL));
        all.add(new FontPack(COURIER));
        all.add(new FontPack(DEFAULT_FONT));

        all.addAll(getAllFontsFiltered(fontFolder));

        all.addAll(getAllFontsFiltered(new File(Environment.getExternalStorageDirectory(), "fonts").getPath()));
        all.addAll(getAllFontsFiltered(new File(Environment.getExternalStorageDirectory(), "Fonts").getPath()));
        if (Strings.equals(fontFolder, DEFAULT_FOLDER)) {
            all.addAll(getAllFontsFiltered(new File("/system/fonts").getPath(), true));
        }

        return all;
    }

    private Collection<FontPack> getAllFontsFiltered(String path) {
        return getAllFontsFiltered(path, false);
    }

    private Collection<FontPack> getAllFontsFiltered(String path, final boolean excludeNoto) {
        if (TxtUtils.isNotEmpty(path) && new File(path).isDirectory()) {
            File file = new File(path);
            String[] list = file.list(new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    name = name.toLowerCase(Locale.US);

                    if (excludeNoto) {
                        if (name.startsWith("noto")) {
                            return false;
                        }
                    }

                    for (String ext : fontExts) {
                        if (name.endsWith(ext)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
            if (list != null && list.length >= 1) {
                List<FontPack> filtered = new ArrayList<FontPack>();

                for (String fontName : list) {

                    String fontNameDisplay = filterFontName(fontName);

                    FontPack e = new FontPack(fontNameDisplay, path);

                    if (!filtered.contains(e)) {
                        e.normalFont = path + "/" + fontName;
                        for (String font : list) {
                            String fontInit = font;

                            font = font.replace(".ttf", "").replace(".otf", "").trim().toLowerCase(Locale.US);
                            fontNameDisplay = fontNameDisplay.replace(".ttf", "").replace(".otf", "").trim().toLowerCase(Locale.US);

                            if (font.startsWith(fontNameDisplay)) {
                                if (font.equals(fontNameDisplay) || font.contains("regular") || font.contains("normal") || font.contains("light") || font.contains("medium") || font.endsWith("me")) {
                                    e.normalFont = path + "/" + fontInit;
                                    break;
                                }
                            }

                        }

                        filtered.add(e);
                    }
                }

                Collections.sort(filtered, new Comparator<FontPack>() {
                    @Override
                    public int compare(FontPack o1, FontPack o2) {
                        return o1.dispalyName.toLowerCase(Locale.US).compareTo(o2.dispalyName.toLowerCase(Locale.US));
                    }

                });
                return filtered;
            }
        }
        return Collections.EMPTY_LIST;
    }

    public static String filterFontName(String fontName) {
        if (!fontName.contains(".")) {
            return fontName;
        }
        String ext = ExtUtils.getFileExtension(fontName);
        if (fontName.contains("-")) {
            fontName = fontName.substring(0, fontName.indexOf("-")) + "." + ext;
        } else if (fontName.contains("_")) {
            fontName = fontName.substring(0, fontName.indexOf("_")) + "." + ext;
        } else if (fontName.contains(" ")) {
            fontName = fontName.substring(0, fontName.indexOf(" ")) + "." + ext;
        }
        return fontName;
    }

    private Collection<String> getAllFontsFromFolder(String path) {
        try {
            if (TxtUtils.isNotEmpty(path) && new File(path).isDirectory()) {
                File file = new File(path);
                String[] list = file.list(new FilenameFilter() {

                    @Override
                    public boolean accept(File dir, String name) {
                        name = name.toLowerCase(Locale.US);
                        for (String ext : fontExts) {
                            if (name.endsWith(ext)) {
                                return true;
                            }
                        }
                        return false;
                    }
                });
                if (list != null && list.length >= 1) {
                    List<String> filtered = new ArrayList<String>();

                    for (String fontName : list) {
                        filtered.add(path + "/" + fontName);
                    }

                    Collections.sort(filtered, new Comparator<String>() {
                        @Override
                        public int compare(String o1, String o2) {
                            return o1.toLowerCase(Locale.US).compareTo(o2.toLowerCase(Locale.US));
                        }

                    });
                    return filtered;
                }
            }
        } catch (Exception e) {
            LOG.e(e);
        }
        return Collections.EMPTY_LIST;
    }

    public boolean isFontFileName(String name) {
        if (TxtUtils.isEmpty(name)) {
            return false;
        }
        name = name.toLowerCase(Locale.US);
        for (String ext : fontExts) {
            if (name.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public String em(int value) {
        if (value == 0) {
            return "0px";
        }
        float em = (float) value / 10;
        return "" + em + "em";
    }

    private static BookCSS inst = new BookCSS();

    public static BookCSS get() {

        return inst;
    }

    public String getTextAlignConst(int id) {
        if (id == TEXT_ALIGN_JUSTIFY) {
            return "justify";
        }
        if (id == TEXT_ALIGN_LEFT) {
            return "left";
        }
        if (id == TEXT_ALIGN_RIGHT) {
            return "right";
        }
        if (id == TEXT_ALIGN_CENTER) {
            return "center";
        }
        return "initial";
    }

    public String toCssString() {
        StringBuilder builder = new StringBuilder();

        String backgroundColor = MagicHelper.colorToString(MagicHelper.getBgColor());
        String textColor = MagicHelper.colorToString(MagicHelper.getTextColor());

        builder.append("documentStyle" + documentStyle + "{}");
        builder.append("isAutoHypens1" + isAutoHypens + hypenLang + "{}");

        builder.append("b>span,strong>span{font-weight:normal}");// fix chess

        // PAGE BEGIN
        builder.append("@page{");
        builder.append(String.format("margin-top:%s !important;", em(marginTop)));
        builder.append(String.format("margin-right:%s !important;", em(marginRight)));
        builder.append(String.format("margin-bottom:%s !important;", em(marginBottom - 1)));
        builder.append(String.format("margin-left:%s !important;", em(marginLeft)));
        builder.append("}");
        // PAGE END

        // FB2
        builder.append("section>title{page-break-before:avoide;}");
        builder.append("section>title>p{text-align:center !important; text-indent:0px !important;}");
        builder.append("title>p{text-align:center !important; text-indent:0px !important;}");
        builder.append("subtitle{text-align:center !important; text-indent:0px !important;}");
        builder.append("image{text-align:center; text-indent:0px;}");
        builder.append("section+section>title{page-break-before:always;}");
        builder.append(String.format("empty-line{padding-top:%s;}", em(emptyLine)));
        builder.append("epigraph{text-align:right; margin-left:2em;font-style: italic;}");

        builder.append("text-author{font-style: italic;font-weight: bold;}");
        builder.append("p>image{display:block;}");
        if (paragraphHeight > 0) {// bug is here
            builder.append(String.format("p{margin:%s 0}", em(paragraphHeight)));
        }
        // not supported text-decoration
        builder.append("del,ins,u,strikethrough{font-family:monospace;}");

        // FB2 END

        builder.append("p,div,span, body{");
        builder.append(String.format("background-color:%s !important;", backgroundColor));
        builder.append(String.format("color:%s !important;", textColor));
        builder.append(String.format("line-height:%s !important;", em(lineHeight)));
        builder.append("}");

        builder.append("body{");
        builder.append("padding:0 !important; margin:0 !important;");
        builder.append("}");

        if (documentStyle == STYLES_DOC_AND_USER || documentStyle == STYLES_ONLY_USER) {

            if (AppState.get().isDayNotInvert) {
                builder.append("a{color:" + linkColorDay + " !important;}");
            } else {
                builder.append("a{color:" + linkColorNight + " !important;}");
            }

            // FONTS BEGIN
            if (isFontFileName(normalFont)) {
                builder.append("@font-face {font-family: my; src: url('" + normalFont + "') format('truetype'); font-weight: normal; font-style: normal;}");
            }

            if (isFontFileName(capitalFont)) {
                builder.append("@font-face {font-family: myCapital; src: url('" + capitalFont + "') format('truetype'); font-weight: normal; font-style: normal;}");
            }

            if (isFontFileName(boldFont)) {
                builder.append("@font-face {font-family: my; src: url('" + boldFont + "') format('truetype'); font-weight: bold; font-style: normal;}");
            }

            if (isFontFileName(italicFont)) {
                builder.append("@font-face {font-family: my; src: url('" + italicFont + "') format('truetype'); font-weight: normal; font-style: italic;}");
            }

            if (isFontFileName(boldItalicFont)) {
                builder.append("@font-face {font-family: my; src: url('" + boldItalicFont + "') format('truetype'); font-weight: bold; font-style: italic;}");
            }

            if (isFontFileName(headersFont)) {
                builder.append("@font-face {font-family: myHeader; src: url('" + headersFont + "') format('truetype'); }");
                builder.append("h1{font-size:1.50em; text-align: center; font-weight: normal; font-family: myHeader;}");
                builder.append("h2{font-size:1.30em; text-align: center; font-weight: normal; font-family: myHeader;}");
                builder.append("h3{font-size:1.15em; text-align: center; font-weight: normal; font-family: myHeader;}");
                builder.append("h4{font-size:1.00em; text-align: center; font-weight: normal; font-family: myHeader;}");
                builder.append("h5{font-size:0.80em; text-align: center; font-weight: normal; font-family: myHeader;}");
                builder.append("h6{font-size:0.60em; text-align: center; font-weight: normal; font-family: myHeader;}");

                builder.append("title,title>p,title>p>strong  {font-size:1.2em; font-weight: normal; font-family: myHeader !important;}");
                builder.append(/*                 */ "subtitle{font-size:1.0em; font-weight: normal; font-family: myHeader !important;}");

            } else {
                builder.append("h1{font-size:1.50em; text-align: center; font-weight: bold; font-family: " + headersFont + ";}");
                builder.append("h2{font-size:1.30em; text-align: center; font-weight: bold; font-family: " + headersFont + ";}");
                builder.append("h3{font-size:1.15em; text-align: center; font-weight: bold; font-family: " + headersFont + ";}");
                builder.append("h4{font-size:1.00em; text-align: center; font-weight: bold; font-family: " + headersFont + ";}");
                builder.append("h5{font-size:0.80em; text-align: center; font-weight: bold; font-family: " + headersFont + ";}");
                builder.append("h6{font-size:0.60em; text-align: center; font-weight: bold; font-family: " + headersFont + ";}");

                builder.append("title, title>p{font-size:1.2em; font-weight: bold; font-family: " + headersFont + ";}");
                builder.append("subtitle, subtitle>p{font-size:1.0em; font-weight: bold; font-family: " + headersFont + ";}");
            }

            builder.append("h1,h2,h3,h4,h5,h6,img {text-indent:0px !important; text-align: center;}");

            // FONTS END

            // BODY BEGIN

            // BODY END

            if (isCapitalLetter) {
                builder.append("letter{");
                if (isFontFileName(capitalFont)) {
                    builder.append("font-family: myCapital !important;");
                } else {
                    builder.append("font-family:" + capitalFont + " !important;");
                }

                builder.append(String.format("font-size:%s;", em(capitalLetterSize)));
                builder.append(String.format("color:%s;", capitalLetterColor));

                builder.append("}");
            }

            builder.append("body,p{");

            if (isFontFileName(normalFont)) {
                builder.append("font-family: my !important;");
            } else {
                builder.append("font-family:" + normalFont + " !important; font-weight:normal;");
            }

            if (AppState.get().isAccurateFontSize) {
                builder.append(String.format("text-align:%s !important;", getTextAlignConst(textAlign)));
            } else {
                builder.append(String.format("text-align:%s;", getTextAlignConst(textAlign)));
            }
            builder.append("}");

            builder.append(String.format("p{text-indent:%s;}", em(textIndent)));

            if (!isFontFileName(boldFont)) {
                builder.append("b{font-family:" + boldFont + ";font-weight: bold;}");
            }

            if (!isFontFileName(italicFont)) {
                builder.append("i{font-family:" + italicFont + "; font-style: italic, oblique;}");
            }
            if (AppState.get().isAccurateFontSize) {
                builder.append("body,p,b,i,em,span{font-size:medium !important;}");
            }

            builder.append(customCSS2.replace("\n", ""));

        }

        String result = builder.toString();
        return result;

    }

    public String getFontWeight(String fontName) {
        return isFontFileName(fontName) ? "bold" : "bold";
    }

    public String getHeaderFontFamily(String fontName) {
        return isFontFileName(fontName) ? "myHeader" : fontName;
    }

    public static Typeface getTypeFaceForFont(String fontName) {
        if (TxtUtils.isEmpty(fontName)) {
            return Typeface.DEFAULT;
        }
        try {

            if (fontName.equals(BookCSS.ARIAL)) {
                return Typeface.SANS_SERIF;
            } else if (fontName.equals(BookCSS.COURIER)) {
                return Typeface.MONOSPACE;
            } else if (fontName.equals(BookCSS.DEFAULT_FONT)) {
                return Typeface.SERIF;
            } else {
                return Typeface.createFromFile(fontName);
            }

        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }

    public void detectLang(String bookPath) {
        FileMeta meta = AppDB.get().load(bookPath);
        if (meta != null) {
            BookCSS.get().hypenLang = meta.getLang();
        }
        if (TxtUtils.isEmpty(BookCSS.get().hypenLang)) {
            BookCSS.get().hypenLang = Urls.getLangCode();
        }
    }

}
