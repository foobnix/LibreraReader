package org.ebookdroid.common.settings;

import org.ebookdroid.common.settings.types.ToastPosition;

import com.foobnix.android.utils.Dips;

public class AppSettings {

    public static final String BACKUP_KEY = "app-settings";

    private static AppSettings current;

    /* =============== UI settings =============== */

    public final boolean brightnessInNightModeOnly;



    public boolean fullScreen;

    public final boolean showTitle;

    public final boolean pageInTitle;

    public final ToastPosition pageNumberToastPosition;

    public final ToastPosition zoomToastPosition;

    public final boolean showAnimIcon;

    public final int linkHighlightColor;

    public final int searchHighlightColor;

    public final int currentSearchHighlightColor;

    public final boolean storeGotoHistory;

    public final boolean storeLinkGotoHistory;

    public final boolean storeOutlineGotoHistory;

    public final boolean storeSearchGotoHistory;

    /* =============== Tap & Scroll settings =============== */

    public final boolean tapsEnabled;

    public final int scrollHeight;

    public final int touchProcessingDelay;

    public final boolean animateScrolling;


    /* =============== Performance settings =============== */

    public final int pagesInMemory;



    public final int decodingThreadPriority;

    public final int drawThreadPriority;

    public final boolean hwaEnabled;

    public final int bitmapSize;

    public final boolean bitmapFileringEnabled;

    public final boolean textureReuseEnabled;

    public final boolean useBitmapHack;

    public final boolean useEarlyRecycling;

    public final boolean reloadDuringZoom;

    public final int heapPreallocate;

    public final int pdfStorageSize;


    /* =============== DjVU Format-specific settings =============== */

    public final int djvuRenderingMode;

    /* =============== Backup settings =============== */

    /* =============================================== */

    private AppSettings() {
        /* =============== UI settings =============== */
        brightnessInNightModeOnly = false;
        fullScreen = false;
        showTitle = true;
        pageInTitle = true;
        pageNumberToastPosition = ToastPosition.Bottom;
        zoomToastPosition = ToastPosition.Bottom;
        showAnimIcon = false;
        linkHighlightColor = 0x80FFFF00;
        searchHighlightColor = 0x3F0000FF;
        currentSearchHighlightColor = 0x7F007F00;
        storeGotoHistory = false;
        storeLinkGotoHistory = false;
        storeOutlineGotoHistory = false;
        storeSearchGotoHistory = false;

        /* =============== Tap & Scroll settings =============== */
        tapsEnabled = true;
        scrollHeight = Dips.dpToPx(10);
        touchProcessingDelay = 50;
        animateScrolling = true;
        /* =============== Performance settings =============== */

		pagesInMemory = 2;
        decodingThreadPriority = Thread.NORM_PRIORITY; // 7
        drawThreadPriority = Thread.NORM_PRIORITY; // 6

        hwaEnabled = false;
        bitmapSize = 9;// 6-64,7-128,8-256,9-512,10-1024
        bitmapFileringEnabled = false;
        textureReuseEnabled = true;
        useBitmapHack = false;
        useEarlyRecycling = false;
        reloadDuringZoom = false;
        heapPreallocate = 0;// 0-256
        pdfStorageSize = 64;// 16-128

        /* =============== DjVU Format-specific settings =============== */
        djvuRenderingMode = 0;// 0-color,1-black,2 color only, 3 mask, 4 backgroud, 5 foreground
    }


    /* =============== */


    public static AppSettings getInstance() {
        if (current == null) {
            current = new AppSettings();
        }
        return current;
    }

}
