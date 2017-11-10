package com.foobnix.pdf.info.wrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.MemoryUtils;
import com.foobnix.opds.SamlibOPDS;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.ExportSettingsManager;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.view.DragingPopup;
import com.foobnix.ui2.AppDB;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.view.KeyEvent;

public class AppState {

    public static final int TEXT_COLOR_DAY = Color.parseColor("#5b5b5b");
    public static final int TEXT_COLOR_NIGHT = Color.parseColor("#8e8e8e");

    public static final long APP_CLOSE_AUTOMATIC = TimeUnit.MINUTES.toMillis(2);
    // public static final long APP_CLOSE_AUTOMATIC =
    // TimeUnit.SECONDS.toMillis(5);
    public static final int DAY_TRANSPARENCY = 200;
    public static final int NIGHT_TRANSPARENCY = 160;
    public static Map<String, String[]> CONVERTERS = new LinkedHashMap<String, String[]>();
    static {
        CONVERTERS.put("PDF", "https://cloudconvert.com/anything-to-pdf, http://topdf.com".split(", "));
        CONVERTERS.put("PDF Rotate", "https://www.pdfrotate.com, https://smallpdf.com/rotate-pdf, http://www.rotatepdf.net".split(", "));
        CONVERTERS.put("EPUB", "https://cloudconvert.com/anything-to-epub, http://toepub.com".split(", "));
        CONVERTERS.put("MOBI", "https://cloudconvert.com/anything-to-mobi, http://toepub.com".split(", "));
        CONVERTERS.put("AZW3", "https://cloudconvert.com/anything-to-azw3, http://toepub.com".split(", "));
        CONVERTERS.put("DOCX", "https://cloudconvert.com/anything-to-docx, http://document.online-convert.com/convert-to-docx, http://pdf2docx.com/".split(", "));

    }

    public static final String PNG = "PNG";
    public static final String JPG = "JPG";

    public static final String[] LIBRE_EXT = ".odt, .odp, .docx, .doc, .pptx, .ppt".split(", ");
    public static final String[] OTHER_BOOK_EXT = ".wav, .abw, .docm, .lwp, .md, .pages, .rst, .sdw, .tex, .wpd, .wps, .zabw, .cbc,  .chm, .lit, .lrf, .oeb, .pml, .rb, .snb, .tcr, .txtz".split(", ");
    public static final String[] OTHER_ARCH_EXT = ".img, .zip, .rar, .7z, .arj, .bz2, .bzip2, .tbz2, .tbz, .txz, .cab, .gz, .gzip, .tgz, .iso, .lzh, .lha, .lzma, .tar, .xar, .z, .taz, .xz, .dmg".split(", ");

    public static int COLOR_WHITE = Color.WHITE;
    // public static int COLOR_BLACK = Color.parseColor("#030303");
    public static int COLOR_BLACK = Color.BLACK;

    public static int WIDGET_LIST = 1;
    public static int WIDGET_GRID = 2;

    public static int EDIT_NONE = 0;
    public static int EDIT_PEN = 1;
    public static int EDIT_DELETE = 2;

    public static int TAP_NEXT_PAGE = 0;
    public static int TAP_PREV_PAGE = 1;
    public static int TAP_DO_NOTHING = 2;

    public boolean isUseTypeFace = false;

    public static List<Integer> NEXT_KEYS = Arrays.asList(//
            KeyEvent.KEYCODE_VOLUME_UP, //
            KeyEvent.KEYCODE_PAGE_UP, //
            // KeyEvent.KEYCODE_DPAD_UP,//
            KeyEvent.KEYCODE_DPAD_RIGHT, //
            94, //
            105, //
            KeyEvent.KEYCODE_DEL//
    );

    public static List<Integer> PREV_KEYS = Arrays.asList(//
            KeyEvent.KEYCODE_VOLUME_DOWN, //
            KeyEvent.KEYCODE_PAGE_DOWN, //
            // KeyEvent.KEYCODE_DPAD_DOWN, //
            KeyEvent.KEYCODE_DPAD_LEFT, //
            95, //
            106, //
            KeyEvent.KEYCODE_ENTER //

    );

    public List<String> COLORS = Arrays.asList(//
            "#000001", //
            "#000002", //
            "#0000FF", //
            "#00FF00", //
            "#808000", //
            "#FFFF00", //
            "#FF0000", //
            "#00FFFF", //
            "#000000", //
            "#FF00FF", //
            "#808080", //
            "#008000", //
            "#800000", //
            "#000080", //
            "#800080", //
            "#008080", //
            "#C0C0C0", //
            "#FFFFFF", //
            "#CDDC39"//
    );

    public static final List<String> STYLE_COLORS = Arrays.asList(//
            "#3949AB", //
            // "#2a56c6", //
            // "#E6A639", //
            // "#395B9C", //
            "#EA5964", //
            "#00897B", //
            "#000000" //

    );

