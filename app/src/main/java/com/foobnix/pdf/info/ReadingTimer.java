package com.foobnix.pdf.info;

import android.content.Context;
import android.os.SystemClock;

public class ReadingTimer {

    private static ReadingTimer instance;
    private Context context;
    private long startTime;
    private long totalTime;

    private ReadingTimer(Context context) {
        this.context = context;
        this.totalTime = 0;
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new ReadingTimer(context);
        }
    }

    public static synchronized ReadingTimer getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ReadingTimer is not initialized, call init() method first.");
        }
        return instance;
    }

    public void start() {
        startTime = SystemClock.elapsedRealtime();
    }

    public void stop() {
        long endTime = SystemClock.elapsedRealtime();
        totalTime += (endTime - startTime);
    }

    public long getTotalTime() {
        return totalTime;
    }
}
