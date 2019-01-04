package org.ebookdroid.common.settings;

import com.foobnix.android.utils.Dips;

public class AppSettings {

    private static AppSettings current;

    public boolean fullScreen;
    public boolean showAnimIcon;

    /* =============== Tap & Scroll settings =============== */

    public boolean tapsEnabled;
    public int scrollHeight;
    public int touchProcessingDelay;
    public boolean animateScrolling;


    /* =============== Performance settings =============== */

    public int pagesInMemory;
    public int decodingThreadPriority;
    public int drawThreadPriority;
    public int bitmapSize;
    public boolean bitmapFileringEnabled;
    public boolean textureReuseEnabled;
    public boolean reloadDuringZoom;
    public int pdfStorageSize;

    /* =============== DjVU Format-specific settings =============== */
    public int djvuRenderingMode;


    private AppSettings() {
        /* =============== UI settings =============== */
        fullScreen = false;
        showAnimIcon = false;

        /* =============== Tap & Scroll settings =============== */
        scrollHeight = Dips.dpToPx(10);
        touchProcessingDelay = 50;
        animateScrolling = true;
        /* =============== Performance settings =============== */

		pagesInMemory = 2;
        decodingThreadPriority = Thread.NORM_PRIORITY; // 7
        drawThreadPriority = Thread.NORM_PRIORITY; // 6

        bitmapSize = 9;// 6-64,7-128,8-256,9-512,10-1024
        bitmapFileringEnabled = false;
        textureReuseEnabled = false;// !!!
        reloadDuringZoom = false;
        pdfStorageSize = 64;// 16-128

        /* =============== DjVU Format-specific settings =============== */
        djvuRenderingMode = 0;// 0-color,1-black,2 color only, 3 mask, 4 backgroud, 5 foreground
    }


    /* =============== */
    public static AppSettings get() {
        return getInstance();
    }

    public static AppSettings getInstance() {
        if (current == null) {
            current = new AppSettings();
        }
        return current;
    }

}
