package org.ebookdroid.common.settings;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;

public class CoreSettings {

    private static CoreSettings current;

    public boolean fullScreen;
    public boolean showAnimIcon;

    /* =============== Tap & Scroll settings =============== */

    public boolean tapsEnabled;
    public boolean animateScrolling;


    /* =============== Performance settings =============== */

    public int pagesInMemory;
    public int decodingThreadPriority;
    public int drawThreadPriority;
    public int bitmapSize;
    public boolean bitmapFileringEnabled;
    public boolean textureReuseEnabled;
    public boolean reloadDuringZoom;

    /* =============== DjVU Format-specific settings =============== */
    public int djvuRenderingMode;


    private CoreSettings() {
        /* =============== UI settings =============== */
        fullScreen = false;
        showAnimIcon = false;

        /* =============== Tap & Scroll settings =============== */
        animateScrolling = true;
        /* =============== Performance settings =============== */

        pagesInMemory = 2;
        decodingThreadPriority = Thread.MAX_PRIORITY; // 7
        drawThreadPriority = Thread.NORM_PRIORITY; // 6

        bitmapSize = Dips.screenMinWH() < 800 ? 8 : 9;// 6-64,7-128,8-256,9-512,10-1024
        bitmapFileringEnabled = false;
        textureReuseEnabled = false;// !!!
        reloadDuringZoom = false;

        /* =============== DjVU Format-specific settings =============== */
        djvuRenderingMode = 0;// 0-color,1-black,2 color only, 3 mask, 4 backgroud, 5 foreground
        LOG.d("bitmapSize", bitmapSize);
    }


    /* =============== */
    public static CoreSettings get() {
        return getInstance();
    }

    public static CoreSettings getInstance() {
        if (current == null) {
            current = new CoreSettings();
        }
        return current;
    }

}
