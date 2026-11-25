package com.foobnix.sys;

import com.foobnix.pdf.info.wrapper.UITab;

import android.os.Looper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class TempHolder {
    public static final ReentrantLock lock = new ReentrantLock();

    private static final long HINT_UI_LOCK_MS = 200;

    /**
     * The document lock for what is only a hint - the text of a page a place is saved or found
     * by. Off the main thread it is waited for; on it, only briefly: a heavy page being drawn
     * holds the lock for seconds, and the reader waiting for it froze ("not responding", PDFium
     * in PDF Reader 10.1.7). False when it was not taken: go without the hint, do not unlock.
     */
    public static boolean lockForHint() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            lock.lock();
            return true;
        }
        try {
            return lock.tryLock(HINT_UI_LOCK_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    public static final TempHolder inst = new TempHolder();
    public static volatile int listHash = 0;

    public static volatile int listHashTemp;
    public static String loadingTime;

    public static boolean isListHashChange() {
        if (listHash == listHashTemp) {
            return false;
        }
        listHashTemp = listHash;
        return true;
    }

    public static volatile boolean isSeaching = false;
    public static volatile boolean isConverting = false;
    public static volatile boolean isRecordTTS = false;

    public static int isRecordFrom = 1;
    public static int isRecordTo = 1;

    public static volatile AtomicBoolean isActiveSpeedRead = new AtomicBoolean(false);
    public String login = "", password = "";
    public int linkPage = -1;
    public int currentTab = UITab.SearchFragment.index;
    public long timerFinishTime = 0;

    public int pageDelta = 0;

    //public static volatile boolean loadingCancelled = false;

    public final LoadingCancel loadingCancelled = new LoadingCancel();
    public boolean forseAppLang = false;

    public volatile long lastRecycledDocument = 0;

    public int textFromPage = 0;
    public String copyFromPath = null;

    public int documentTitleBarHeight;



    public static TempHolder get() {
        return inst;
    }


}
