package com.foobnix.sys;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.os.Process;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppProfile;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.PageUrl;

/**
 * Draws the covers of PDF, DjVu and TIFF books in a process of its own (":covers"), see
 * {@link CoverProcess}. A page can take minutes to draw - a PDF whose first page is one form of
 * hundreds of thousands of pattern tiles took over two minutes on a Mac in both engines - and
 * neither MuPDF's nor PDFium's drawing of one form can be stopped from Java. In the app's own
 * process that page held the engine lock all that time: no other cover was drawn and no book
 * opened. Here it holds only this process, which is killed when it is late.
 */
public class CoverRenderService extends Service {

    /** The app's set-up for this process: enough to open a book, and nothing that writes. */
    public static void initProcess(final Context c) {
        Dips.init(c);
        CacheZipUtils.init(c);
        CacheZipUtils.createAllCacheDirs();
        AppProfile.initToRead(c);
        TempHolder.get().loadingCancelled.set(false);
        LOG.d("CoverRenderService process", Process.myPid());
    }

    private final ICoverRenderer.Stub binder = new ICoverRenderer.Stub() {
        @Override
        public int pid() {
            return Process.myPid();
        }

        @Override
        public Bitmap render(final String pageUrl, final int pdfEngine) {
            // one at a time: the engines are not to be used from two threads
            synchronized (CoverRenderService.class) {
                try {
                    // the engine the app reads with now, which may have changed since this
                    // process read the settings
                    AppState.get().pdfEngine = pdfEngine;
                    // memory trimmed in this process says nothing of a cover being drawn
                    TempHolder.get().loadingCancelled.set(false);
                    LOG.d("CoverRenderService render", pageUrl);
                    return ImageExtractor.getInstance(getApplicationContext())
                                         .proccessOtherPage(PageUrl.fromString(pageUrl));
                } catch (Throwable e) {
                    LOG.e(e);
                    return null;
                }
            }
        }
    };

    @Override
    public IBinder onBind(final Intent intent) {
        return binder;
    }
}
