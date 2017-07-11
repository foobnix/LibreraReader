package com.google.android.gms.analytics;

import java.lang.Thread.UncaughtExceptionHandler;

import android.content.Context;

public class ExceptionReporter implements UncaughtExceptionHandler {

    public ExceptionReporter(Tracker t, UncaughtExceptionHandler u, Context c) {

    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {

    }

    public void setExceptionParser(ExceptionParser parser) {

    }

}
