package org.ebookdroid.droids.mupdf.codec;

import android.graphics.Bitmap;

import com.foobnix.pdf.info.AppsConfig;

import org.ebookdroid.core.codec.AbstractCodecContext;

public abstract class MuPdfContext extends AbstractCodecContext {

    public static final Bitmap.Config NATIVE_BITMAP_CFG = AppsConfig.CURRENT_BITMAP_ARGB;

    public MuPdfContext() {
    }

    @Override
    public Bitmap.Config getBitmapConfig() {
    return NATIVE_BITMAP_CFG;
    }


    @Override
    public boolean isParallelPageAccessAvailable() {
        return false;
    }



}
