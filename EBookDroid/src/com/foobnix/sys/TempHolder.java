package com.foobnix.sys;

import java.util.concurrent.locks.ReentrantLock;

import org.ebookdroid.BookType;

import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.wrapper.UITab;

public class TempHolder {
    public static final ReentrantLock lock = new ReentrantLock();
    public static TempHolder inst = new TempHolder();

    public volatile String path;
    public boolean isTextFormat;
    public boolean isTextForamtButNotTxt;

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

    public static TempHolder get() {
        return inst;
    }

    public void init(String pathI) {
        path = pathI;
        isTextFormat = isTextForamtInner();
        isTextForamtButNotTxt = isTextForamtButNotTxt();
    }

    public void clear() {
        path = null;
    }

    private boolean isTextForamtInner() {
        try {
            return ExtUtils.isTextFomat(path);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTextForamtButNotTxt() {
        try {
            return ExtUtils.isTextFomat(path) && !BookType.TXT.is(path);
        } catch (Exception e) {
            return false;
        }
    }

}