    public final static String OPDS_DEFAULT = "" + //
    // "http://flibusta.is/opds,Flibusta,Книжное
    // братство,http://flibusta.is/favicon.ico;" + //
            "http://opds.litres.ru,Litres,Библиотека электронных книг,assets://opds/litres.ico;" + //
            "https://books.fbreader.org/opds,FBReader,My personal catalogue,assets://opds/fbreader.png;" + //
            "https://www.gitbook.com/api/opds/catalog.atom,GitBook,Public books are always free.,assets://opds/gitbook.png;" + //
            "http://m.gutenberg.org/ebooks.opds/,Project Gutenberg,Free ebooks since 1971,assets://opds/gutenberg.png;" + //
            "http://manybooks.net/opds/index.php,Manybooks,Online Catalog for Manybooks.net,assets://opds/manybooks.png;" + //
            "https://www.smashwords.com/atom,Smashwords,Online Catalog,assets://opds/smashwords.png;" + //
            "http://www.feedbooks.com/publicdomain/catalog.atom,Feedbooks,Free ebooks,assets://opds/feedbooks.ico;" + //
            "http://samlib.ru,Журнал Самиздат (samlib.ru),Cовременная литература при библиотеке Мошкова,assets://opds/web.png;" + //
            SamlibOPDS.ROOT_AWARDS + ",Usefull links: The Awards and Top Books - Награды и премии, Complete award winners listing,assets://opds/rating.png;" //
    // end
    ;
    // end

    public String myOPDS = OPDS_DEFAULT;

    public final static String READ_COLORS_DEAFAUL =
            // (name),(bg),(text),(0-day 1-nigth)
            "" + //
                    "1,#ffffff,#000000,0;" + //
                    "2,#f2f0e9,#383226,0;" + //
                    "3,#f9f5e8,#333333,0;" + //
                    //
                    "A,#000000,#ffffff,1;" + //
                    "B,#000000,#8cffb5,1;" + //
                    "C,#3a3a3a,#c8c8c8,1;"; //

    public String readColors = READ_COLORS_DEAFAUL;

    public static String DEFAULTS_TABS_ORDER = "0#1,1#1,2#1,3#1,4#1,5#1,6#0";
    public String tabsOrder = DEFAULTS_TABS_ORDER;

    public int tintColor = Color.parseColor(STYLE_COLORS.get(0));

    public int statusBarColorDay = TEXT_COLOR_DAY;
    public int statusBarColorNight = TEXT_COLOR_NIGHT;
    // public int tintColor =
    // Color.parseColor(STYLE_COLORS.get(STYLE_COLORS.size() - 2));
    public int userColor = Color.MAGENTA;

    final public static List<Integer> WIDGET_SIZE = Arrays.asList(0, 70, 100, 150, 200, 250);

    public final static int MAX_SPEED = 149;

    public final static int SORT_BY_PATH = 0;
    public final static int SORT_BY_NAME = 1;
    public final static int SORT_BY_SIZE = 2;
    public final static int SORT_BY_DATE = 3;

    public final static int MODE_GRID = 1;
    public final static int MODE_LIST = 2;
    public final static int MODE_COVERS = 3;
    public final static int MODE_AUTHORS = 4;
    public final static int MODE_GENRE = 5;
    public final static int MODE_SERIES = 6;
    public final static int MODE_LIST_COMPACT = 7;

    public final static int BOOKMARK_MODE_BY_DATE = 1;
    public final static int BOOKMARK_MODE_BY_BOOK = 2;

    public final static int DOUBLE_CLICK_AUTOSCROLL = 0;
    public final static int DOUBLE_CLICK_ADJUST_PAGE = 1;
    public final static int DOUBLE_CLICK_NOTHING = 2;
    public final static int DOUBLE_CLICK_ZOOM_IN_OUT = 3;
    public final static int DOUBLE_CLICK_CENTER_HORIZONTAL = 4;

    public final static int BR_SORT_BY_PATH = 0;
    public final static int BR_SORT_BY_DATE = 1;
    public final static int BR_SORT_BY_SIZE = 2;
    public final static int BR_SORT_BY_TITLE = 3;// not possible

    public final static int NEXT_SCREEN_SCROLL_BY_PAGES = 0;

    public int doubleClickAction = DOUBLE_CLICK_ZOOM_IN_OUT;
    public int inactivityTime = 2;
    public int remindRestTime = 60;
    public int flippingInterval = 10;
    public int ttsTimer = 60;

    public boolean longTapEnable = true;

    public boolean isEditMode = true;
    public boolean isFullScreen = true;
    public boolean isAutoFit = false;
    public boolean notificationOngoing = false;

    public boolean isShowImages = true;
    public boolean isShowToolBar = true;
    public boolean isShowReadingProgress = true;
    public boolean isShowChaptersOnProgress = true;

    public int nextScreenScrollBy = NEXT_SCREEN_SCROLL_BY_PAGES;// 0 by
                                                                // pages,
                                                                // 25 - 25%
                                                                // persent

    public boolean isWhiteTheme = true;
    public boolean isOpenLastBook = false;

    public boolean isSortAsc = true;
    public int sortBy = AppDB.SORT_BY.PATH.ordinal();
    public int sortByBrowse = BR_SORT_BY_PATH;
    public boolean sortByReverse = false;

    public boolean isBrighrnessEnable = true;
    public boolean isRewindEnable = true;

    public int contrastImage = 0;
    public int brigtnessImage = 0;
    public boolean bolderTextOnImage = false;

    public float brightness = -1f;
    public float cropTolerance = 0.5f;

    public float ttsSpeed = 1.0f;
    public float ttsPitch = 1.0f;

    public List<Integer> nextKeys = NEXT_KEYS;
    public List<Integer> prevKeys = PREV_KEYS;
    public boolean isUseVolumeKeys = true;
    public boolean isReverseKeys = Dips.isSmallScreen();
    public boolean isMusicianMode = false;
    public String musicText = "Musician";

    public boolean isCrop = false;
    public boolean isCut = false;
    public boolean isDouble = false;
    public boolean isDoubleCoverAlone = false;

    public boolean isInvert = true;

