package com.foobnix.model;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.view.KeyEvent;

import com.foobnix.android.utils.Apps;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.IO;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.MemoryUtils;
import com.foobnix.android.utils.Objects;
import com.foobnix.android.utils.Objects.IgnoreHashCode;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.opds.SamlibOPDS;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.Urls;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.MagicHelper;
import com.foobnix.pdf.info.wrapper.UITab;
import com.foobnix.ui2.AppDB;

import org.librera.LinkedJSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class AppState {


    public static final String PROXY_HTTP = "HTTP";
    public static final String PROXY_SOCKS = "SOCKS";

    public static final String TEXT_COLOR_DAY = "#5b5b5b";
    public static final String TEXT_COLOR_NIGHT = "#8e8e8e";

    public static final long APP_CLOSE_AUTOMATIC = TimeUnit.MINUTES.toMillis(500);// SECONDS, MINUTES
    public static final long APP_UPDATE_TIME_IN_UI = TimeUnit.SECONDS.toMillis(30);
    // public static final long APP_CLOSE_AUTOMATIC =
    // TimeUnit.SECONDS.toMillis(5);
    public static final int DAY_TRANSPARENCY = 200;
    public static final int NIGHT_TRANSPARENCY = 160;
    public static final String PNG = "PNG";
    public static final String JPG = "JPG";
    public static final String[] LIBRE_EXT = ".odp, .pptx, .ppt".split(", ");
    public static final String[] OTHER_BOOK_EXT = ".abw, .docm, .lwp, .n, .rst, .sdw, .tex, .wpd, .wps, .zabw, .cbc, .chm, .lit, .lrf, .oeb, .pml, .rb, .snb, .tcr, .txtz, .azw1, .tpz".split(", ");
    public static final String[] OTHER_ARCH_EXT = ".img, .rar, .7z, .arj, .bz2, .bzip2, .tbz2, .tbz, .txz, .cab, .gz, .gzip, .tgz, .iso, .lzh, .lha, .lzma, .tar, .xar, .z, .taz, .xz, .dmg".split(", ");
    public static final String PREF_SCROLL_MODE = "pdf, djvu";
    public static final String PREF_BOOK_MODE = "epub, mobi, fb2, azw, azw3";
    public static final String PREF_MUSIC_MODE = "";
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_DARK_OLED = 2;
    public static final int THEME_INK = 3;
    public static final int FULL_SCREEN_NORMAL = 0;
    public static final int FULL_SCREEN_FULLSCREEN = 1;
    public static final int FULL_SCREEN_FULLSCREEN_CUTOUT = 2;
    public static final List<String> COLORS = Arrays.asList(//
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
            "#EA5964", //
            "#00897B", //
            "#000000" //

    );
    public static final List<String> ACCENT_COLORS = Arrays.asList(//
            "#E3C800", //
            "#EA5964", //
            "#00897B", //
            "#FFFFFF" //

    );
    public final static String OPDS_DEFAULT = "" + //

            //"https://www.feedbooks.com/catalog.atom,Feedbooks,Free ebooks,assets://opds/feedbooks.ico;" + //
            "https://m.gutenberg.org/ebooks.opds/,Project Gutenberg,Free ebooks since 1971,assets://opds/opds.png;" + //
            "https://bookserver.archive.org/catalog/,Internet Archive,Internet Archive,assets://opds/opds.png;" + //
//            "http://opds.oreilly.com/opds/,O'Reilly,O'Reilly,assets://opds/opds.png;" + //
            //"http://www.epubbud.com/feeds/catalog.atom,ePubBud Children's books,ePubBud Children's books,assets://opds/opds.png;" + //

            SamlibOPDS.ROOT_AWARDS + ",Top Books to Read, Complete award winners listing,assets://opds/rating.png;" //
            // end
            ;
    public final static String READ_COLORS_DEAFAUL =
            // (name),(bg),(text),(0-day 1-nigth)
            "" + //
                    "1,#ffffff,#000000,0;" + //
                    "2,#f2f0e9,#383226,0;" + //
                    "3,#f9f5e8,#333333,0;" + //
                    //
                    "A,#3a3a3a,#c8c8c8,1;" + //
                    "B,#000000,#8cffb5,1;" + //
                    "C,#000000,#ffffff,1;"; //
    public static final String TTS_REPLACEMENTS =

            "{'*[()\"«»*”“/\\\\[\\\\]]':' ' , " +//
                    "'*[?!:;–|—|―]':'. ' , " +//
                    "'it’s':'it is' , " +//
                    "'#bla':'bla disabled' , " +//
                    "'*(L|l)ibre.':'$1ibréra'}";//
    public static final String TTS_PUNCUATIONS = ".;:!?";
    public final static String DEFAULTS_TABS_ORDER = "0#1,1#1,2#1,3#1,4#1,5#1,6#0,7#0";
    final public static List<Integer> WIDGET_SIZE = Arrays.asList(0, 70, 100, 150, 200, 250);
    public final static int MAX_SPEED = 149;
    public final static int MODE_GRID = 1;
    public final static int MODE_LIST = 2;
    public final static int MODE_COVERS = 3;
    public final static int MODE_AUTHORS = 4;
    public final static int MODE_GENRE = 5;
    public final static int MODE_SERIES = 6;
    public final static int MODE_LIST_COMPACT = 7;
    public final static int MODE_USER_TAGS = 8;
    public final static int MODE_KEYWORDS = 9;
    public final static int MODE_LANGUAGES = 10;
    public final static int MODE_PUBLICATION_DATE = 11;
    public final static int MODE_PUBLISHER = 12;
    public final static int BOOKMARK_MODE_BY_DATE = 1;
    public final static int BOOKMARK_MODE_BY_BOOK = 2;
    // end
    public final static int DOUBLE_CLICK_AUTOSCROLL = 0;
    public final static int DOUBLE_CLICK_ADJUST_PAGE = 1;
    public final static int DOUBLE_CLICK_NOTHING = 2;
    public final static int DOUBLE_CLICK_ZOOM_IN_OUT = 3;
    public final static int DOUBLE_CLICK_CENTER_HORIZONTAL = 4;
    public final static int DOUBLE_CLICK_CLOSE_BOOK = 5;
    public final static int DOUBLE_CLICK_CLOSE_BOOK_AND_APP = 6;
    public final static int DOUBLE_CLICK_CLOSE_HIDE_APP = 7;
    public final static int DOUBLE_CLICK_START_STOP_TTS = 8;
    public final static int BR_SORT_BY_PATH = 0;
    public final static int BR_SORT_BY_DATE = 1;
    public final static int BR_SORT_BY_SIZE = 2;
    public final static int BR_SORT_BY_TITLE = 3;// not possible
    public final static int BR_SORT_BY_NUMBER = 4;// not possible
    public final static int BR_SORT_BY_PAGES = 5;// not possible
    public final static int BR_SORT_BY_EXT = 6;// not possible
    public final static int BR_SORT_BY_AUTHOR = 7;// not possible
    public final static int NEXT_SCREEN_SCROLL_BY_PAGES = 0;
    public final static int OUTLINE_HEADERS_AND_SUBHEADERES = 0;
    public final static int OUTLINE_ONLY_HEADERS = 1;
    public final static int PAGE_NUMBER_FORMAT_NUMBER = 0;
    public final static int PAGE_NUMBER_FORMAT_PERCENT = 1;
    public final static int CHAPTER_FORMAT_1 = 0;
    public final static int CHAPTER_FORMAT_2 = 1;
    public final static int CHAPTER_FORMAT_3 = 2;
    public final static int AUTO_BRIGTNESS = -1000;
    public final static int READING_MODE_SCROLL = 1;
    public final static int READING_MODE_BOOK = 2;
    public final static int READING_MODE_MUSICIAN = 3;
    public final static int READING_MODE_TAG_MANAGER = 4;
    public final static int READING_MODE_OPEN_WITH = 5;
    public final static List<String> appDictionariesKeys = Arrays.asList(//
            "search", //
            "lingvo", //
            "linguee", //
            "dic", //
            "livio", //
            "tran", //
            "promt", //
            "fora", //
            "aard", //
            "web", //
            "encyc", // encyclopedias
            "oxford", //
            "mobifusion", //
            "cambridge", //
            "longman", //
            "oup",//
            "engl"//

            //
    );
    public static final List<String> langCodes = Arrays.asList("ar"
            , "be"
            , "bg"
            , "ca"
            , "cs"
            , "de"
            , "el"
            , "en"
            , "es"
            , "eu"
            , "fa"
            , "fi"
            , "fr"
            , "ga"
            , "he"
            , "hi"
            , "hu"
            , "id"
            , "it"
            , "ja"
            , "kk"
            , "ko"
            , "la"
            , "lt"
            , "ml"
            , "nl"
            , "no"
            , "pl"
            , "pt"
            , "ro"
            , "ru"
            , "sc"
            , "sk"
            , "sv"
            , "sw"
            , "ta"
            , "th"
            , "tr"
            , "uk"
            , "vi"
            , "zh-rCN"
            , "zh-rTW");
    public static final int BOOKMARK_SORT_PAGE_ASC = 0;
    public static final int BOOKMARK_SORT_PAGE_DESC = 1;
    public static final int BOOKMARK_SORT_DATE_ASC = 2;
    public static final int BOOKMARK_SORT_DATE_DESC = 3;
    public static Map<String, String[]> CONVERTERS = new LinkedHashMap<>();
    public static Map<String, String> TTS_ENGINES = new LinkedHashMap<>();
    public static int COLOR_WHITE = Color.WHITE;
    public static int COLOR_WHITE_2 = Color.parseColor("#c8c8c8");
    // public static int COLOR_BLACK = Color.parseColor("#030303");
    public static int COLOR_BLACK = Color.BLACK;
    public static int COLOR_BLACK_2 = Color.parseColor("#3a3a3a");
    public static int WIDGET_LIST = 1;
    public static int WIDGET_GRID = 2;
    public static int EDIT_NONE = 0;
    public static int EDIT_PEN = 1;
    public static int EDIT_DELETE = 2;
    public static int TAP_NEXT_PAGE = 0;
    public static int TAP_PREV_PAGE = 1;
    public static int TAP_DO_NOTHING = 2;
    public static int STATUSBAR_POSITION_TOP = 1;
    public static int STATUSBAR_POSITION_BOTTOM = 2;
    public static int BLUE_FILTER_DEFAULT_COLOR = Color.BLACK;
    public static String MY_SYSTEM_LANG = "my";
    public static List<Integer> NEXT_KEYS = Arrays.asList(//
            KeyEvent.KEYCODE_VOLUME_UP, //
            KeyEvent.KEYCODE_PAGE_UP, //
            // KeyEvent.KEYCODE_DPAD_UP,//
            KeyEvent.KEYCODE_DPAD_RIGHT, //
            KeyEvent.KEYCODE_MEDIA_NEXT, //
            94, //
            105 //
            // KeyEvent.KEYCODE_DEL//
    );
    public static List<Integer> PREV_KEYS = Arrays.asList(//
            KeyEvent.KEYCODE_VOLUME_DOWN, //
            KeyEvent.KEYCODE_PAGE_DOWN, //
            // KeyEvent.KEYCODE_DPAD_DOWN, //
            KeyEvent.KEYCODE_DPAD_LEFT, //
            KeyEvent.KEYCODE_MEDIA_PREVIOUS, //
            95, //
            106 //
            // KeyEvent.KEYCODE_ENTER //

    );
    private static AppState instance = new AppState();

    static {
        CONVERTERS.put("PDF", "https://cloudconvert.com/anything-to-pdf, https://topdf.com, https://www.zamzar.com".split(", "));
        CONVERTERS.put("PDF Rotate", "https://www.pdfrotate.com, https://smaltilpdf.com/rotate-pdf, https://www.rotatepdf.net".split(", "));
        CONVERTERS.put("EPUB", "https://cloudconvert.com/anything-to-epub, https://toepub.com".split(", "));
        CONVERTERS.put("MOBI", "https://cloudconvert.com/anything-to-mobi, https://toepub.com".split(", "));
        CONVERTERS.put("AZW3", "https://cloudconvert.com/anything-to-azw3, https://toepub.com".split(", "));
        CONVERTERS.put("DOCX", "https://cloudconvert.com/anything-to-docx, https://document.online-convert.com/convert-to-docx, https://pdf2docx.com/".split(", "));
        CONVERTERS.put("FB2", "https://cloudconvert.com/anything-to-fb2, https://ebook.online-convert.com/convert-to-fb2".split(", "));

    }

    static {
        TTS_ENGINES.put("Google Text-to-Speech", "https://play.google.com/store/apps/details?id=com.google.android.tts");
        TTS_ENGINES.put("Acapela TTS Voices", "https://play.google.com/store/apps/details?id=com.acapelagroup.android.tts");
        TTS_ENGINES.put("Vocalizer TTS Voice", "https://play.google.com/store/apps/details?id=es.codefactory.vocalizertts");
        TTS_ENGINES.put("RHVoice", "https://play.google.com/store/apps/details?id=com.github.olga_yakovleva.rhvoice.android");

    }

    public boolean allowOtherMusic = false;
    public boolean isSystemThemeColor = false;
    public String allOPDSLinks = OPDS_DEFAULT;
    public boolean opdsLargeCovers = true;
    public boolean createBookNameFolder = false;
    public String readColors = READ_COLORS_DEAFAUL;
    // public static String DEFAULTS_TABS_ORDER =
    // "0#1,1#1,2#1,3#1,4#1,5#1,6#0,7#1";BETA
    public String tabsOrder7 = DEFAULTS_TABS_ORDER;
    public int tintColor = Color.parseColor(STYLE_COLORS.get(0));
    public boolean isUiTextColor = false;
    public int uiTextColor = Color.BLUE;
    public int uiTextColorUser = Color.MAGENTA;
    public int statusBarColorDay = Color.parseColor(TEXT_COLOR_DAY);
    public int statusBarColorNight = Color.parseColor(TEXT_COLOR_NIGHT);
    @IgnoreHashCode
    public String statusBarColorDays = "#5B5B5B, #000000";
    @IgnoreHashCode
    public String statusBarColorNights = "#767676, #e2e2e2";
    // public int tintColor =
    // Color.parseColor(STYLE_COLORS.get(STYLE_COLORS.size() - 2));
    public int userColor = Color.MAGENTA;
    public int helpHash = 0;
    @IgnoreHashCode
    public int doubleClickAction1 = DOUBLE_CLICK_ADJUST_PAGE;
    @IgnoreHashCode
    public int inactivityTime = 5;
    @IgnoreHashCode
    public int remindRestTime = -1;
    public int flippingInterval = 10;
    public int ttsTimer = 240;
    public int ttsPauseDuration = 50;
    public int transparencyUI = 250;
    @IgnoreHashCode
    public int pageNumberFormat = PAGE_NUMBER_FORMAT_NUMBER;
    @IgnoreHashCode
    public int chapterFormat = CHAPTER_FORMAT_3;
    public int outlineMode = OUTLINE_ONLY_HEADERS;
    @IgnoreHashCode
    public boolean isAllowTextSelection = true;
    //public boolean isFullScreen = true;
    //public boolean isFullScreenMain = false;
    public boolean isAccurateFontSize = false;
    public boolean isShowFooterNotesInText = false;
    public boolean isCharacterEncoding = false;
    public String characterEncoding = "UTF-8";
    @IgnoreHashCode
    public boolean isEditMode = true;
    public int fullScreenMode = FULL_SCREEN_NORMAL;
    public int fullScreenMainMode = FULL_SCREEN_NORMAL;
    public boolean isShowImages = true;
    public boolean isShowToolBar = true;
    public boolean isConvertToMp3 = true;
    public boolean isShowPanelBookNameScrollMode = true;
    public boolean isShowPanelBookNameBookMode = false;
    public boolean isShowReadingProgress = true;
    public boolean isShowChaptersOnProgress = true;
    public boolean isShowSubChaptersOnProgress = true;
    public int antiAliasLevel = 8;//0-8
    // n,
    // 25 - 25%
    // persent
    public int tabPositionInRecentDialog = 0;
    public boolean tapPositionTop = true;
    public boolean tabWithNames = true;
    public long fontExtractTime = 0;
    public int nextScreenScrollBy = NEXT_SCREEN_SCROLL_BY_PAGES;// 0 by
    public int nextScreenScrollMyValue = 15;
    public int statusBarPosition = STATUSBAR_POSITION_BOTTOM;
    public boolean isReplaceWhite = false;
    public int appTheme = THEME_LIGHT;
    public boolean isOpenLastBook = false;
    // sort by
    public boolean isSortAsc = false;
    public int sortBy = AppDB.SORT_BY.DATA.ordinal();
    public int sortByBrowse = BR_SORT_BY_PATH;
    public boolean sortByReverse = false;
    public int sortByFavorite = BR_SORT_BY_DATE;
    public boolean sortByFavoriteReverse = false;
    @IgnoreHashCode
    public boolean isBrighrnessEnable = false;
    @IgnoreHashCode
    public boolean isAllowMinBrigthness = false;
    public boolean isShowRateUsOnExit = true;
    @IgnoreHashCode
    public boolean isRewindEnable = true;
    @IgnoreHashCode
    public boolean isShowTime = true;
    @IgnoreHashCode
    public boolean isShowBattery = true;
    public int contrastImage = 0;
    public int brigtnessImage = 0;
    public boolean bolderTextOnImage = false;
    public boolean isEnableBC = false;
    @IgnoreHashCode
    public boolean stopReadingOnCall = true;
    @IgnoreHashCode
    public int appBrightness = AUTO_BRIGTNESS;
    @IgnoreHashCode
    public int appBrightnessNight = AUTO_BRIGTNESS;
    public volatile int fastReadSpeed = 200;
    public volatile int fastReadFontSize = 32;
    public volatile int fastManyWords = 2;
    public volatile float ttsSpeed = 1.0f;
    public volatile float ttsPitch = 1.0f;
    @IgnoreHashCode
    public boolean ttsReadBySentences = true;
    @IgnoreHashCode
    public String ttsSentecesDivs = TTS_PUNCUATIONS;
    @IgnoreHashCode
    public boolean ttsTunnOnLastWord = false;
    @IgnoreHashCode
    public boolean isEnalbeTTSReplacements = true;
    public boolean isReferenceMode = false;
    public boolean isShowPageNumbers = false;
    public boolean isEnableAccessibility = false;
    @IgnoreHashCode
    @Deprecated
    public String lineTTSReplacements;
    @IgnoreHashCode
    public String lineTTSReplacements3 = TTS_REPLACEMENTS;
    public List<Integer> nextKeys = NEXT_KEYS;
    public List<Integer> prevKeys = PREV_KEYS;
    @IgnoreHashCode
    public boolean isUseVolumeKeys = true;
    @IgnoreHashCode
    public boolean isFastBookmarkByTTS = false;
    @IgnoreHashCode
    public boolean isReverseKeys = Dips.isSmallScreen();
    @IgnoreHashCode
    public boolean isVisibleSorting = true;
    @IgnoreHashCode
    public boolean isShowBookmarsPanelInMusicMode = true;
    @IgnoreHashCode
    public boolean isShowBookmarsPanelInScrollMode = false;
    @IgnoreHashCode
    public boolean isShowBookmarsPanelInBookMode = false;
    @IgnoreHashCode
    public boolean isShowRectangularTapZones = true;
    @IgnoreHashCode
    public boolean isShowLastPageRed = true;
    @IgnoreHashCode
    public boolean isShowLineDividing = true;
    @IgnoreHashCode
    public boolean isShowBookmarsPanelText = true;
    public String musicText = "Musician";
    public int cropTop = 0;
    public int cropBottom = 0;
    public int cropLeft = 0;
    public int cropRigth = 0;
    public boolean isDayNotInvert = true;
    public boolean isShowSearchBar = true;
    public boolean isShowFastScroll = true;
    public boolean isUseBGImageDay = false;
    public boolean isUseBGImageNight = false;
    public String bgImageDayPath = MagicHelper.IMAGE_BG_1;
    public String bgImageNightPath = MagicHelper.IMAGE_BG_1;
    public int bgImageDayTransparency = DAY_TRANSPARENCY;
    public int bgImageNightTransparency = NIGHT_TRANSPARENCY;
    public String appLang = AppState.MY_SYSTEM_LANG;
    public boolean isPrefFormatMode = false;
    public String prefScrollMode = PREF_SCROLL_MODE;
    public String prefBookMode = PREF_BOOK_MODE;
    public String prefMusicianMode = PREF_MUSIC_MODE;
    @IgnoreHashCode
    public volatile boolean isLoopAutoplay = false;
    public boolean isBookCoverEffect = false;
    public int editWith = EDIT_NONE;
    public String annotationDrawColor = "";
    public String annotationTextColor = COLORS.get(2);
    public int editAlphaColor = 100;
    public float editLineWidth = 3;
    @IgnoreHashCode
    public boolean isRememberMode = false;
    public volatile boolean isAutoScroll = false;
    public int autoScrollSpeed = 120;
    @IgnoreHashCode
    public boolean isScrollSpeedByVolumeKeys = false;
    @IgnoreHashCode
    public int mouseWheelSpeed = 70;
    @IgnoreHashCode
    public String selectedText;
    // public int widgetHeigth = 100;
    public int widgetType = WIDGET_LIST;
    public int widgetItemsCount = 4;
    @IgnoreHashCode
    public String rememberDict1 = "web:Google Translate";
    @IgnoreHashCode
    public int rememberDict1Hash = 0;
    @IgnoreHashCode
    public int rememberDictHash2 = 0;
    @IgnoreHashCode
    public boolean isRememberDictionary;
    public String fromLang = "en";
    public String toLang = Urls.getLangCode();
    @IgnoreHashCode
    public int orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR;
    public int libraryMode = MODE_GRID;
    public int broseMode = MODE_LIST;
    public int recentMode = MODE_LIST;
    public int cloudMode = MODE_LIST_COMPACT;
    public int bookmarksMode = BOOKMARK_MODE_BY_DATE;
    public int starsMode = MODE_LIST_COMPACT;
    public boolean isBrowseGrid = false;
    public boolean isShowCloudsLine = true;
    public String fileToDelete;
    public int mp3seek = 0;
    public int colorDayText = COLOR_BLACK;
    public int colorDayBg = COLOR_WHITE;
    public int colorNigthText = COLOR_WHITE_2;
    public int colorNigthBg = COLOR_BLACK_2;
    public boolean supportPDF = true;
    public boolean supportXPS = false;
    public boolean supportDJVU = true;
    public boolean supportEPUB = true;
    public boolean supportFB2 = true;
    public boolean supportRTF = true;
    public boolean supportODT = true;
    public boolean supportDOCX = true;
    public boolean supportMOBI = true;
    public boolean supportCBZ = false;
    public boolean supportZIP = true;
    public boolean supportArch = false;
    public boolean supportOther = false;
    public boolean supportTXT = false;
    public boolean isPreText = false;
    public boolean isLineBreaksText = false;
    public boolean isIgnoreAnnotatations = false;
    @IgnoreHashCode
    public boolean isSaveAnnotatationsAutomatically = false;
    public boolean isShowWhatIsNewDialog = true;
    public boolean isShowCloseAppDialog = true;
    public boolean isFirstSurname = false;
    public boolean isAuthorTitleFromMetaPDF = false;
    public boolean isShowOnlyOriginalFileNames = false;
    public boolean isSkipFolderWithNOMEDIA = true;
    public boolean isOLED = false;
    public int cutP = 50;
    public volatile int statusBarTextSizeAdv = Dips.isXLargeScreen() ? 16 : 14;
    public volatile int statusBarTextSizeEasy = Dips.isXLargeScreen() ? 16 : 12;
    public volatile int progressLineHeight = Dips.isXLargeScreen() ? 8 : 4;
    public volatile int statusBarTextAlpha = 128;
    public volatile int statusBarBackgroundAlpha = 128;
    public String versionNew = "";
    public boolean isRTL = Urls.isRtl();
    public boolean isCutRTL = Urls.isRtl();
    // perofrmance
    public int pagesInMemory = 3;
    public float pageQuality = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 1.6f : 1.4f;
    public int rotate = 0;
    public int rotateViewPager = 0;
    @IgnoreHashCode
    public int tapzoneSize = Dips.isXLargeScreen() ? 15 : 25;
    public transient int allocatedMemorySize = (int) MemoryUtils.RECOMENDED_MEMORY_SIZE;
    @IgnoreHashCode
    public boolean isScrollAnimation = true;
    @IgnoreHashCode
    public boolean isEnableVerticalSwipe = true;
    @IgnoreHashCode
    public boolean isEnableHorizontalSwipe = true;
    @IgnoreHashCode
    public boolean isSwipeGestureReverse = false;
    public boolean isCustomizeBgAndColors = false;
    @IgnoreHashCode
    public boolean isVibration = true;
    public boolean isExperimental = false;
    @IgnoreHashCode
    public boolean isLockPDF = false;
    @IgnoreHashCode
    public boolean isCropPDF = false;
    public boolean selectingByLetters = Arrays.asList("ja", "zh", "ko", "vi").contains(Urls.getLangCode());
    public long installationDate = System.currentTimeMillis();
    @IgnoreHashCode
    public boolean isShowLongBackDialog = true;
    @IgnoreHashCode
    public boolean isZoomInOutWithVolueKeys = false;
    @IgnoreHashCode
    public boolean isZoomInOutWithLock = true;
    public String customConfigColors = "";
    public boolean isStarsInWidget = false;
    public boolean isShowFastBookmarks = true;
    public boolean isShowOnlyAvailabeBooks = false;
    public boolean isCropBookCovers = true;
    public boolean isBorderAndShadow = true;
    public int coverBigSize = (int) (((Dips.screenWidthDP() / (Dips.screenWidthDP() / 120)) - 8) * (Dips.isXLargeScreen() ? 1.5f : 1));
    public int coverSmallSize = 80;
    @IgnoreHashCode
    public int tapZoneLeftTop = TAP_PREV_PAGE;
    @IgnoreHashCode
    public int tapZoneLeftBottom = TAP_PREV_PAGE;
    @IgnoreHashCode
    public int tapZoneRightTop = TAP_NEXT_PAGE;
    @IgnoreHashCode
    public int tapZoneRightBottom = TAP_NEXT_PAGE;
    @IgnoreHashCode
    public int tapZoneTop = TAP_PREV_PAGE;
    @IgnoreHashCode
    public int tapZoneBottom = TAP_NEXT_PAGE;
    @IgnoreHashCode
    public int tapZoneLeft = TAP_PREV_PAGE;
    @IgnoreHashCode
    public int tapZoneRight = TAP_NEXT_PAGE;
    @IgnoreHashCode
    public int blueLightColor = BLUE_FILTER_DEFAULT_COLOR;
    @IgnoreHashCode
    public int blueLightAlpha = 30;
    @IgnoreHashCode
    public int blueLightAlphaNight = 30;
    @IgnoreHashCode
    public boolean isEnableBlueFilter = false;
    @IgnoreHashCode
    public boolean isEnableBlueFilterNight = false;
    public boolean proxyEnable = false;
    public String proxyServer = "";
    public int proxyPort = 0;
    public String proxyUser = "";
    public String proxyPassword = "";
    public String proxyType = PROXY_HTTP;
    public String nameVerticalMode = "";
    public String nameHorizontalMode = "";
    public String nameMusicianMode = "";
    public boolean isDisplayAllFilesInFolder = false;
    public boolean isAlwaysOpenOnPage1 = false;
    public boolean isHideReadBook = false;
    public boolean isFolderPreview = false;
    public String myAutoCompleteDb = "";
    public String bookTags = "";
    public String recentTag = "";
    public boolean isRestoreSearchQuery = false;
    public String searchQuery = "";
    @IgnoreHashCode
    public int hashCode = 0;
    @IgnoreHashCode
    public boolean isSelectTexByTouch = false;
    public boolean isAppPassword;
    public boolean isLoaded = false;
    public boolean isUseCalibreOpf = true;
    public boolean isDisplayAnnotation = false;
    public boolean isMirrorImage = false;
    public boolean isBionicMode = false;
    public boolean alwaysTwoPages = false;
    public boolean isDefaultHyphenLanguage = false;
    public String defaultHyphenLanguageCode = "en";
    public boolean isMenuIntegration = false;
    public boolean isShowFavTags = true;
    public boolean isShowFavPlaylist = true;
    public boolean isShowFavFolders = true;
    public boolean isShowFavBooks = true;
    public boolean isShowSyncBooks = true;
    public boolean isShowTestBooks = false;
    public boolean isShowDiscardedBooks = true;
    @IgnoreHashCode
    public int sortBookmarksOrder = BOOKMARK_SORT_PAGE_ASC;
    public boolean isEnableTextReplacement = false;
    public long textReplacementHash = 0;
    public boolean isShowSeriesNumberInTitle = true;
    public boolean enableImageScale = true;


    public static synchronized AppState get() {
        return instance;
    }

    public String getAppLang(){
        return AppState.get().appLang.equals(AppState.MY_SYSTEM_LANG) ? Urls.getLangCode() : AppState.get().appLang;
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
        final List<Integer> res = new ArrayList<>();

        for (final String value : list.split(",")) {
            if (value != null && !value.trim().equals("")) {
                res.add(new Integer(value.trim()));
            }
        }
        Collections.sort(res);
        return res;
    }

    public List<Integer> getNextKeys() {
        return isReverseKeys ? prevKeys : nextKeys;
    }

    public List<Integer> getPrevKeys() {
        return isReverseKeys ? nextKeys : prevKeys;
    }

    public void defaults(Context a) {
        nameVerticalMode = a.getString(R.string.mode_vertical);
        nameHorizontalMode = a.getString(R.string.mode_horizontally);
        nameMusicianMode = a.getString(R.string.mode_musician);
        musicText = a.getString(R.string.musician);

        appTheme = Dips.isDarkThemeOn() ? AppState.THEME_DARK : AppState.THEME_LIGHT;
        if (Dips.isEInk()) {
            appTheme = AppState.THEME_INK;
            isDayNotInvert = true;
            isEditMode = true;
            isRememberMode = false;
            isReverseKeys = true;
            isScrollAnimation = false;
            tintColor = Color.BLACK;
            bolderTextOnImage = true;
            isEnableBC = true;
            brigtnessImage = -50;
            isZoomInOutWithLock = false;
        }
        if (Apps.isAccessibilityEnable(a)) {
            accessibilityDefaults();

        }


        if (!AppsConfig.LIBRERA_READER.equals(Apps.getPackageName(a)) && !AppsConfig.PRO_LIBRERA_READER.equals(Apps.getPackageName(a))) {
            isShowWhatIsNewDialog = false;
        }
    }

    public void accessibilityDefaults() {
        AppState.get().isEnableAccessibility = true;
        AppState.get().tabWithNames = false;
        AppState.get().tapPositionTop = true;
        BookCSS.get().appFontScale = 1.1f;
        AppState.get().isScrollAnimation = false;
        AppSP.get().isFirstTimeVertical = false;
        AppSP.get().isFirstTimeHorizontal = false;
        AppState.get().tabsOrder7 = AppState.get().tabsOrder7.replace(UITab.PrefFragment.index + "#0", UITab.PrefFragment.index + "#1");

    }

    public boolean loadInit(final Context a) {
        boolean init = isLoaded;

        if (!isLoaded) {
            defaults(a);


            load(a);
            if (AppState.get().isShowPanelBookNameBookMode && AppState.get().statusBarPosition == AppState.STATUSBAR_POSITION_TOP) {
                AppState.get().isShowPanelBookNameBookMode = false;
            }

            try {
                if (TxtUtils.isNotEmpty(AppState.get().lineTTSReplacements)) {
                    LinkedJSONObject o1 = new LinkedJSONObject(AppState.get().lineTTSReplacements);
                    LinkedJSONObject o3 = new LinkedJSONObject(AppState.get().lineTTSReplacements3);
                    Iterator<String> keys = o1.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        if (!key.startsWith("[")) {
                            String value = o1.getString(key);
                            o3.put(key, value);
                            LOG.d("migration", key, value);
                        }

                    }
                    AppState.get().lineTTSReplacements3 = o3.toString();
                    AppState.get().lineTTSReplacements = "";
                }

            } catch (Exception e) {
                LOG.e(e);
            }

            isLoaded = true;
        }

        return init;
    }

    public void load(final Context a) {
        if (a == null) {
            return;
        }

        IO.readObj(AppProfile.syncState, instance);

    }

    public void save(final Context a) {
        if (a == null) {
            return;
        }

        int currentHash = Objects.hashCode(instance, false);
        if (currentHash != instance.hashCode) {
            hashCode = currentHash;
            IO.writeObj(AppProfile.syncState, instance);
        }
    }


}
