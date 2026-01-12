package org.ebookdroid.droids.mupdf.codec;

import android.graphics.RectF;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.ext.CacheZipUtils;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.ExtUtils;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.sys.TempHolder;

import org.ebookdroid.BookType;
import org.ebookdroid.core.codec.AbstractCodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.droids.EpubContext;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MuPdfDocument extends AbstractCodecDocument {

    public static final int FORMAT_PDF = 0;

    public static final String META_INFO_AUTHOR = "info:Author";
    public static final String META_INFO_TITLE = "info:Title";
    public static final String META_INFO_SUBJECT = "info:Subject";
    public static final String META_INFO_KEYWORDS = "info:Keywords";
    public static final String META_INFO_CREATOR = "info:Creator";
    public static final String META_INFO_PRODUCER = "info:Producer";
    public static final String META_INFO_CREATIONDATE = "info:CreationDate";
    public static final String META_INFO_MODIFICATIONDATE = "info:ModDate";
    private static long cacheHandle;
    private static int cacheWH;
    private static long cacheSize;
    private static int cacheCount;
    int w, h;
    BookType bookType;
    private boolean isEpub = false;
    private volatile Map<String, String> footNotes;
    private volatile List<String> mediaAttachment;
    private int pagesCount = -1;
    private String fname;

    public MuPdfDocument(final MuPdfContext context, final int format, final String fname, final String pwd) {
        super(context, openFile(format, fname, pwd, BookCSS.get().toCssString(fname)));
        this.fname = fname;
        isEpub = ExtUtils.isTextFomat(fname);
        bookType = BookType.getByUri(fname);
    }

    static void normalizeLinkTargetRect(final long docHandle, final int targetPage, final RectF targetRect,
                                        final int flags) {

        if ((flags & 0x0F) == 0) {
            targetRect.right = targetRect.left = 0;
            targetRect.bottom = targetRect.top = 0;
            return;
        }

        final CodecPageInfo cpi = new CodecPageInfo();
        TempHolder.lock.lock();
        try {
            MuPdfDocument.getPageInfo(docHandle, targetPage, cpi);
        } finally {
            TempHolder.lock.unlock();
        }

        final float left = targetRect.left;
        final float top = targetRect.top;

        if (((cpi.rotation / 90) % 2) != 0) {
            targetRect.right = targetRect.left = left / cpi.height;
            targetRect.bottom = targetRect.top = 1.0f - top / cpi.width;
        } else {
            targetRect.right = targetRect.left = left / cpi.width;
            targetRect.bottom = targetRect.top = 1.0f - top / cpi.height;
        }
    }

    native static int getPageInfo(long docHandle, int pageNumber, CodecPageInfo cpi);

    // 'info:Title'
    // 'info:Author'
    // 'info:Subject'
    // 'info:Keywords'
    // 'info:Creator'
    // 'info:Producer'
    // 'info:CreationDate'
    // 'info:ModDate'
    private native static String getMeta(long docHandle, final String option);

    private native static String setMetaData(long docHandle, final String key, String value);

    private static long openFile(final int format, String fname, final String pwd, String css) {
        TempHolder.lock.lock();
        try {
            int allocatedMemory = AppState.get().allocatedMemorySize * 1024 * 1024;
            // int allocatedMemory = CoreSettings.get().pdfStorageSize;
            LOG.d("allocatedMemory", AppState.get().allocatedMemorySize, " MB " + allocatedMemory);
            int isImageScale = AppState.get().enableImageScale ? 1 : 0;

            LOG.d("accel cache1", fname);
            String accel = new EpubContext().getCacheFileName(fname).getPath() + "+accel";
            accel = accel.replace(CacheZipUtils.CACHE_BOOK_DIR.getPath(), CacheZipUtils.CACHE_TEMP.getPath());
            LOG.d("accel cache2", accel, new File(accel).exists());

            final long
                    open =
                    open(allocatedMemory, format, fname, pwd, css, BookCSS.get().documentStyle == BookCSS.STYLES_ONLY_USER ? 0 : 1, BookCSS.get().imageScale, AppState.get().antiAliasLevel, accel, isImageScale);
            LOG.d("TEST", "Open document " + fname + " " + open);
            LOG.d("TEST", "Open document css ", css);
            LOG.d("TEST", "Open document isImageScale ", isImageScale);
            LOG.d("MUPDF! >>> open [document]", open, ExtUtils.getFileName(fname));

            if (open == -1) {
                throw new RuntimeException("Document is corrupted");
            }

            // final int n = getPageCountWithException(open);
            return open;
        } finally {
            TempHolder.lock.unlock();
        }
    }

    public static native String getFzVersion();

    private static native long open(int storememory, int format, String fname, String pwd, String css, int useDocStyle,
                                    float scale, int antialias, String accel, int isImageScale);

    private static native void free(long handle);

    private static int getPageCountWithException(final long handle, int w, int h, int size) {
        final int count = getPageCountSafe(handle, w, h, Dips.spToPx(size));
//        if (count == 0) {
//            throw new RuntimeException("Document is corrupted");
//        }
        return count;
    }

    private static int getPageCountSafe(long handle, int w, int h, int size) {
        LOG.d("getPageCountSafe w h size", w, h, size);

        if (handle == cacheHandle && size == cacheSize && w + h == cacheWH) {
            LOG.d("getPageCount from cache", cacheCount);
            return cacheCount;
        }
        TempHolder.lock.lock();
        try {
            cacheHandle = handle;
            cacheSize = size;
            cacheWH = w + h;
            cacheCount = getPageCount(handle, w, h, size);
            LOG.d("getPageCount put to  cache", cacheCount);
            return cacheCount;
        } catch (Exception e) {
            return -1;
        } finally {
            TempHolder.lock.unlock();
        }
    }

    private static native int getPageCount(long handle, int w, int h, int size);

    public String getPath() {
        return fname;
    }

    @Override
    public void setMeta(String key, String value) {
        TempHolder.lock.lock();
        try {
            LOG.d(this.getClass(), "setMetaData", key, value);
            setMetaData(documentHandle, key, value);
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public BookType getBookType() {
        return bookType;
    }

    @Override
    public String documentToHtml() {
        StringBuilder out = new StringBuilder();
        int pages = getPageCount();
        for (int i = 0; i < pages; i++) {
            CodecPage pageCodec = getPage(i);
            String pageHTML = pageCodec.getPageHTML();
            out.append(pageHTML);
        }
        return out.toString();
    }

    @Override
    public Map<String, String> getFootNotes() {
        return footNotes;
    }

    public void setFootNotes(Map<String, String> footNotes) {
        this.footNotes = footNotes;
    }

    @Override
    public List<OutlineLink> getOutline() {
        final MuPdfOutline ou = new MuPdfOutline();
        return ou.getOutline(documentHandle);
    }

    @Override
    public CodecPage getPageInner(final int pageNumber) {
        MuPdfPage createPage = MuPdfPage.createPage(this, pageNumber + 1);
        return createPage;
    }

    @Override
    public int getPageCount() {
        LOG.d("MuPdfDocument,getPageCount", getW(), getH(), BookCSS.get().fontSizeSp);
        return getPageCountWithException(documentHandle, getW(), getH(), BookCSS.get().fontSizeSp);
    }

    @Override
    public CodecPageInfo getUnifiedPageInfo() {
        if (isEpub) {
            LOG.d("MuPdfDocument, getUnifiedPageInfo");
            return new CodecPageInfo(getW(), getH());
        } else {
            return null;
        }
    }

    @Override
    public int getPageCount(int w, int h, int size) {
        this.w = w;
        this.h = h;
        int pageCountWithException = getPageCountWithException(documentHandle, w, h, size);
        LOG.d("MuPdfDocument,, getPageCount", w, h, size, "count", pageCountWithException);
        return pageCountWithException;
    }

    public int getW() {
        return w > 0 ? w : Dips.screenWidth();
    }

    public int getH() {
        return h > 0 ? h : Dips.screenHeight();
    }

    @Override
    public CodecPageInfo getPageInfo(final int pageNumber) {
        final CodecPageInfo info = new CodecPageInfo();
        TempHolder.lock.lock();
        try {
            final int res = getPageInfo(documentHandle, pageNumber + 1, info);
            if (res == -1) {
                return null;
            } else {
                // Check rotation
                info.rotation = (360 + info.rotation) % 360;
                return info;
            }
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    protected void freeDocument() {
        TempHolder.lock.lock();
        try {
            cacheHandle = -1;
            free(documentHandle);
        } finally {
            TempHolder.lock.unlock();
        }

        LOG.d("MUPDF! <<< recycle [document]", documentHandle, ExtUtils.getFileName(fname));
    }

    @Override
    public String getMeta(final String option) {
        TempHolder.lock.lock();
        try {

            if (true) {
                return getMeta(documentHandle, option);
            }

            final AtomicBoolean ready = new AtomicBoolean(false);
            final StringBuilder info = new StringBuilder();

            new Thread("@T extract meta") {
                @Override
                public void run() {

                    try {
                        LOG.d("getMeta", option);
                        String key = getMeta(documentHandle, option);
                        info.append(key);
                    } catch (Throwable e) {
                        LOG.e(e);
                    } finally {
                        ready.set(true);
                    }

                }

                ;
            }.start();

            while (!ready.get()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }

            return info.toString();
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public String getBookTitle() {
        return getMeta("info:Title");
    }

    @Override
    public String getBookAuthor() {
        return getMeta("info:Author");
    }

    private native void saveInternal(long handle, String path);

    private native boolean hasChangesInternal(long handle);

    boolean isHasChanges = false;

    @Override
    public boolean hasChanges() {

        if (isHasChanges) {
            LOG.d("hasChanges cache");
            return true;
        }
        TempHolder.lock.lock();
        try {
            LOG.d("hasChanges internal");
            isHasChanges = hasChangesInternal(documentHandle);
            return isHasChanges;
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public void saveAnnotations(String path) {
        LOG.d("Save Annotations saveInternal 1");
        TempHolder.lock.lock();
        try {
            saveInternal(documentHandle, path);
            LOG.d("Save Annotations saveInternal 2");
        } finally {
            TempHolder.lock.unlock();
        }
    }

    @Override
    public List<RectF> searchText(final int pageNuber, final String pattern) throws DocSearchNotSupported {
        throw new DocSearchNotSupported();
    }

    @Override
    public void deleteAnnotation(long pageHandle, int index) {
        TempHolder.lock.lock();
        try {
            deleteAnnotationInternal(documentHandle, pageHandle, index);
        } finally {
            TempHolder.lock.unlock();
        }

    }

    private native void deleteAnnotationInternal(long docHandle, long pageHandle, int annot_index);

    public void setMediaAttachment(List<String> mediaAttachment) {
        this.mediaAttachment = mediaAttachment;
    }

    @Override
    public List<String> getMediaAttachments() {
        return mediaAttachment;
    }

}