    public int cpTextLight = Color.BLACK;
    public int cpBGLight = Color.WHITE;
    public int cpTextBlack = Color.WHITE;
    public int cpBGBlack = Color.BLACK;

    public boolean isUseBGImageDay = false;
    public boolean isUseBGImageNight = false;
    public String bgImageDayPath = MagicHelper.IMAGE_BG_1;
    public String bgImageNightPath = MagicHelper.IMAGE_BG_1;
    public int bgImageDayTransparency = DAY_TRANSPARENCY;
    public int bgImageNightTransparency = NIGHT_TRANSPARENCY;

    public String appLang = Urls.getLangCode();
    public float appFontScale = 1.0f;

    public boolean isLocked = false;
    public boolean isLoopAutoplay = false;
    public boolean isBookCoverEffect = false;

    public int editWith = EDIT_PEN;
    public String annotationDrawColor = "";
    public String annotationTextColor = COLORS.get(2);
    public int editAlphaColor = 100;
    public float editLineWidth = 3;

    public boolean isAlwaysOpenAsMagazine = false;
    public boolean isRememberMode = false;
    public boolean isInkMode = true;

    public volatile boolean isAutoScroll = false;
    public int autoScrollSpeed = 120;
    public int mouseWheelSpeed = 70;
    public String selectedText;

    // public int widgetHeigth = 100;
    public int widgetType = WIDGET_LIST;
    public int widgetItemsCount = 4;

    public int widgetSize = WIDGET_SIZE.get(1);

    public String rememberDict = "web:Google Translate";
    public boolean isRememberDictionary;

    public String fromLang = "en";
    public String toLang = Urls.getLangCode();

    public int orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;

    private static AppState instance = new AppState();
    private SharedPreferences sp;

    public int libraryMode = MODE_GRID;
    public int broseMode = MODE_LIST;
    public int recentMode = MODE_LIST;
    public int bookmarksMode = BOOKMARK_MODE_BY_DATE;
    public int starsMode = MODE_LIST;

    public boolean isBrowseGrid = false;
    public boolean isRecentGrid = false;

    public String searchPaths = Environment.getExternalStorageDirectory() == null ? "/" : Environment.getExternalStorageDirectory().getPath();
    public String texturePath = Environment.getExternalStorageDirectory().getPath();
    public String ttsSpeakPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
    public String downlodsPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Librera").getPath();

    public String fileToDelete;
    public String lastBookPath;
    public int lastBookPage = 0;

    public int colorDayText = COLOR_BLACK;
    public int colorDayBg = COLOR_WHITE;

    public int colorNigthText = COLOR_WHITE;
    public int colorNigthBg = COLOR_BLACK;

    public boolean supportPDF = true;
    public boolean supportXPS = false;
    public boolean supportDJVU = true;
    public boolean supportEPUB = true;
    public boolean supportFB2 = true;
    public boolean supportRTF = false;
    public boolean supportMOBI = true;
    public boolean supportCBZ = false;
    public boolean supportZIP = false;
    public boolean supportOther = false;

    public boolean supportTXT = false;
    public boolean isPreText = false;
    public boolean isLineBreaksText = false;
    public boolean isShowDroid = true;
    public boolean isIgnoreAnnotatations = false;
    public boolean isSaveAnnotatationsAutomatically = false;
    public boolean isShowWhatIsNewDialog = true;
    public boolean isShowCloseAppDialog = true;

    public boolean isFirstSurname = false;
    public boolean isOLED = false;

    public int cutP = 50;

    public volatile int fontSizeSp = Dips.isLargeScreen() ? 36 : 24;
    public volatile int statusBarTextSizeAdv = Dips.isLargeScreen() ? 16 : 14;
    public volatile int statusBarTextSizeEasy = Dips.isLargeScreen() ? 16 : 12;
    public volatile int progressLineHeight = Dips.isLargeScreen() ? 8 : 4;

    public String lastA;
    public String lastMode;
    public String dirLastPath;

    public String versionNew = "";

    public boolean isRTL = Urls.isRtl();
    public boolean isCutRTL = Urls.isRtl();

    // perofrmance
    public int pagesInMemory = 1;
    public float pageQuality = 1.2f;
    public int rotate = 0;
    public int rotateViewPager = 0;

    public int tapzoneSize = 25;
    public int allocatedMemorySize = (int) MemoryUtils.RECOMENDED_MEMORY_SIZE;
    public boolean isScrollAnimation = true;
    public String imageFormat = PNG;
    public boolean isCustomizeBgAndColors = false;
    public boolean isVibration = true;
    public boolean isLockPDF = false;
    public boolean selectingByLetters = Arrays.asList("ja", "zh", "ko", "vi").contains(Urls.getLangCode());

    public long installationDate = System.currentTimeMillis();
    public long searchDate = 0;

    public String customConfigColors = "";

    public boolean isStarsInWidget = false;

    public boolean isCropBookCovers = true;
    public boolean isBorderAndShadow = true;

    public boolean isBrowseImages = false;

    public int coverBigSize = (int) (((Dips.screenWidthDP() / (Dips.screenWidthDP() / 120)) - 8) * (Dips.isLargeScreen() ? 1.5f : 1));
    public int coverSmallSize = 80;

    public int tapZoneTop = TAP_PREV_PAGE;
    public int tapZoneBottom = TAP_NEXT_PAGE;
    public int tapZoneLeft = TAP_PREV_PAGE;
    public int tapZoneRight = TAP_NEXT_PAGE;

    public List<Integer> getNextKeys() {
        return isReverseKeys ? prevKeys : nextKeys;
    }

