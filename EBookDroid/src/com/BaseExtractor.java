package com;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.ext.EbookMeta;
import com.foobnix.pdf.info.IMG;
import com.foobnix.pdf.info.TintUtil;
import com.foobnix.pdf.info.wrapper.AppState;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;

public abstract class BaseExtractor {

    public abstract EbookMeta getBookMetaInformation(String path);

    public abstract byte[] getBookCover(String path);

    public abstract Map<String, String> getFooterNotes(String path);

    public abstract boolean convert(String path, String to);

    public abstract String getBookOverview(String path);

    public static Bitmap getBookCoverWithTitleBitmap(String title, String author) {

        if (TxtUtils.isEmpty(author)) {
            author = "";
        }
        if (TxtUtils.isEmpty(title)) {
            title = "";
        }

        title = TxtUtils.ellipsize(title, 20);
        author = TxtUtils.ellipsize(author, 40);

        int w = Dips.dpToPx(AppState.get().coverBigSize - 8);
        int h = (int) (w * (IMG.WIDTH_DK));

        TextPaint pNormal = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        pNormal.setColor(Color.WHITE);
        pNormal.setTextSize(h / 11);

        TextPaint pBold = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
        pBold.setColor(Color.WHITE);
        pBold.setTextSize(h / 14);
        pBold.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        c.save();
        c.drawColor(TintUtil.randomColor((title + author).hashCode()));

        int margin = Dips.dpToPx(10);
        StaticLayout mTextLayout = new StaticLayout(title, pBold, c.getWidth() - margin * 2, Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
        c.translate(margin, Dips.dpToPx(20));
        mTextLayout.draw(c);

        StaticLayout text2 = new StaticLayout(author, pNormal, c.getWidth() - margin * 2, Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
        c.translate(0, mTextLayout.getHeight() + (margin));
        text2.draw(c);
        return bitmap;

    }

    public static byte[] getBookCoverWithTitle(String title) {
        try {

            Bitmap bitmap = getBookCoverWithTitleBitmap(title, "");

            byte[] byteArray = bitmapToByteArray(bitmap);

            return byteArray;
        } catch (OutOfMemoryError e) {
            LOG.e(e);
            return null;
        }

    }

    public static Bitmap getBookCoverWithTitle(String title, String author, boolean withLogo) {
        try {
            Bitmap bookCoverWithTitleBitmap = getBookCoverWithTitleBitmap(title, author);
            if (withLogo) {
                MagicHelper.applyBookEffectWithLogo(bookCoverWithTitleBitmap);
            }
            return bookCoverWithTitleBitmap;
        } catch (OutOfMemoryError e) {
            LOG.e(e);
            return null;
        }
    }

    public static Bitmap arrayToBitmap(byte[] array, int width) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(array, 0, array.length, options);
            int imageWidth = options.outWidth;
            options.inSampleSize = imageWidth / width;
            // options.inSampleSize = 1;
            options.inJustDecodeBounds = false;
            LOG.d("inSampleSize", options.inSampleSize);
            return BitmapFactory.decodeByteArray(array, 0, array.length, options);

        } catch (Exception e) {
            return null;
        }
    }

    public static InputStream decodeImage(String path, int width) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);
            int imageWidth = options.outWidth;
            options.inSampleSize = imageWidth / width;
            // options.inSampleSize = 1;
            options.inJustDecodeBounds = false;
            LOG.d("inSampleSize", options.inSampleSize);
            Bitmap decodeFile = BitmapFactory.decodeFile(path, options);

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            decodeFile.compress(Bitmap.CompressFormat.PNG, 95, stream);

            return new ByteArrayInputStream(stream.toByteArray());

        } catch (Exception e) {
            return null;
        }
    }


    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 95, stream);
        byte[] byteArray = stream.toByteArray();
        bitmap.recycle();
        bitmap = null;
        try {
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return byteArray;
    }

    public static final int BUFFER_SIZE = 16 * 1024;

    public static byte[] getEntryAsByte(InputStream zipInputStream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipInputStream.read(bytesIn)) != -1) {
            out.write(bytesIn, 0, read);
        }
        out.close();
        return out.toByteArray();
    }
}
