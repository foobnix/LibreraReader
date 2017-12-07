package com.foobnix.pdf.info;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.foobnix.android.utils.LOG;
import com.google.android.gms.analytics.ExceptionParser;
import com.google.android.gms.analytics.ExceptionReporter;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

public class Analytics {

    private static Tracker myTracker;

    public static void onStart(Activity a) {
        if (a == null || myTracker == null) {
            return;
        }
        try {
            if (a != null && AppsConfig.IS_APP_WITH_ANALITICS) {
                myTracker.setScreenName(a.getClass().getName());
                myTracker.send(new HitBuilders.ScreenViewBuilder().build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void onStop(Activity a) {
        if (a == null || myTracker == null) {
            return;
        }
        try {
            if (a != null && AppsConfig.IS_APP_WITH_ANALITICS) {
                myTracker.setScreenName(a.getClass().getName());
                myTracker.send(new HitBuilders.ScreenViewBuilder().build());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setContext(Context mCtx) {
        if (mCtx == null) {
            return;
        }
        try {
            if (mCtx != null && AppsConfig.IS_APP_WITH_ANALITICS) {
                myTracker = GoogleAnalytics.getInstance(mCtx).newTracker(AppsConfig.ANALYTICS_ID);
                myTracker.enableAdvertisingIdCollection(true);
                ExceptionReporter myHandler = new ExceptionReporter(myTracker, Thread.getDefaultUncaughtExceptionHandler(), mCtx);

                myHandler.setExceptionParser(new ExceptionParser() {

                    @Override
                    public String getDescription(String arg0, Throwable e) {
                        StringWriter sw = new StringWriter();
                        PrintWriter pw = new PrintWriter(sw);
                        e.printStackTrace(pw);
                        String res = sw.toString();
                        return res.length() > 1500 ? res.substring(0, 1500) : res;

                    }
                });
                Thread.setDefaultUncaughtExceptionHandler(myHandler);

            }
        } catch (Throwable e) {
            LOG.e(e);
        }
    }

    public static void sendException(Throwable ex, boolean fatal) {
        if (myTracker == null) {
            return;
        }
        StringWriter stackTrace = new StringWriter();
        ex.printStackTrace(new PrintWriter(stackTrace));
        String full = stackTrace.toString();
        Log.e("TEST", "send full" + full);
        myTracker.send(new HitBuilders.ExceptionBuilder().setDescription(ex.getMessage())//
                .setFatal(true)//
                .build());
    }
}
