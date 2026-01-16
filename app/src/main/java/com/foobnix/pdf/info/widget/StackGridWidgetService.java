package com.foobnix.pdf.info.widget;

import static android.util.TypedValue.COMPLEX_UNIT_PX;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import androidx.core.util.TypedValueCompat;

import com.bumptech.glide.Glide;
import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.dao2.FileMeta;
import com.foobnix.model.AppData;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.ui2.AppDB;

import com.foobnix.LibreraApp;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class StackGridWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StackRemoteViewsFactory(getApplicationContext(), intent);
    }

}

class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context context;
    private List<FileMeta> recent;

    public StackRemoteViewsFactory(Context context, Intent intent) {
        this.context = context;
    }

    @Override
    public void onCreate() {
        AppProfile.init(context);
        onDataSetChanged();
    }

    @Override
    public void onDataSetChanged() {
        if (AppState.get().isStarsInWidget) {
            recent = AppData.get().getAllFavoriteFiles(false);
        } else {
            recent = AppData.get().getAllRecent(false);
        }
        AppDB.removeClouds(recent);
    }

    @Override
    public void onDestroy() {

    }

    @Override
    public int getCount() {
        return recent.size() > AppState.get().widgetItemsCount ? AppState.get().widgetItemsCount : recent.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget_grid_image);

        if (recent.size() <= position) {
            return v;
        }

        FileMeta uri = recent.get(position);

        try {
            Bitmap image =
                    Glide.with(LibreraApp.context).asBitmap().load(IMG.getCoverUrl(uri.getPath())).submit().get();
            v.setImageViewBitmap(R.id.imageView1, image);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                v.setViewLayoutWidth(R.id.imageView1, IMG.getImageSize(), COMPLEX_UNIT_PX);
            }
            v.setViewPadding(R.id.imageView1, Dips.DP_2,Dips.DP_2,Dips.DP_2,Dips.DP_2);
        } catch (Exception e) {
            LOG.e(e);
        }


        Bundle extras = new Bundle();
        extras.putParcelable("uri", Uri.fromFile(new File(uri.getPath())));
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);

        v.setOnClickFillInIntent(R.id.imageView1, fillInIntent);

        return v;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

}
