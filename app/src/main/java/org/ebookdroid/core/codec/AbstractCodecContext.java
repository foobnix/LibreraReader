package org.ebookdroid.core.codec;

import android.graphics.Bitmap;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.ext.CacheZipUtils.CacheDir;
import com.foobnix.ext.EpubProcessingSettings;
import com.foobnix.model.AppSP;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.BookCacheLeases;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.BookType;
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordException;
import org.ebookdroid.droids.mupdf.codec.exceptions.MuPdfPasswordRequiredException;
import org.ebookdroid.ui.viewer.VerticalViewActivity;

import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

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

    public CodecDocument openDocumentInnerCanceled(String fileName, String password) {
        long t = System.currentTimeMillis();
        CodecDocument openDocument = openDocumentInner(fileName, password);
        LOG.d("openDocumentInner-time", (float) (System.currentTimeMillis() - t) / 1000, fileName);
        LOG.d("removeTempFiles1", TempHolder.get().loadingCancelled.get());
//        if (TempHolder.get().loadingCancelled.get()) {
//            removeTempFiles();
//            return null;
//        }


        return openDocument;
    }

    public void removeTempFilesIfCancel() {
        LOG.d("removeTempFiles2", "remove temp files", TempHolder.get().loadingCancelled.get());
        if (TempHolder.get().loadingCancelled.get()) {
            //recycle();
            try {
                Thread.sleep(1000);
                CacheZipUtils.removeFiles(CacheZipUtils.CACHE_TEMP.listFiles());
            }catch (Exception e){
                LOG.w(e);
            }
        }
    }

    public static String sourceRevisionKey(String path) {
        try {
            File file = new File(path);
            // Cache publishers register immutable sources whose pathname includes
            // their revision. Their mtime is an LRU clock, not an input revision.
            if (BookCacheLeases.isImmutableRevisionNamedSource(file)) return "|immutable-revision-in-path";
            return "|length=" + file.length() + "|modified=" + file.lastModified();
        } catch (Exception e) {
            LOG.e(e);
            return "|unreadable-revision=" + java.util.UUID.randomUUID();
        }
    }

    /** Text converters override this; binary codecs do not capture text-processing state. */
    protected EpubProcessingSettings.Scope captureProcessingSettings(String path) { return null; }

    @Override
    public CodecDocument openDocument(String fileNameOriginal, String password) {
        File source = new File(fileNameOriginal);
        AutoCloseable openingLease = BookCacheLeases.acquire(source);
        try (EpubProcessingSettings.Scope settings = captureProcessingSettings(fileNameOriginal)) {
            CodecDocument document = openDocumentWithProtectedSource(fileNameOriginal, password);
            if (document instanceof AbstractCodecDocument) {
                ((AbstractCodecDocument) document).retainSource(source);
            }
            if (document != null) CacheZipUtils.pruneBookCache();
            return document;
        } finally {
            try { openingLease.close(); } catch (Exception e) { LOG.e(e); }
        }
    }

    private CodecDocument openDocumentWithProtectedSource(String fileNameOriginal, String password) {
        LOG.d("Open-Document", fileNameOriginal);
        // TempHolder.loadingCancelled = false;
        if (ExtUtils.isZip(fileNameOriginal)) {
            LOG.d("Open-Document ZIP", fileNameOriginal);
            return openDocumentInnerCanceled(fileNameOriginal, password);
        }

        LOG.d("Open-Document 2 LANG:", AppSP.get().hypenLang, fileNameOriginal);

        File cacheFileName = getCacheFileName(fileNameOriginal + sourceRevisionKey(fileNameOriginal));

        if (cacheFileName != null && cacheFileName.isFile()) {
            LOG.d("Open-Document from cache", fileNameOriginal);
            return openDocumentInnerCanceled(fileNameOriginal, password);
        }

        CacheZipUtils.cacheLock2.lock();
        CacheZipUtils.createAllCacheDirs();
        try {
            String fileName = CacheZipUtils.extracIfNeed(fileNameOriginal, CacheDir.ZipApp).unZipPath;
            LOG.d("Open-Document extract", fileName);
            if (!ExtUtils.isValidFile(fileName)) {
                LOG.d( "isValidFile",fileName);
                return null;
            }
            try {
                return openDocumentInnerCanceled(fileName, password);
            } catch (MuPdfPasswordException e) {
                throw new MuPdfPasswordRequiredException();
            } catch (Throwable e) {
                LOG.w(e);
                return null;
            }
        } finally {
            CacheZipUtils.cacheLock2.unlock();
        }

    }

    public File getCacheFileName(String fileNameOriginal) {
        return null;
    }

    /**
     * Constructor.
     *
     * @param contextHandle contect handler
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
        TempHolder.lock.lock();
        try {
            if (!isRecycled()) {
                freeContext();
                contextHandle = 0;
            }
        } finally {
            TempHolder.lock.unlock();
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
        return AppsConfig.CURRENT_BITMAP_ARGB;
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
                final Field f = VerticalViewActivity.DM.getClass().getDeclaredField("densityDpi");
                densityDPI = ((Integer) f.get(VerticalViewActivity.DM));
            } catch (final Throwable ex) {
                densityDPI = Integer.valueOf(120);
            }
        }
        return densityDPI.intValue();
    }
}