    public List<Integer> getPrevKeys() {
        return isReverseKeys ? nextKeys : prevKeys;
    }

    public static Map<String, String> getUserGuides() {
        final Map<String, String> providers = new LinkedHashMap<String, String>();
        providers.put("ru", "Русский");
        providers.put("en", "English");

        return providers;
    }

    public static Map<String, String> getDictionaries(String input) {
        final Map<String, String> providers = new LinkedHashMap<String, String>();
        String ln = AppState.get().toLang;
        String from = AppState.get().fromLang;
        String text = Uri.encode(input);
        providers.put("Google Translate", String.format("https://translate.google.com/#%s/%s/%s", from, ln, text));
        providers.put("Lingvo", String.format("http://www.lingvo-online.ru/en/Translate/%s-%s/%s", from, ln, text));

        providers.put("Dictionary.com", "http://dictionary.reference.com/browse/" + text);

        providers.put("Oxford", "http://www.oxforddictionaries.com/definition/english/" + text);
        providers.put("Longman", "http://www.ldoceonline.com/search/?q=" + text);
        providers.put("Cambridge", "http://dictionary.cambridge.org/dictionary/american-english/" + text);
        providers.put("Macmillan", "http://www.macmillandictionary.com/dictionary/british/" + text);
        providers.put("Collins", "http://www.collinsdictionary.com/dictionary/english/" + text);
        providers.put("Merriam-Webster", "http://www.merriam-webster.com/dictionary/" + text);
        providers.put("1tudien", "http://www.1tudien.com/?w=" + text);
        providers.put("Vdict", String.format("http://vdict.com/%s,1,0,0.html", text));
        providers.put("Google Search", String.format("http://www.google.com/search?q=%s", text));
        providers.put("Wikipedia", String.format("https://%s.wikipedia.org/wiki/%s", from, text));
        return providers;
    }

    public final static List<String> appDictionariesKeysTest = Arrays.asList(//
            "pdf" //
    //
    );

    public final static List<String> appDictionariesKeys = Arrays.asList(//
            "search", //
            "lingvo", //
            "dict", //
            "livio", //
            "tran", //
            "promt", //
            "fora", //
            "aard", //
            "web", //
            "woordenboek"// https://play.google.com/store/apps/details?id=com.prisma.woordenboek.englesxl

    //
    );

    public static synchronized AppState getInstance() {
        return instance;
    }

    public static synchronized AppState get() {
        return instance;
    }

    private boolean isLoaded = false;

    public void defaults(Context c) {
        musicText = c.getString(R.string.musician);
        if (AppsConfig.IS_CLASSIC) {
            AppState.get().tabsOrder = DEFAULTS_TABS_ORDER.replace(UITab.OpdsFragment.index + "#1", UITab.OpdsFragment.index + "#0");
        }
        if (AppState.get().isInkMode || AppsConfig.IS_INK) {
            AppsConfig.ADMOB_FULLSCREEN = null;
            AppState.getInstance().isInkMode = true;
            AppState.getInstance().isInvert = true;
            AppState.getInstance().isEditMode = true;
            AppState.getInstance().isRememberMode = true;
            AppState.getInstance().isAlwaysOpenAsMagazine = true;
            AppState.getInstance().isMusicianMode = false;
            AppState.getInstance().isReverseKeys = true;
            AppState.getInstance().isScrollAnimation = false;
            AppState.getInstance().tintColor = Color.BLACK;

        }
        LOG.d("defaults", AppsConfig.IS_CLASSIC, AppState.get().tabsOrder);
    }

