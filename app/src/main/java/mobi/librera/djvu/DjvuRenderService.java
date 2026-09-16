package mobi.librera.djvu;

import android.app.Service;
import android.content.Intent;
import android.graphics.RectF;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.AppsConfig;

import org.ebookdroid.core.codec.CodecPageInfo;
import org.ebookdroid.core.codec.OutlineLink;
import org.ebookdroid.core.codec.PageTextBox;
import org.ebookdroid.droids.djvu.codec.DjvuContext;
import org.ebookdroid.droids.djvu.codec.DjvuDocument;
import org.ebookdroid.droids.djvu.codec.DjvuPage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Renders DjVu documents for other apps through {@link IDjvuRenderer}, with Librera's own
 * djvulibre. It runs in a process of its own (":djvu"), so a document that crashes the decoder
 * takes down neither Librera nor the app asking.
 */
public class DjvuRenderService extends Service {

    public static final String ACTION = "mobi.librera.djvu.RENDER";
    public static final int API_VERSION = 1;

    private static final String TAG = "DjvuRenderService";

    /** Decoded pages kept open for each document: the ones on screen and either side. */
    private static final int MAX_OPEN_PAGES = 4;

    /** The largest picture drawn at once, in pixels: a 4K page and some. */
    private static final long MAX_PIXELS = 40_000_000L;

    private final AtomicLong ids = new AtomicLong();
    private final Map<Long, Doc> docs = new ConcurrentHashMap<>();
    private final ExecutorService writers = Executors.newCachedThreadPool();

    private static final class Doc {
        final int uid;
        final ParcelFileDescriptor fd;
        /** This service's own copy of the document, where the descriptor could not be read by name. */
        final File copy;
        final DjvuDocument document;
        final int pageCount;
        final LinkedHashMap<Integer, DjvuPage> pages = new LinkedHashMap<>(16, 0.75f, true);
        boolean closed;

        Doc(int uid, ParcelFileDescriptor fd, File copy, DjvuDocument document) {
            this.uid = uid;
            this.fd = fd;
            this.copy = copy;
            this.document = document;
            this.pageCount = document.getPageCount();
        }

        DjvuPage page(int index) {
            DjvuPage page = pages.get(index);
            if (page != null && !page.isRecycled()) {
                return page;
            }
            page = document.getPageInner(index);
            pages.put(index, page);
            if (pages.size() > MAX_OPEN_PAGES) {
                Iterator<Map.Entry<Integer, DjvuPage>> it = pages.entrySet().iterator();
                Map.Entry<Integer, DjvuPage> eldest = it.next();
                it.remove();
                eldest.getValue().recycle();
            }
            return page;
        }

