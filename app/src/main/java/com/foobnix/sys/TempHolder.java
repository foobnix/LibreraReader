package com.foobnix.sys;

import com.foobnix.pdf.info.wrapper.UITab;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

public class TempHolder {
    public static final ReentrantLock lock = new ReentrantLock();

    public static TempHolder inst = new TempHolder();
    public static int listHash = 0;
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

    public volatile boolean loadingCancelled = false;
    public boolean forseAppLang = false;

    public volatile long lastRecycledDocument = 0;

    public int textFromPage = 0;
    public String copyFromPath = null;

    public int documentTitleBarHeight;

    public static TempHolder get() {
        return inst;
    }


}
