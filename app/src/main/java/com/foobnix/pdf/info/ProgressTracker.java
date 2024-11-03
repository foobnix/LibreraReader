package com.foobnix.pdf.info;

import android.content.Context;

public class ProgressTracker {

    private static ProgressTracker instance;
    private Context context;

    private ProgressTracker(Context context) {
        this.context = context;
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new ProgressTracker(context);
        }
    }

    public static synchronized ProgressTracker getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ProgressTracker is not initialized, call init() method first.");
        }
        return instance;
    }

    public void updateProgress(String bookId, int progress) {
        Prefs.get().putProgress(bookId, progress);
    }

    public int getProgress(String bookId) {
        return Prefs.get().getProgress(bookId);
    }
}
