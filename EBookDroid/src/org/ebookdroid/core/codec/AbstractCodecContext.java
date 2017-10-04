package org.ebookdroid.core.codec;

import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordException;
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordRequiredException;
import org.ebookdroid.ui.viewer.ViewerActivity;

import com.foobnix.android.utils.LOG;
import com.foobnix.android.utils.TxtUtils;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.EbookMeta;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.ui2.FileMetaCore;

import android.graphics.Bitmap;

public abstract class AbstractCodecContext implements CodecContext {

    private static final AtomicLong SEQ = new AtomicLong();

    private static Integer densityDPI;

    private long contextHandle;

    /**
     * Constructor.
     */
    protected AbstractCodecContext() {
        this(SEQ.incrementAndGet());
    }

    public abstract CodecDocument openDocumentInner(String fileName, String password);

    @Override
    public CodecDocument openDocument(String fileNameOriginal, String password) {
        LOG.d("Open Document", fileNameOriginal);
        if (ExtUtils.isZip(fileNameOriginal)) {
            LOG.d("Open Document ZIP", fileNameOriginal);
            return openDocumentInner(fileNameOriginal, password);
        }

        EbookMeta ebookMeta = FileMetaCore.get().getEbookMeta(fileNameOriginal);

        String lang = ebookMeta.getLang();
        if (TxtUtils.isNotEmpty(lang)) {
            BookCSS.get().hypenLang = lang;
        }

        LOG.d("openDocument2 LANG:", lang, fileNameOriginal);

        File cacheFileName = getCacheFileName(fileNameOriginal);
        CacheZipUtils.removeFiles(CacheZipUtils.CACHE_BOOK_DIR.listFiles(), cacheFileName);

        if (cacheFileName != null && cacheFileName.isFile()) {
            LOG.d("Open Document from cache", fileNameOriginal);
            return openDocumentInner(fileNameOriginal, password);
        }

        CacheZipUtils.cacheLock.lock();
        CacheZipUtils.createAllCacheDirs();
        try {
            String fileName = CacheZipUtils.extracIfNeed(fileNameOriginal).unZipPath;
            LOG.d("Open Document extract", fileName);
            if (!ExtUtils.isValidFile(fileName)) {
                return null;
            }
            try {
                return openDocumentInner(fileName, password);
            } catch (MuPdfPasswordException e) {
                throw new MuPdfPasswordRequiredException();
            } catch (Throwable e) {
                LOG.e(e);
                return null;
            }
        } finally {
            CacheZipUtils.cacheLock.unlock();
        }

    }

    public File getCacheFileName(String fileNameOriginal) {
        return null;
    }

    /**
     * Constructor.
     *
     * @param contextHandle
     *            contect handler
     */
    protected AbstractCodecContext(final long contextHandle) {
        this.contextHandle = contextHandle;
    }

    @Override
    protected final void finalize() throws Throwable {
        // recycle();
        super.finalize();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecContext#recycle()
     */
    @Override
    public final void recycle() {
        if (!isRecycled()) {
            freeContext();
            contextHandle = 0;
        }
    }

    protected void freeContext() {
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecContext#isRecycled()
     */
    @Override
    public final boolean isRecycled() {
        return contextHandle == 0;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecContext#getContextHandle()
     */
    @Override
    public final long getContextHandle() {
        return contextHandle;
    }

    @Override
    public boolean isPageSizeCacheable() {
        return true;
    }

    @Override
    public boolean isParallelPageAccessAvailable() {
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.ebookdroid.core.codec.CodecContext#getBitmapConfig()
     */
    @Override
    public Bitmap.Config getBitmapConfig() {
        return Bitmap.Config.RGB_565;
    }

    public static int getSizeInPixels(final float pdfHeight, float dpi) {
        if (dpi == 0) {
            // Archos fix
            dpi = getDensityDPI();
        }
        if (dpi < 72) { // Density lover then 72 is to small
            dpi = 72; // Set default density to 72
        }
        return (int) (pdfHeight * dpi / 72);
    }

    private static int getDensityDPI() {
        if (densityDPI == null) {
            try {
                final Field f = ViewerActivity.DM.getClass().getDeclaredField("densityDpi");
                densityDPI = ((Integer) f.get(ViewerActivity.DM));
            } catch (final Throwable ex) {
                densityDPI = Integer.valueOf(120);
            }
        }
        return densityDPI.intValue();
    }
}
