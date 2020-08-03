package com.foobnix.model;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
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
import com.foobnix.pdf.info.widget.DialogTranslateFromTo;
import com.foobnix.pdf.info.wrapper.MagicHelper;
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
    public final static String OPDS_DEFAULT = "" + //
            // "http://flibusta.is/opds,Flibusta,Книжное
            // братство,http://flibusta.is/favicon.ico;" + //

            // "http://opds.litres.ru,Litres,Библиотека электронных
            // книг,assets://opds/litres.ico;" + //
            "https://books.fbreader.org/opds,FBReader,My personal catalogue,assets://opds/fbreader.png;" + //
            // "https://www.gitbook.com/api/opds/catalog.atom,GitBook,Public books are
            // always free.,assets://opds/gitbook.png;" + //
            "http://www.feedbooks.com/publicdomain/catalog.atom,Feedbooks,Free ebooks,assets://opds/feedbooks.ico;" + //
            "http://m.gutenberg.org/ebooks.opds/,Project Gutenberg,Free ebooks since 1971,assets://opds/gutenberg.png;" + //
            // "http://manybooks.net/opds/index.php,Manybooks,Online Catalog for
            // Manybooks.net,assets://opds/manybooks.png;" + //
            "https://www.smashwords.com/atom,Smashwords,Online Catalog,assets://opds/smashwords.png;" + //
            "http://samlib.ru,Журнал Самиздат (samlib.ru),Cовременная литература при библиотеке Мошкова,assets://opds/web.png;" + //
            SamlibOPDS.ROOT_AWARDS + ",Usefull links: The Awards anspand Top Books - Награды и премии, Complete award winners listing,assets://opds/rating.png;" //
            // end
            ;
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
    public static final List<String> langCodes = Arrays.asList(//
            "en", "ar", "cs", "de", "es", "fa", "fi", "fr", "he", //
            "hi", "hu", "id", "it", "ja", "ko", "la", "lt", //
            "nl", "no", "pl", "pt", "ro", "ru", "sk", "sv", //
            "sw", "th", "tr", "uk", "vi", "ga", "bg", "ml", DialogTranslateFromTo.CHINESE_SIMPLE, DialogTranslateFromTo.CHINESE_TRADITIOANAL);
    public static Map<String, String[]> CONVERTERS = new LinkedHashMap<>();
    public static Map<String, String> TTS_ENGINES = new LinkedHashMap<>();
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
        CONVERTERS.put("PDF", "https://cloudconvert.com/anything-to-pdf, http://topdf.com".split(", "));
        CONVERTERS.put("PDF Rotate", "https://www.pdfrotate.com, https://smaltilpdf.com/rotate-pdf, http://www.rotatepdf.net".split(", "));
        CONVERTERS.put("EPUB", "https://cloudconvert.com/anything-to-epub, http://toepub.com".split(", "));
        CONVERTERS.put("MOBI", "https://cloudconvert.com/anything-to-mobi, http://toepub.com".split(", "));
        CONVERTERS.put("AZW3", "https://cloudconvert.com/anything-to-azw3, http://toepub.com".split(", "));
        CONVERTERS.put("DOCX", "https://cloudconvert.com/anything-to-docx, http://document.online-convert.com/convert-to-docx, http://pdf2docx.com/".split(", "));
        CONVERTERS.put("FB2", "https://cloudconvert.com/anything-to-fb2, https://ebook.online-convert.com/convert-to-fb2".split(", "));

    }

    static {
        TTS_ENGINES.put("Google Text-to-Speech", "https://play.google.com/store/apps/details?id=com.google.android.tts");
        TTS_ENGINES.put("Acapela TTS Voices", "https://play.google.com/store/apps/details?id=com.acapelagroup.android.tts");
        TTS_ENGINES.put("Vocalizer TTS Voice", "https://play.google.com/store/apps/details?id=es.codefactory.vocalizertts");
        TTS_ENGINES.put("RHVoice", "https://play.google.com/store/apps/details?id=com.github.olga_yakovleva.rhvoice.android");

    }

    public String myOPDSLinks = OPDS_DEFAULT;
    public boolean opdsLargeCovers = true;
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
    public int remindRestTime = 120;
    public int flippingInterval = 10;
    public int ttsTimer = 60;
    public int ttsPauseDuration = 50;
    public int transparencyUI = 245;
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
    // n,
    // 25 - 25%
    // persent
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
    public int orientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
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
    public int colorNigthText = COLOR_WHITE;
    public int colorNigthBg = COLOR_BLACK;
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
    public boolean isShowLongBackDialog = false;
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
    public String myAutoCompleteDb = "";
    public String bookTags = "";
    public String recentTag = "";
    @IgnoreHashCode
    public int hashCode = 0;
    @IgnoreHashCode
    public boolean isSelectTexByTouch = false;
    public boolean isAppPassword;
    public boolean isLoaded = false;
    public boolean isUseCalibreOpf = true;
    public boolean isDisplayAnnotation = false;
    public boolean isMirrorImage = false;
    public boolean isDefaultHyphenLanguage = false;
    public String defaultHyphenLanguageCode = "en";
    public boolean isMenuIntegration = false;

    public static Map<String, String> getDictionaries(String input) {
        final Map<String, String> providers = new LinkedHashMap<>();
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
        providers.put("Wikipedia", String.format("https://%s.m.wikipedia.org/wiki/%s", from, text));
        providers.put("Wiktionary", String.format("https://%s.m.wiktionary.org/wiki/%s", from, text));
        providers.put("Academic.ru", String.format("https://dic.academic.ru/searchall.php?SWord=%s", text));
        providers.put("Treccani.it", String.format("http://www.treccani.it/vocabolario/ricerca/%s", text));
        providers.put("Deepl.com", String.format("https://www.deepl.com/translator#%s/%s/%s", from, ln, text));
        providers.put("Vocabulary.com", String.format("https://www.vocabulary.com/dictionary/%s", text));
        return providers;
    }

    public static synchronized AppState get() {
        return instance;
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


        if (!AppsConfig.LIBRERA_READER.equals(Apps.getPackageName(a)) && !AppsConfig.PRO_LIBRERA_READER.equals(Apps.getPackageName(a))) {
            isShowWhatIsNewDialog = false;
        }


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
