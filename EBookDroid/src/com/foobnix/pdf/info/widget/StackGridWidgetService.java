package com.foobnix.pdf.info.widget;

import java.io.File;
import java.util.List;

import com.foobnix.dao2.FileMeta;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.sys.ImageExtractor;
import com.foobnix.ui2.AppDB;
import com.nostra13.universalimageloader.core.ImageLoader;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class StackGridWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StackRemoteViewsFactory(getApplicationContext(), intent);
    }

}

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context context;
    private List<FileMeta> recent;

    public StackRemoteViewsFactory(Context context, Intent intent) {
        this.context = context;
    }

    @Override
    public void onCreate() {
        AppState.get().load(context);
        onDataSetChanged();
    }

    @Override
    public void onDataSetChanged() {
        if (AppState.get().isStarsInWidget) {
            recent = AppDB.get().getStarsFiles();
        } else {
            recent = AppDB.get().getRecent();
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

        String url = IMG.toUrl(uri.getPath(), ImageExtractor.COVER_PAGE_WITH_EFFECT, IMG.getImageSize());
        Bitmap image = ImageLoader.getInstance().loadImageSync(url, IMG.displayCacheMemoryDisc);
        v.setImageViewBitmap(R.id.imageView1, image);
        v.setContentDescription(R.id.imageView1, new File(uri.getPath()).getName());

        Bundle extras = new Bundle();
        extras.putInt("pos", position);
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
