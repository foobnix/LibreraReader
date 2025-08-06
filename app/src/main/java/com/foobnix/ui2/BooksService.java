package com.foobnix.ui2;

import android.os.Handler;

public class BooksService {

    public static String TAG = "BooksService";
    public static String INTENT_NAME = "BooksServiceIntent";
    public static String ACTION_SEARCH_ALL = "ACTION_SEARCH_ALL";
    public static String ACTION_REMOVE_DELETED = "ACTION_REMOVE_DELETED";
    public static String ACTION_SYNC_DROPBOX = "ACTION_SYNC_DROPBOX";
    public static String ACTION_RUN_SELF_TEST = "ACTION_RUN_SELF_TEST";
    public static String ACTION_RUN_SYNCRONICATION = "ACTION_RUN_SYNCRONICATION";
    public static String RESULT_SYNC_FINISH = "RESULT_SYNC_FINISH";
    public static String RESULT_SEARCH_FINISH = "RESULT_SEARCH_FINISH";
    public static String RESULT_BUILD_LIBRARY = "RESULT_BUILD_LIBRARY";
    public static String RESULT_SEARCH_COUNT = "RESULT_SEARCH_COUNT";
    public static String RESULT_NOTIFY_ALL = "RESULT_NOTIFY_ALL";

    public static String RESULT_SEARCH_MESSAGE_TXT = "RESULT_SEARCH_MESSAGE_TXT";

    public static volatile boolean isRunning = false;
    Handler handler;
    boolean isStartForeground = false;

}
