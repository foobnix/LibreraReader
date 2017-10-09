package com.foobnix.pdf.info.view;

import com.foobnix.android.utils.IntegerResponse;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.R;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.widget.LinearLayout;

public class Dialogs {

    public static void showContrastDialogByUrl(final Context c, String url, final Runnable action) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setMessage(R.string.contrast);
        builder.setCancelable(true);

        final ScaledImageView image = new ScaledImageView(c);
        image.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1));

        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);

        final Bitmap bitmap = IMG.getCoverPage(url);
        image.setImageBitmap(AppState.get().contrast == 0 ? bitmap : MagicHelper.createQuickContrast(bitmap, AppState.get().contrast));

        final CustomSeek seek = new CustomSeek(c);
        seek.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0));
        seek.initWith("", "");
        seek.init(0, 200, AppState.get().contrast);
        seek.setOnSeekChanged(new IntegerResponse() {

            @Override
            public boolean onResultRecive(int result) {
                AppState.get().contrast = result;
                if (result == 0) {
                    image.setImageBitmap(bitmap);
                    return false;
                }
                Bitmap contrast = MagicHelper.createQuickContrast(bitmap, result);
                image.setImageBitmap(contrast);
                return false;
            }
        });

        l.addView(image);
        l.addView(seek);

        builder.setView(l);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int id) {
                if (action != null) {
                    action.run();
                }

            }
        });
        builder.show();
    }

}
