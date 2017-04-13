package com.foobnix.pdf.info.presentation;

import java.io.File;
import java.util.List;

import com.foobnix.pdf.info.AppSharedPreferences;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.widget.FileInformationDialog;
import com.foobnix.pdf.info.wrapper.AppState;

import android.app.Activity;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

public class RecentBooksView {

    public static void init(final Activity a, final LinearLayout starList, final boolean isSubmenu) {
        if (starList == null) {
            return;
        }
        starList.removeAllViews();
        List<Uri> stars = AppSharedPreferences.get().getStars();
        if (stars == null || stars.isEmpty()) {
            ((ViewGroup) starList.getParent()).setVisibility(View.GONE);
            return;
        }
        ((ViewGroup) starList.getParent()).setVisibility(View.VISIBLE);

        for (final Uri uri : stars) {
            final String path = uri.getPath();
            View layout = LayoutInflater.from(starList.getContext()).inflate(R.layout.item_star, null, false);
            final ImageView img = (ImageView) layout.findViewById(R.id.browserItemIcon);
            final ImageView starIcon = (ImageView) layout.findViewById(R.id.starIcon);

            IMG.getCoverPage(img, path, IMG.getImageSize());
            IMG.updateImageSizeSmall(img);

            if (AppState.get().isCropBookCovers) {
                img.setScaleType(ScaleType.CENTER_CROP);
            } else {
                img.setScaleType(ScaleType.FIT_CENTER);
            }

            starList.addView(layout);

            TintUtil.drawStar(starIcon, true);

            starIcon.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    TintUtil.drawStar(starIcon, AppSharedPreferences.get().changeIsStar(path));
                }
            });

            layout.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    File file = new File(path);
                    ExtUtils.openFile(a, file);
                }
            });

            layout.setOnLongClickListener(new OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    File file = new File(path);
                    FileInformationDialog.showFileInfoDialog(a, file, null);
                    return true;
                }
            });

        }

    }
}