        void close() {
            if (closed) {
                return;
            }
            closed = true;
            for (DjvuPage page : pages.values()) {
                page.recycle();
            }
            pages.clear();
            // Frees the ddjvu context together with the document.
            document.recycle();
            closeQuietly(fd);
            if (copy != null) {
                copy.delete();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Touching AppsConfig loads libMuPDF.so, which holds djvulibre and its JNI bridge.
        LOG.d(TAG, "onCreate", AppsConfig.MUPDF_FZ_VERSION);
        // Copies left behind by a process that did not get to close its documents.
        File[] stale = copies().listFiles();
        if (stale != null) {
            for (File f : stale) {
                f.delete();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        for (Doc doc : docs.values()) {
            synchronized (doc) {
                doc.close();
            }
        }
        docs.clear();
        writers.shutdownNow();
        super.onDestroy();
    }

    /** The caller's document, or an error that crosses the binder to the caller. */
    private Doc doc(long id) {
        Doc doc = docs.get(id);
        if (doc == null || doc.closed) {
            throw new IllegalStateException("document is not open: " + id);
        }
        if (doc.uid != Binder.getCallingUid()) {
            throw new SecurityException("document belongs to another app");
        }
        return doc;
    }

    private static void checkPage(Doc doc, int page) {
        if (page < 0 || page >= doc.pageCount) {
            throw new IllegalArgumentException("page " + page + " of " + doc.pageCount);
        }
    }

    private final IDjvuRenderer.Stub binder = new IDjvuRenderer.Stub() {

        @Override
        public int getApiVersion() {
            return API_VERSION;
        }

        @Override
        public long open(ParcelFileDescriptor fd) {
            if (fd == null) {
                throw new IllegalArgumentException("no file descriptor");
            }
            long id = ids.incrementAndGet();
            File copy = null;
            DjvuContext context = null;
            try {
                String path = readablePath(fd);
                if (path == null) {
                    copy = new File(copies(), id + ".djvu");
                    copyOut(fd, copy);
                    path = copy.getPath();
                }
                context = new DjvuContext();
                DjvuDocument document = context.openDocumentInner(path, null);
                context = null; // the document owns it now, and frees it with itself
                Doc doc = new Doc(Binder.getCallingUid(), fd, copy, document);
                if (doc.pageCount <= 0) {
                    doc.close();
                    throw new IllegalStateException("the document has no pages");
                }
                docs.put(id, doc);
                LOG.d(TAG, "open", id, doc.pageCount, path);
                return id;
            } catch (IllegalStateException e) {
                throw e;
            } catch (Throwable e) {
                LOG.e(e);
                if (context != null) {
                    context.recycle();
                }
                closeQuietly(fd);
                if (copy != null) {
                    copy.delete();
                }
                throw new IllegalStateException("cannot open the document: " + e.getMessage());
            }
        }

        @Override
        public void close(long id) {
            Doc doc = docs.get(id);
            if (doc == null || doc.uid != Binder.getCallingUid()) {
                return;
            }
            docs.remove(id);
            synchronized (doc) {
                doc.close();
            }
        }

        @Override
        public int getPageCount(long id) {
            return doc(id).pageCount;
        }

        @Override
        public int[] getPageSize(long id, int page) {
            Doc doc = doc(id);
            checkPage(doc, page);
            synchronized (doc) {
                CodecPageInfo info = doc.document.getPageInfo(page);
                if (info == null) {
                    throw new IllegalStateException("no size for page " + page);
                }
                return new int[]{info.width, info.height, info.dpi};
            }
        }

        @Override
        public ParcelFileDescriptor render(long id, int page, final int width, final int height,
                                           float left, float top, float right, float bottom) {
            Doc doc = doc(id);
            checkPage(doc, page);
            if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                throw new IllegalArgumentException("picture size " + width + "x" + height);
            }
            if (!(right > left) || !(bottom > top)) {
                throw new IllegalArgumentException("empty part of the page");
            }
            final int[] pixels = new int[width * height];
            synchronized (doc) {
                if (doc.closed) {
                    throw new IllegalStateException("document is closed");
                }
                try {
                    doc.page(page).renderPixels(width, height, new RectF(left, top, right, bottom), pixels, 0);
                } catch (Throwable e) {
                    LOG.e(e);
                    throw new IllegalStateException("cannot draw page " + page + ": " + e.getMessage());
                }
            }
            final ParcelFileDescriptor[] pipe;
            try {
                pipe = ParcelFileDescriptor.createPipe();
            } catch (IOException e) {
                throw new IllegalStateException("no pipe: " + e.getMessage());
            }
            // The pipe holds a few kilobytes, the picture megabytes: the pixels go down it from
            // another thread while the caller reads, and the read end goes back now. A caller
            // that stops reading closes its end, and the write fails and ends here.
            writers.execute(() -> {
                try (OutputStream out = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])) {
                    byte[] row = new byte[width * 4];
                    for (int y = 0; y < height; y++) {
                        int at = y * width;
                        for (int x = 0, b = 0; x < width; x++) {
                            int c = pixels[at + x];
                            row[b++] = (byte) (c >> 16);
                            row[b++] = (byte) (c >> 8);
                            row[b++] = (byte) c;
                            row[b++] = (byte) 0xFF;
                        }
                        out.write(row);
                    }
                } catch (IOException e) {
                    LOG.d(TAG, "render: reader went away", e.getMessage());
                }
            });
            // The stub writes a returned descriptor with PARCELABLE_WRITE_RETURN_VALUE, which
            // closes this process's copy of the read end once it is sent.
            return pipe[0];
        }

        @Override
        public Bundle getPageText(long id, int page) {
            Doc doc = doc(id);
            checkPage(doc, page);
            List<PageTextBox> words;
            synchronized (doc) {
                try {
                    words = doc.document.getPageWords(page);
                } catch (Throwable e) {
                    LOG.e(e);
                    words = null;
                }
            }
            int n = words == null ? 0 : words.size();
            String[] text = new String[n];
            float[] rects = new float[n * 4];
            for (int i = 0; i < n; i++) {
                PageTextBox box = words.get(i);
                text[i] = box.text == null ? "" : box.text;
                rects[i * 4] = box.left;
                rects[i * 4 + 1] = box.top;
                rects[i * 4 + 2] = box.right;
                rects[i * 4 + 3] = box.bottom;
            }
            Bundle out = new Bundle();
            out.putStringArray("words", text);
            out.putFloatArray("rects", rects);
            return out;
        }

        @Override
        public Bundle getOutline(long id) {
            Doc doc = doc(id);
            List<OutlineLink> links;
            synchronized (doc) {
                try {
                    links = doc.document.getOutline();
                } catch (Throwable e) {
                    LOG.e(e);
                    links = null;
                }
            }
            List<OutlineLink> kept = new ArrayList<>();
            if (links != null) {
                for (OutlineLink link : links) {
                    // The outline ends with an empty entry of level -1 that Librera's own
                    // views use as a sentinel.
                    if (link.getLevel() >= 0 && link.getTitle() != null) {
                        kept.add(link);
                    }
                }
            }
            int n = kept.size();
            String[] titles = new String[n];
            int[] levels = new int[n];
            int[] pages = new int[n];
            for (int i = 0; i < n; i++) {
                OutlineLink link = kept.get(i);
                titles[i] = link.getTitle();
                levels[i] = link.getLevel();
                pages[i] = pageOf(link.getLink(), doc.pageCount);
            }
            Bundle out = new Bundle();
            out.putStringArray("titles", titles);
            out.putIntArray("levels", levels);
            out.putIntArray("pages", pages);
            return out;
        }

        @Override
        public Bundle getMeta(long id) {
            Doc doc = doc(id);
            Bundle out = new Bundle();
            synchronized (doc) {
                try {
                    putIfAny(out, "title", doc.document.getBookTitle());
                    putIfAny(out, "author", doc.document.getBookAuthor());
                } catch (Throwable e) {
                    LOG.e(e);
                }
            }
            return out;
        }
    };

