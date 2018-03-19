package com.foobnix.sys;

import java.util.concurrent.locks.ReentrantLock;

import com.foobnix.pdf.info.wrapper.UITab;

public class TempHolder {
    public static final ReentrantLock lock = new ReentrantLock();
    public static TempHolder inst = new TempHolder();

    public String login = "", password = "";

    public int linkPage = -1;
    public int currentTab = UITab.SearchFragment.index;

    public static int listHash = 0;

    public static volatile boolean isSeaching = false;
    public static volatile boolean isConverting = false;
    public static volatile boolean isRecordTTS = false;

    public long timerFinishTime = 0;

    public int pageDelta = 0;

    public volatile boolean loadingCancelled = false;
    public boolean forseAppLang = false;

    public volatile long lastRecycledDocument = 0;

    public boolean isAllowTextSelectionFirstTime = true;
    public int textFromPage = 0;

    public static TempHolder get() {
        return inst;
    }



}