    public void load(final Context a) {
        try {
            if (!isLoaded) {
                AppState.get().isInkMode = Dips.isEInk(a);
                defaults(a);
                loadIn(a);
                BookCSS.get().load(a);
                DragingPopup.loadCache(a);
                LOG.d("AppState Load lasta", lastA);
            } else {
                LOG.d("AppState is Loaded", lastA);
            }
            isLoaded = true;
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    public static float getAsFloatOrInt(SharedPreferences sp, String name, float init) {
        try {
            return sp.getFloat(name, init);
        } catch (final Exception e) {
            return sp.getInt(name, (int) init);
        }
    }

    public void loadIn(final Context a) {
        sp = a.getSharedPreferences(ExportSettingsManager.PREFIX_PDF, Context.MODE_PRIVATE);

        isCropBookCovers = sp.getBoolean("isCropBookCovers", isCropBookCovers);
        isBorderAndShadow = sp.getBoolean("isBorderAndShadow", isBorderAndShadow);
        isBrowseImages = sp.getBoolean("isBrowseImages", isBrowseImages);

        longTapEnable = sp.getBoolean("longTapEnable", longTapEnable);
        isEditMode = sp.getBoolean("isEditMode", isEditMode);
        isBrowseGrid = sp.getBoolean("isBrowseGrid", isBrowseGrid);
        isRecentGrid = sp.getBoolean("isRecentGrid", isRecentGrid);
        isFullScreen = sp.getBoolean("isFullScrean", isFullScreen);
        notificationOngoing = sp.getBoolean("notificationOngoing", notificationOngoing);
        isShowToolBar = sp.getBoolean("isShowToolBar", isShowToolBar);
        isShowImages = sp.getBoolean("isShowImages", isShowImages);
        isShowReadingProgress = sp.getBoolean("isShowReadingProgress", isShowReadingProgress);
        isShowChaptersOnProgress = sp.getBoolean("isShowChaptersOnProgress", isShowChaptersOnProgress);
        isWhiteTheme = sp.getBoolean("isWhiteTheme", isWhiteTheme);
        isOpenLastBook = sp.getBoolean("isOpenLastBook", isOpenLastBook);
        orientation = sp.getInt("orientation", orientation);
        mouseWheelSpeed = sp.getInt("mouseWheelSpeed", mouseWheelSpeed);
        rotate = sp.getInt("rotate", rotate);
        rotateViewPager = sp.getInt("rotateViewPager", rotateViewPager);
        tapzoneSize = sp.getInt("tapzoneSize", tapzoneSize);
        allocatedMemorySize = sp.getInt("allocatedMemorySize", allocatedMemorySize);

        pagesInMemory = sp.getInt("pagesInMemory", pagesInMemory);

        pageQuality = getAsFloatOrInt(sp, "pageQuality", pageQuality);
        brightness = getAsFloatOrInt(sp, "brightness1", brightness);
        cropTolerance = getAsFloatOrInt(sp, "cropTolerance", cropTolerance);
        ttsSpeed = getAsFloatOrInt(sp, "ttsSpeed", ttsSpeed);
        ttsPitch = getAsFloatOrInt(sp, "ttsPitch", ttsPitch);
        editLineWidth = getAsFloatOrInt(sp, "editLineWidth", editLineWidth);
        appFontScale = getAsFloatOrInt(sp, "appFontScale", appFontScale);

        isSortAsc = sp.getBoolean("isSortAsc", isSortAsc);
        sortByReverse = sp.getBoolean("sortByReverse", sortByReverse);
        isBrighrnessEnable = sp.getBoolean("isBrighrnessEnable", isBrighrnessEnable);
        isRewindEnable = sp.getBoolean("isRewindEnable", isRewindEnable);
        isReverseKeys = sp.getBoolean("isReverseKeys", isReverseKeys);
        isUseVolumeKeys = sp.getBoolean("isUseVolumeKeys", isUseVolumeKeys);
        isRememberMode = sp.getBoolean("isRememberMode1", isRememberMode);
        isInkMode = sp.getBoolean("isInkMode", isInkMode);

        // isCrop = sp.getBoolean("isCrop", isCrop);
        // isCut = sp.getBoolean("isCut", isCut);
        // isDouble = sp.getBoolean("isDouble", isDouble);

        isInvert = sp.getBoolean("isInvert", isInvert);

        isLoopAutoplay = sp.getBoolean("isLoopAutoplay", isLoopAutoplay);
        isBookCoverEffect = sp.getBoolean("isBookCoverEffect", isBookCoverEffect);
        isMusicianMode = sp.getBoolean("isReverseTaps", isMusicianMode);

        isAlwaysOpenAsMagazine = sp.getBoolean("isOlwaysOpenAsMagazine", isAlwaysOpenAsMagazine);

        nextKeys = stringToKyes(sp.getString("nextKeys1", keyToString(NEXT_KEYS)));
        prevKeys = stringToKyes(sp.getString("prevKeys1", keyToString(PREV_KEYS)));

        cpTextLight = sp.getInt("cpTextLight", cpTextLight);
        cpBGLight = sp.getInt("cpBGLight", cpBGLight);
        cpTextBlack = sp.getInt("cpTextBlack", cpTextBlack);
        cpBGBlack = sp.getInt("cpBGBlack", cpBGBlack);

        libraryMode = sp.getInt("libraryMode", libraryMode);
        starsMode = sp.getInt("starsMode", starsMode);
        broseMode = sp.getInt("broseMode", broseMode);
        recentMode = sp.getInt("recentMode", recentMode);
        bookmarksMode = sp.getInt("bookmarksMode", bookmarksMode);
        isRememberDictionary = sp.getBoolean("isRememberDictionary", isRememberDictionary);
        // isExpirementalFeatures =
        // sharedPreferences.getBoolean("isExpirementalFeatures",
        // isExpirementalFeatures);

        widgetItemsCount = sp.getInt("widgetItemsCount", widgetItemsCount);
        widgetType = sp.getInt("widgetType", widgetType);
        widgetSize = sp.getInt("widgetSize", widgetSize);

        sortBy = sp.getInt("sortBy", SORT_BY_PATH);
        sortByBrowse = sp.getInt("sortByBrowse", SORT_BY_PATH);
        contrastImage = sp.getInt("contrastImage", contrastImage);
        brigtnessImage = sp.getInt("brigtnessImage", brigtnessImage);
        searchPaths = sp.getString("searchPaths", searchPaths);
        rememberDict = sp.getString("rememberDict", rememberDict);

        fileToDelete = sp.getString("fileToDelete", fileToDelete);
        lastBookPath = sp.getString("lastBookPath", lastBookPath);
        lastBookPage = sp.getInt("lastBookPage", lastBookPage);
        lastA = sp.getString("lastA", lastA);
        lastMode = sp.getString("lastMode", lastMode);
        dirLastPath = sp.getString("dirLastPath", dirLastPath);
        versionNew = sp.getString("versionNew", versionNew);
        musicText = sp.getString("musicText", musicText);

        colorDayText = sp.getInt("colorDayText", colorDayText);
        colorDayBg = sp.getInt("colorDayBg", colorDayBg);
        colorNigthText = sp.getInt("colorNigthText", colorNigthText);
        colorNigthBg = sp.getInt("colorNigthBg", colorNigthBg);

        tintColor = sp.getInt("tintColor", tintColor);
        statusBarColorDay = sp.getInt("statusBarColorDay", statusBarColorDay);
        statusBarColorNight = sp.getInt("statusBarColorNight", statusBarColorNight);
        userColor = sp.getInt("userColor", userColor);
        fontSizeSp = sp.getInt("fontSizeSp", fontSizeSp);
        statusBarTextSizeAdv = sp.getInt("statusBarTextSizeAdv", statusBarTextSizeAdv);
        statusBarTextSizeEasy = sp.getInt("statusBarTextSizeEasy", statusBarTextSizeEasy);
        progressLineHeight = sp.getInt("progressLineHeight", progressLineHeight);

        doubleClickAction = sp.getInt("doubleClickAction", doubleClickAction);
        inactivityTime = sp.getInt("inactivityTime", inactivityTime);
        remindRestTime = sp.getInt("remindRestTime", remindRestTime);
        flippingInterval = sp.getInt("flippingInterval", flippingInterval);
        ttsTimer = sp.getInt("ttsTimer", ttsTimer);

        supportPDF = sp.getBoolean("supportPDF", supportPDF);
        supportXPS = sp.getBoolean("supportXPS", supportXPS);
        supportDJVU = sp.getBoolean("supportDJVU", supportDJVU);
        supportEPUB = sp.getBoolean("supportEPUB", supportEPUB);
        supportFB2 = sp.getBoolean("supportFB2", supportFB2);
        supportTXT = sp.getBoolean("supportTXT", supportTXT);
        supportRTF = sp.getBoolean("supportRTF", supportRTF);
        supportMOBI = sp.getBoolean("supportMOBI", supportMOBI);
        supportCBZ = sp.getBoolean("supportCBZ", supportCBZ);
        supportZIP = sp.getBoolean("supportZIP", supportZIP);
        supportOther = sp.getBoolean("supportOther", supportOther);
        isPreText = sp.getBoolean("isPreText", isPreText);
        isLineBreaksText = sp.getBoolean("isLineBreaksText", isLineBreaksText);

        isShowDroid = sp.getBoolean("isShowDroid", isShowDroid);
        isRTL = sp.getBoolean("isRTL", isRTL);
        isCutRTL = sp.getBoolean("isCutRTL", isCutRTL);
        isScrollAnimation = sp.getBoolean("isScrollAnimation", isScrollAnimation);
        isCustomizeBgAndColors = sp.getBoolean("isCustomizeBgAndColors", isCustomizeBgAndColors);
        isVibration = sp.getBoolean("isVibration", isVibration);
        isLockPDF = sp.getBoolean("isLockPDF", isLockPDF);
        selectingByLetters = sp.getBoolean("selectingByLetters", selectingByLetters);
        isStarsInWidget = sp.getBoolean("isStarsInWidget", isStarsInWidget);

        isIgnoreAnnotatations = sp.getBoolean("isIgnoreAnnotatations", isIgnoreAnnotatations);
        isSaveAnnotatationsAutomatically = sp.getBoolean("isSaveAnnotatationsAutomatically", isSaveAnnotatationsAutomatically);
        isShowWhatIsNewDialog = sp.getBoolean("isShowWhatIsNewDialog", isShowWhatIsNewDialog);
        isShowCloseAppDialog = sp.getBoolean("isShowCloseAppDialog", isShowCloseAppDialog);
        isFirstSurname = sp.getBoolean("isFirstSurname", isFirstSurname);
        isOLED = sp.getBoolean("isOLED", isOLED);

        imageFormat = sp.getString("imageFormat", imageFormat);
        fromLang = sp.getString("fromLang", fromLang);
        toLang = sp.getString("toLang", toLang);
        appLang = sp.getString("appLang", appLang);

        customConfigColors = sp.getString("customConfigColors", customConfigColors);

        installationDate = sp.getLong("installationDate", installationDate);
        searchDate = sp.getLong("searchDate", searchDate);

        // custom bgs
        isUseBGImageDay = sp.getBoolean("isUseBGImageDay", isUseBGImageDay);
        isUseBGImageNight = sp.getBoolean("isUseBGImageNight", isUseBGImageNight);

        bgImageDayPath = sp.getString("bgImageDayPath", bgImageDayPath);
        bgImageNightPath = sp.getString("bgImageNightPath", bgImageNightPath);
        texturePath = sp.getString("texturePath", texturePath);
        ttsSpeakPath = sp.getString("ttsSpeakPath", ttsSpeakPath);
        downlodsPath = sp.getString("downlodsPath", downlodsPath);
        readColors = sp.getString("readColors", readColors);
        myOPDS = sp.getString("myOPDS", myOPDS);
        tabsOrder = sp.getString("tabsOrder2", tabsOrder);

        bgImageDayTransparency = sp.getInt("bgImageDayTransparency", bgImageDayTransparency);
        bgImageNightTransparency = sp.getInt("bgImageNightTransparency", bgImageNightTransparency);

        coverSmallSize = sp.getInt("coverSmallSize", coverSmallSize);
        coverBigSize = sp.getInt("coverBigSize", coverBigSize);

        tapZoneTop = sp.getInt("tapZoneTop", tapZoneTop);
        tapZoneBottom = sp.getInt("tapZoneBottom", tapZoneBottom);
        tapZoneLeft = sp.getInt("tapZoneLeft", tapZoneLeft);
        tapZoneRight = sp.getInt("tapZoneRight", tapZoneRight);
        nextScreenScrollBy = sp.getInt("nextScreenScrollBy", nextScreenScrollBy);

        LOG.d("LOAD AppState", "coverSmallSize", coverSmallSize);
    }

    public static String keyToString(final List<Integer> list) {
        Collections.sort(list);
        final StringBuilder line = new StringBuilder();
        for (final int value : list) {
            line.append(value);
            line.append(",");
        }
        return line.toString();
    }

    public static List<Integer> stringToKyes(final String list) {
        final List<Integer> res = new ArrayList<Integer>();

        for (final String value : list.split(",")) {
            if (value != null && !value.trim().equals("")) {
                res.add(new Integer(value.trim()));
            }
        }
        Collections.sort(res);
        return res;
    }

    public synchronized void save(final Context a) {
        try {
            saveIn(a);
            BookCSS.get().save(a);
            DragingPopup.saveCache(a);
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    @Override
    public int hashCode() {
        try {
            int hashCode = sp.getAll().hashCode();
            LOG.d("AppState hash", hashCode);
            return hashCode;
        } catch (Exception e) {
            return 0;
        }
    }

    public void saveIn(final Context a) {
        if (a == null) {
            return;
        }
        sp = a.getSharedPreferences(ExportSettingsManager.PREFIX_PDF, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sp.edit();

        try {
            editor.putString("appName", AppsConfig.APP_NAME);
            editor.putString("appVersion", Apps.getVersionName(a));
        } catch (Exception e) {
            LOG.e(e);
        }

        editor.putBoolean("isCropBookCovers", isCropBookCovers);
        editor.putBoolean("isBorderAndShadow", isBorderAndShadow);
        editor.putBoolean("isBrowseImages", isBrowseImages);
        editor.putBoolean("longTapEnable", longTapEnable);
        editor.putBoolean("isEditMode", isEditMode);
        editor.putBoolean("isFullScrean", isFullScreen);
        editor.putBoolean("notificationOngoing", notificationOngoing);
        editor.putBoolean("isShowToolBar", isShowToolBar);
        editor.putBoolean("isShowImages", isShowImages);
        editor.putBoolean("isShowReadingProgress", isShowReadingProgress);
        editor.putBoolean("isShowChaptersOnProgress", isShowChaptersOnProgress);
        editor.putBoolean("isWhiteTheme", isWhiteTheme);
        editor.putBoolean("isOpenLastBook", isOpenLastBook);
        editor.putBoolean("isRememberMode1", isRememberMode);
        editor.putBoolean("isInkMode", isInkMode);
        editor.putBoolean("isBrowseGrid", isBrowseGrid);
        editor.putBoolean("isRecentGrid", isRecentGrid);

        editor.putInt("orientation", orientation);
        editor.putInt("mouseWheelSpeed", mouseWheelSpeed);
        editor.putInt("rotate", rotate);
        editor.putInt("rotateViewPager", rotateViewPager);
        editor.putInt("tapzoneSize", tapzoneSize);
        editor.putInt("allocatedMemorySize", allocatedMemorySize);

        editor.putInt("pagesInMemory", pagesInMemory);
        editor.putFloat("pageQuality", pageQuality);
        editor.putFloat("editLineWidth", editLineWidth);
        editor.putFloat("appFontScale", appFontScale);

        editor.putBoolean("isSortAsc", isSortAsc);
        editor.putBoolean("sortByReverse", sortByReverse);
        editor.putBoolean("isBrighrnessEnable", isBrighrnessEnable);
        editor.putBoolean("isRewindEnable", isRewindEnable);
        editor.putBoolean("isReverseKeys", isReverseKeys);
        editor.putBoolean("isUseVolumeKeys", isUseVolumeKeys);

        // editor.putBoolean("isCrop", isCrop);
        // editor.putBoolean("isCut", isCut);
        // editor.putBoolean("isDouble", isDouble);

        editor.putBoolean("isInvert", isInvert);

        editor.putBoolean("isLoopAutoplay", isLoopAutoplay);
        editor.putBoolean("isBookCoverEffect", isBookCoverEffect);
        editor.putInt("libraryMode", libraryMode);
        editor.putInt("starsMode", starsMode);
        editor.putInt("broseMode", broseMode);
        editor.putInt("recentMode", recentMode);
        editor.putInt("bookmarksMode", bookmarksMode);
        editor.putBoolean("isReverseTaps", isMusicianMode);

        editor.putBoolean("isOlwaysOpenAsMagazine", isAlwaysOpenAsMagazine);
        // editor.putBoolean("isExpirementalFeatures", isExpirementalFeatures);
        editor.putBoolean("isRememberDictionary", isRememberDictionary);

        editor.putInt("sortBy", sortBy);
        editor.putInt("sortByBrowse", sortByBrowse);
        editor.putInt("contrastImage", contrastImage);
        editor.putInt("brigtnessImage", brigtnessImage);
        editor.putFloat("brightness1", brightness);
        editor.putFloat("cropTolerance", cropTolerance);
        editor.putFloat("ttsSpeed", ttsSpeed);
        editor.putFloat("ttsPitch", ttsPitch);

        editor.putString("nextKeys1", keyToString(nextKeys));
        editor.putString("prevKeys1", keyToString(prevKeys));

        editor.putInt("cpTextLight", cpTextLight);
        editor.putInt("cpBGLight", cpBGLight);
        editor.putInt("cpTextBlack", cpTextBlack);
        editor.putInt("cpBGBlack", cpBGBlack);
        editor.putString("searchPaths", searchPaths);
        editor.putInt("widgetItemsCount", widgetItemsCount);
        editor.putInt("widgetType", widgetType);
        editor.putInt("widgetSize", widgetSize);
        editor.putString("rememberDict", rememberDict);
        editor.putString("recurcive", null);
        editor.putString("fileToDelete", fileToDelete);
        editor.putString("lastBookPath", lastBookPath);
        editor.putInt("lastBookPage", lastBookPage);
        editor.putString("lastA", lastA);
        editor.putString("lastMode", lastMode);
        editor.putString("dirLastPath", dirLastPath);
        editor.putString("versionNew", versionNew);
        editor.putString("musicText", musicText);

        editor.putInt("colorDayBg", colorDayBg);
        editor.putInt("colorDayText", colorDayText);

        editor.putInt("colorNigthBg", colorNigthBg);
        editor.putInt("colorNigthText", colorNigthText);

        editor.putInt("tintColor", tintColor);
        editor.putInt("statusBarColorDay", statusBarColorDay);
        editor.putInt("statusBarColorNight", statusBarColorNight);
        editor.putInt("userColor", userColor);
        editor.putInt("fontSizeSp", fontSizeSp);
        editor.putInt("statusBarTextSizeAdv", statusBarTextSizeAdv);
        editor.putInt("statusBarTextSizeEasy", statusBarTextSizeEasy);
        editor.putInt("progressLineHeight", progressLineHeight);
        editor.putInt("doubleClickAction", doubleClickAction);
        editor.putInt("inactivityTime", inactivityTime);
        editor.putInt("remindRestTime", remindRestTime);
        editor.putInt("flippingInterval", flippingInterval);
        editor.putInt("ttsTimer", ttsTimer);

        editor.putBoolean("supportPDF", supportPDF);
        editor.putBoolean("supportXPS", supportXPS);
        editor.putBoolean("supportDJVU", supportDJVU);
        editor.putBoolean("supportEPUB", supportEPUB);
        editor.putBoolean("supportFB2", supportFB2);
        editor.putBoolean("supportTXT", supportTXT);
        editor.putBoolean("supportMOBI", supportMOBI);
        editor.putBoolean("supportCBZ", supportCBZ);
        editor.putBoolean("supportRTF", supportRTF);
        editor.putBoolean("supportZIP", supportZIP);
        editor.putBoolean("supportOther", supportOther);

        editor.putBoolean("isPreText", isPreText);
        editor.putBoolean("isLineBreaksText", isLineBreaksText);
        editor.putBoolean("isShowDroid", isShowDroid);
        editor.putBoolean("isRTL", isRTL);
        editor.putBoolean("isCutRTL", isCutRTL);
        editor.putBoolean("isScrollAnimation", isScrollAnimation);
        editor.putBoolean("isCustomizeBgAndColors", isCustomizeBgAndColors);
        editor.putBoolean("isVibration", isVibration);
        editor.putBoolean("isLockPDF", isLockPDF);
        editor.putBoolean("selectingByLetters", selectingByLetters);
        editor.putBoolean("isStarsInWidget", isStarsInWidget);
        editor.putBoolean("isIgnoreAnnotatations", isIgnoreAnnotatations);
        editor.putBoolean("isSaveAnnotatationsAutomatically", isSaveAnnotatationsAutomatically);
        editor.putBoolean("isShowWhatIsNewDialog", isShowWhatIsNewDialog);
        editor.putBoolean("isShowCloseAppDialog", isShowCloseAppDialog);
        editor.putBoolean("isFirstSurname", isFirstSurname);
        editor.putBoolean("isOLED", isOLED);

        editor.putString("imageFormat", imageFormat);

        editor.putString("fromLang", fromLang);
        editor.putString("toLang", toLang);
        editor.putString("appLang", appLang);
        editor.putString("customConfigColors", customConfigColors);
        editor.putLong("installationDate", installationDate);
        editor.putLong("searchDate", searchDate);

        // custom bgs
        editor.putBoolean("isUseBGImageDay", isUseBGImageDay);
        editor.putBoolean("isUseBGImageNight", isUseBGImageNight);

        editor.putString("bgImageDayPath", bgImageDayPath);
        editor.putString("bgImageNightPath", bgImageNightPath);
        editor.putString("texturePath", texturePath);
        editor.putString("ttsSpeakPath", ttsSpeakPath);
        editor.putString("downlodsPath", downlodsPath);
        editor.putString("readColors", readColors);
        editor.putString("myOPDS", myOPDS);
        editor.putString("tabsOrder2", tabsOrder);

        editor.putInt("bgImageDayTransparency", bgImageDayTransparency);
        editor.putInt("bgImageNightTransparency", bgImageNightTransparency);

        editor.putInt("coverSmallSize", coverSmallSize);
        editor.putInt("coverBigSize", coverBigSize);

        editor.putInt("tapZoneTop", tapZoneTop);
        editor.putInt("tapZoneBottom", tapZoneBottom);
        editor.putInt("tapZoneLeft", tapZoneLeft);
        editor.putInt("tapZoneRight", tapZoneRight);
        editor.putInt("nextScreenScrollBy", nextScreenScrollBy);

        editor.commit();

        LOG.d("Save AppState", "coverSmallSize", coverSmallSize);
        LOG.d("AppState Save lasta", lastA, a.getClass());
    }

    public boolean isFullScrean() {
        return isFullScreen;
    }

    public void setFullScrean(final boolean isFullScrean) {
        this.isFullScreen = isFullScrean;
    }

    public static void outOfMemoryHack() {
        AppState.get().pagesInMemory--;
        if (AppState.get().pagesInMemory < 0) {
            AppState.get().pagesInMemory = 0;
        }
    }

}