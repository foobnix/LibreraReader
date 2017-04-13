package com.foobnix.pdf.info.widget;

import com.foobnix.android.utils.LOG;

import android.content.Context;
import android.content.Intent;

public class RecentUpates {

    public static void updateAll(Context c) {
        if (c == null) {
            return;
        }

        LOG.d("RecentUpates", "update widgets");

        Intent intent = new Intent(c, RecentBooksWidget.class);
        intent.setAction("android.appwidget.action.APPWIDGET_UPDATE");
        c.sendBroadcast(intent);

    }

}