    private File copies() {
        File dir = new File(getCacheDir(), "djvu-render");
        dir.mkdirs();
        return dir;
    }

    /**
     * A name djvulibre can open the descriptor by, or null. The descriptor is this process's
     * own and /proc names it, but opening that name checks the file itself again: a file private
     * to the calling app is refused, and so is a pipe, which cannot be read twice. Those are
     * copied instead.
     */
    private static String readablePath(ParcelFileDescriptor fd) {
        if (fd.getStatSize() < 0) {
            return null;
        }
        String path = "/proc/self/fd/" + fd.getFd();
        try (FileInputStream probe = new FileInputStream(path)) {
            return probe.read() >= 0 ? path : null;
        } catch (IOException | SecurityException e) {
            return null;
        }
    }

    private static void copyOut(ParcelFileDescriptor fd, File to) throws IOException {
        // Read through a duplicate, so the original stays open until the document is closed.
        try (InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fd.dup());
             OutputStream out = new FileOutputStream(to)) {
            byte[] buf = new byte[256 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        }
    }

    private static void closeQuietly(ParcelFileDescriptor fd) {
        try {
            fd.close();
        } catch (IOException ignored) {
        }
    }

    private static void putIfAny(Bundle out, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            out.putString(key, value.trim());
        }
    }

    /** The bridge turns a link into "#N", N the 1-based page, where it can resolve it. */
    static int pageOf(String link, int pageCount) {
        if (link == null || !link.startsWith("#")) {
            return -1;
        }
        try {
            int page = Integer.parseInt(link.substring(1).trim()) - 1;
            return page >= 0 && page < pageCount ? page : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
