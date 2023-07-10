package com.foobnix.pdf.info.model;

import static com.foobnix.pdf.info.AppsConfig.MUPDF_1_11;
import static com.foobnix.pdf.info.AppsConfig.MUPDF_FZ_VERSION;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Environment;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.JsonDB;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.Objects;
import com.foobnix.android.utils.Objects.IgnoreHashCode;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppBook;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppSP;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.ui2.AppDB;
import com.foobnix.ui2.FileMetaCore;

import org.ebookdroid.common.settings.books.SharedBooks;
import org.librera.LinkedJSONObject;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class BookCSS {
    /// PATHS

    public static final String LIBRERA_CLOUD_DROPBOX = "Librera.Cloud-Dropbox";
    public static final String LIBRERA_CLOUD_GOOGLEDRIVE = "Librera.Cloud-GoogleDrive";
    public static final String LIBRERA_CLOUD_ONEDRIVE = "Librera.Cloud-OneDrive";
    public static final String LINK_COLOR_UNIVERSAL = "#0066cc";
    public static final int TEXT_ALIGN_JUSTIFY = 0;
    public static final int TEXT_ALIGN_LEFT = 1;
    public static final int TEXT_ALIGN_RIGHT = 2;
    public static final int TEXT_ALIGN_CENTER = 3;

    public static final String TIMES_NEW_ROMAN = "Times New Roman";
    public static final String ARIAL = "Arial";
    public static final String COURIER = "Courier";
    public static final String CHARIS_SIL = "Charis SIL";

    public static final String DEFAULT_FONT = CHARIS_SIL;

    public static final String LINKCOLOR_DAYS = "#001BA5, #9F0600" + "," + LINK_COLOR_UNIVERSAL;
    public static final String LINKCOLOR_NIGHTS = "#7494B2, #B99D83" + "," + LINK_COLOR_UNIVERSAL;
    private static final Object TAG = "BookCSS";
    public static int STYLES_DOC_AND_USER = 0;
    public static int STYLES_ONLY_DOC = 1;
    public static int STYLES_ONLY_USER = 2;
    public static List<String> fontExts = Arrays.asList(".ttf", ".otf");
    private static BookCSS instance = new BookCSS();
    public String searchPathsJson;

    public String cachePath = new File(AppProfile.DOWNLOADS_DIR, "Librera/Cache").getPath();
    public String downlodsPath;

    ///
    public String ttsSpeakPath = new File(AppProfile.DOWNLOADS_DIR, "Librera/TTS").getPath();
    public String backupPath = new File(AppProfile.DOWNLOADS_DIR, "Librera/Backup").getPath();

    public String syncDropboxPath = new File(AppProfile.DOWNLOADS_DIR, "Librera/" + LIBRERA_CLOUD_DROPBOX).getPath();
    public String syncGdrivePath = new File(AppProfile.DOWNLOADS_DIR, "Librera/" + LIBRERA_CLOUD_GOOGLEDRIVE).getPath();
    public String syncOneDrivePath = new File(AppProfile.DOWNLOADS_DIR, "Librera/" + LIBRERA_CLOUD_ONEDRIVE).getPath();
    public String dictPath;
    public String fontFolder;
    public volatile int fontSizeSp = Dips.isXLargeScreen() ? 26 : 21;
    public float appFontScale = 1.0f;
    public String mp3BookPathJson;
    public String dirLastPath;
    public String pathSAF = "";
    public boolean isSyncWifiOnly;
    public boolean isSyncPullToRefresh = true;
    public boolean isSyncAnimation = true;
    public int documentStyle = STYLES_DOC_AND_USER;
    public int marginTop;
    public int marginRight;
    public int marginBottom;
    public int marginLeft;
    public int emptyLine;
    public int lineHeight12;
    public int paragraphHeight;
    public int textIndent;
    public int fontWeight;
    public String customCSS2;
    public int textAlign;
    public String displayFontName;
    public String normalFont;
    public String boldFont;
    public String boldItalicFont;
    public String italicFont;
    public String headersFont;
    public String capitalFont;
    public boolean isAutoHypens;
    public String linkColorDay;
    public String linkColorNight;
    public boolean isCapitalLetter = false;
    public int capitalLetterSize = 20;
    public String capitalLetterColor = "#ff0000";
    public float imageScale = 2.0f;

    @IgnoreHashCode
    public int hashCode = 0;

    @IgnoreHashCode
    public String linkColorDays = LINKCOLOR_DAYS;
    @IgnoreHashCode
    public String linkColorNigths = LINKCOLOR_NIGHTS;
    public String userStyleCss = MUPDF_FZ_VERSION.equals(MUPDF_1_11) ? "app-Librera.css" : "app-Librera-Tables.css";
    private String lastBookPathCache = "";
    private String trackPathCache;

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

    public static BookCSS get() {

        return instance;
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
            } else if (fontName.equals(BookCSS.TIMES_NEW_ROMAN)) {
                return Typeface.SERIF;
            } else {
                return Typeface.createFromFile(fontName);
            }

        } catch (Exception e) {
            return Typeface.DEFAULT;
        }
    }

    public void mp3BookPath(String track) {
        final LinkedJSONObject obj = (mp3BookPathJson == null) ? new LinkedJSONObject() : new LinkedJSONObject(mp3BookPathJson);
        obj.put(AppSP.get().lastBookPath, track);

        LOG.d("mp3BookPath-set", AppSP.get().lastBookPath, track);
        mp3BookPathJson = obj.toString();

        trackPathCache = track;
        lastBookPathCache = AppSP.get().lastBookPath;
    }

    public String mp3BookPathGet() {
        if (lastBookPathCache != null && lastBookPathCache.equals(AppSP.get().lastBookPath)) {
            return trackPathCache;
        }
        final LinkedJSONObject obj = (mp3BookPathJson == null) ? new LinkedJSONObject() : new LinkedJSONObject(mp3BookPathJson);
        final String track = obj.optString(AppSP.get().lastBookPath);
        LOG.d("mp3BookPath-get", AppSP.get().lastBookPath, track);
        trackPathCache = track;
        lastBookPathCache = AppSP.get().lastBookPath;

        return track;
    }

    public boolean isTextFormat() {
        try {
            return ExtUtils.isTextFomat(AppSP.get().lastBookPath);
        } catch (Exception e) {
            return false;
        }
    }

    public void resetToDefault(Context c) {
        textAlign = TEXT_ALIGN_JUSTIFY;

        marginTop = 9;
        marginBottom = 6;

        marginRight = 10;
        marginLeft = 10;

        emptyLine = 1;

        lineHeight12 = 14;
        paragraphHeight = 1;
        textIndent = 10;
        fontWeight = 400;

        fontFolder = AppProfile.syncFontFolder.getPath();
        downlodsPath = AppProfile.syncDownloadFolder.getPath();
        displayFontName = DEFAULT_FONT;
        normalFont = DEFAULT_FONT;
        boldFont = DEFAULT_FONT;
        italicFont = DEFAULT_FONT;
        boldItalicFont = DEFAULT_FONT;
        headersFont = DEFAULT_FONT;
        capitalFont = DEFAULT_FONT;

        documentStyle = STYLES_DOC_AND_USER;
        isAutoHypens = true;
        AppSP.get().hypenLang = null;

        linkColorDay = LINK_COLOR_UNIVERSAL;
        linkColorNight = LINK_COLOR_UNIVERSAL;

        linkColorDays = LINKCOLOR_DAYS;
        linkColorNigths = LINKCOLOR_NIGHTS;

        customCSS2 = //
                "code,pre,pre>* {white-space:pre-wrap; font-size:0.8em;}\n" + //
                        ""//
        ;

        LOG.d("BookCSS", "resetToDefault");

    }

    public void load1(Context c) {
        if (c == null) {
            return;
        }
        resetToDefault(c);

        IO.readObj(AppProfile.syncCSS, instance);

        try {
            if (TxtUtils.isEmpty(instance.searchPathsJson)) {
                List<String> extFolders = ExtUtils.getAllExternalStorages(c);

                if (!extFolders.contains(Environment.getExternalStorageDirectory().getPath())) {
                    extFolders.add(Environment.getExternalStorageDirectory().getPath());
                }

                instance.searchPathsJson = JsonDB.set(extFolders);
                LOG.d("searchPaths-all", instance.searchPathsJson);
            }
        } catch (Exception e) {
            LOG.e(e);
        }

    }

    public void save(Context c) {
        if (c == null) {
            return;
        }

        int currentHash = Objects.hashCode(instance, false);
        if (currentHash != instance.hashCode) {
            LOG.d("Objects-save", "SAVE BookCSS");
            hashCode = currentHash;
            IO.writeObj(AppProfile.syncCSS, instance);
        }
    }

    public int position(String fontName) {
        try {
            List<String> allFonts = getAllFonts();
            return allFonts.indexOf(fontName);
        } catch (Exception e) {
            return 0;
        }

    }

    public void allFonts(String fontName) {
        normalFont = fontName;
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

        all.add(TIMES_NEW_ROMAN);
        all.add(ARIAL);
        all.add(COURIER);
        all.add(CHARIS_SIL);

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
        if (AppSP.get().lastBookPath != null) {
            all.addAll(getAllFontsFromFolder(new File(AppSP.get().lastBookPath).getParent()));
        }
        all.add(TIMES_NEW_ROMAN);
        all.add(ARIAL);
        all.add(COURIER);
        all.add(CHARIS_SIL);

        all.addAll(getAllFontsFromFolder(fontFolder));
        all.addAll(getAllFontsFromFolder(new File(Environment.getExternalStorageDirectory(), "fonts").getPath()));
        all.addAll(getAllFontsFromFolder(new File(Environment.getExternalStorageDirectory(), "Fonts").getPath()));
        all.addAll(getAllFontsFromFolder(new File("/system/fonts").getPath()));

        return all;
    }

    public List<FontPack> getAllFontsPacks() {
        List<FontPack> all = new ArrayList<FontPack>();

        all.addAll(getAllFontsFiltered(fontFolder));
        all.addAll(getAllFontsFiltered(new File(Environment.getExternalStorageDirectory(), "fonts").getPath()));
        all.addAll(getAllFontsFiltered(new File(Environment.getExternalStorageDirectory(), "Fonts").getPath()));
        all.addAll(getAllFontsFiltered(new File("/system/fonts").getPath(), true));

        Collections.sort(all, new Comparator<FontPack>() {
            @Override
            public int compare(FontPack o1, FontPack o2) {
                return o1.dispalyName.compareTo(o2.dispalyName);
            }
        });

        all.add(0, new FontPack(COURIER));
        all.add(0, new FontPack(ARIAL));
        all.add(0, new FontPack(TIMES_NEW_ROMAN));
        all.add(0, new FontPack(CHARIS_SIL));
        if (AppSP.get().lastBookPath != null) {
            all.addAll(0, getAllFontsFiltered(new File(AppSP.get().lastBookPath).getParent()));
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
                    LOG.d("name-accept", name);
                    if (excludeNoto) {
                        if (name.startsWith("noto")) {
                            return false;
                        } else if (name.startsWith("sec")) {
                            return false;
                        } else if (name.startsWith("samsung")) {
                            return false;
                        } else if (name.startsWith("clock")) {
                            return false;
                        }
                    }

                    for (
                            String ext : fontExts) {
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

    public boolean isUrlFont(String name) {
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
        return toCssString("");
    }

    public String important(String input) {
        if (documentStyle == STYLES_ONLY_USER) {
            return input.replace(";", " !important;");
        }
        return input;

    }

    public String toCssString(String path) {

        lineHeight12 = Math.max(10, lineHeight12);

        StringBuilder builder = new StringBuilder();

        File cssFile = new File(AppProfile.SYNC_FOLDER_DEVICE_PROFILE, userStyleCss);
        if (!cssFile.exists()) {
            try {
                String css = IO.readStringFromAsset("app-Librera.css");
                LOG.d("BookCSS-from asset");
                builder.append(css);
            } catch (Exception e) {
                LOG.e(e);
            }
        } else {
            String css = IO.readString(cssFile);
            LOG.d("BookCSS-from file", cssFile);
            builder.append(css);
        }

        String backgroundColor = MagicHelper.colorToString(MagicHelper.getBgColor());
        String textColor = MagicHelper.colorToString(MagicHelper.getTextColor());

        builder.append("documentStyle" + documentStyle + "{}");
        builder.append("isAutoHypens1" + isAutoHypens + AppSP.get().hypenLang + "{}");

        // PAGE BEGIN
        builder.append("@page {");
        builder.append(String.format("margin-top:%s !important;", em(marginTop * 2)));
        builder.append(String.format("margin-right:%s !important;", em(marginRight * 2)));
        builder.append(String.format("margin-bottom:%s !important;", em((marginBottom - 1) * 2)));
        builder.append(String.format("margin-left:%s !important;", em(marginLeft * 2)));
        builder.append("}");
        // PAGE END

        builder.append(String.format("empty-line {padding:%s;}", em(emptyLine)));

        builder.append("t {color:" + (AppState.get().isDayNotInvert ? linkColorDay : linkColorNight) + " !important;}");

        builder.append("svg {");
        builder.append("margin:0 !important;");
        builder.append("padding:0 !important;");
        builder.append("}");

        if (documentStyle == STYLES_DOC_AND_USER || documentStyle == STYLES_ONLY_USER) {

            builder.append("a {color:" + (AppState.get().isDayNotInvert ? linkColorDay : linkColorNight) + " !important;}");
            //apply settings

            if (paragraphHeight > 0) {// bug is here
                builder.append("div, p {");
                builder.append(important(String.format("margin:%s 0;", em(paragraphHeight * 2))));
                builder.append("}");
            }

            // <P> begin
            builder.append("body, div, p, span {");
            builder.append(String.format("font-size:medium !important;"));

            if (AppState.get().isDayNotInvert) {
                if (!"#FFFFFF".equals(backgroundColor)) {
                    builder.append(important(String.format("background-color:%s;", backgroundColor)));
                }
                if (!"#000000".equals(textColor)) {
                    builder.append(important(String.format("color:%s;", textColor)));
                }

            } else {
                //Important in the night mode
                builder.append(String.format("background-color:%s !important;", backgroundColor));
                builder.append(String.format("color:%s !important;", textColor));
            }
            //always important
            builder.append(String.format("line-height:%s !important;", em(lineHeight12)));
            builder.append(String.format("text-indent:%s !important;", em(textIndent)));
            builder.append(String.format("text-align:%s;", getTextAlignConst(textAlign)));

            if (isUrlFont(normalFont)) {
                builder.append(important("font-family:'my';"));
            } else {
                builder.append(important("font-family:'" + normalFont + "';"));
            }

            builder.append("}");
            // </P> end

            builder.append("div {");
            builder.append("margin-right:0 !important;");
            builder.append("margin-left:0 !important;");
            builder.append("padding-left:0 !important;");
            builder.append("padding-right:0 !important;");
            builder.append("}");

            // FONTS BEGIN
            if (isUrlFont(normalFont)) {
                builder.append("@font-face {font-family:'my'; src:url('" + normalFont + "'); font-weight:normal; font-style:normal;}");
            }
            if (isUrlFont(boldFont)) {
                builder.append("@font-face {font-family:'my'; src:url('" + boldFont + "'); font-weight:bold; font-style:normal;}");
            } else {
                builder.append("b {font-family:'" + boldFont + "';font-weight:bold;}");
            }

            if (isUrlFont(italicFont)) {
                builder.append("@font-face {font-family:'my'; src:url('" + italicFont + "'); font-weight:normal; font-style:italic;}");
            } else {
                builder.append("i {font-family:'" + italicFont + "'; font-style:italic;}");
            }

            if (isUrlFont(boldItalicFont)) {
                builder.append("@font-face {font-family:'my'; src:url('" + boldItalicFont + "'); font-weight:bold; font-style:italic;}");
            }

            if (isUrlFont(headersFont)) {
                builder.append("@font-face {font-family:'myHeader'; src:url('" + headersFont + "');}");
                builder.append(important("h1,h2,h3,h4,h5,h6 {font-weight:normal; font-family:'myHeader';}"));
                builder.append(important("title,title>p,title>p>strong {font-weight:normal; font-family:'myHeader';}"));
                builder.append(important("subtitle {font-weight:normal; font-family:'myHeader';}"));

            } else {
                builder.append(important("h1,h2,h3,h4,h5,h6 {font-weight:bold; font-family:'" + headersFont + "';}"));
                builder.append(important("title,title>p,title>p>strong {font-weight:bold; font-family:'" + headersFont + "';}"));
                builder.append(important("subtitle, subtitle>p {font-weight:bold; font-family:'" + headersFont + "';}"));
            }

            builder.append(customCSS2.replace("\n", ""));

        }
        //FB2 Capital letter for all styles
        if (isCapitalLetter) {
            if (isUrlFont(capitalFont)) {
                builder.append("@font-face {font-family:myCapital; src:url('" + capitalFont + "') ; font-weight:normal; font-style:normal;}");
            }
            builder.append("letter{");
            if (isUrlFont(capitalFont)) {
                builder.append("font-family:myCapital !important;");
            } else {
                builder.append("font-family:" + capitalFont + " !important;");
            }

            builder.append(String.format("font-size:%s;", em(capitalLetterSize)));

            if (capitalLetterColor.equals("#000000") && !AppState.get().isDayNotInvert) {
                builder.append(String.format("color:%s;", textColor));
            } else if (capitalLetterColor.equals("#FFFFFF") && AppState.get().isDayNotInvert) {
                builder.append(String.format("color:%s;", textColor));
            } else {
                builder.append(String.format("color:%s;", capitalLetterColor));
            }

            builder.append("}");
        }

        String result = builder.toString();
        LOG.d("BookCSS", result);
        return result;
    }

    public String getHeaderFontFamily(String fontName) {
        return isUrlFont(fontName) ? "myHeader" : fontName;
    }

    public void detectLang(String bookPath) {

        if (AppState.get().isDefaultHyphenLanguage) {
            AppSP.get().hypenLang = AppState.get().defaultHyphenLanguageCode;
            LOG.d("set defaultHyphenLanguageCode", AppSP.get().hypenLang);
            return;
        }

        FileMeta meta = AppDB.get().load(bookPath);
        if (meta == null) {
            meta = FileMetaCore.createMetaIfNeed(bookPath, false);
        }
        AppSP.get().hypenLang = meta.getLang();
        LOG.d("detectLang", bookPath, AppSP.get().hypenLang);

        if (TxtUtils.isEmpty(AppSP.get().hypenLang)) {
            final AppBook load = SharedBooks.load(bookPath);
            if (load != null) {
                AppSP.get().hypenLang = load.ln;
            }
        }
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

}
