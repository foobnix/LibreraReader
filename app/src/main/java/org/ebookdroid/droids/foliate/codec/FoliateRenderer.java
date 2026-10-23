package org.ebookdroid.droids.foliate.codec;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebChromeClient;
import android.webkit.WebViewClient;

import com.foobnix.LibreraApp;
import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.AppsConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The WebView foliate-js runs in, and the only class here that touches the main thread.
 *
 * A WebView may only be created and called from the main thread, while Librera decodes pages on a
 * worker - the single Glide source thread in paged mode, the single {@code DecodeServiceBase} loop
 * in scroll mode. So every call here posts its work to the main thread and blocks the caller until
 * the answer comes back, the pattern {@link com.foobnix.android.utils.WebViewUtils} already uses to
 * rasterise MathML during an epub conversion.
 *
 * <b>The WebView has to be attached to a window</b>, which is why this needs an activity on screen.
 * {@code WebViewUtils} gets away with an unattached one because it renders a single document, but
 * Chromium builds no subframe tree for a WebView outside a window: the iframe foliate paginates a
 * section in never navigates - its document stays empty at {@code about:blank} and its load event
 * never fires - and {@code View.layout} alone does not resize the web contents either, so the page
 * would be laid out at the screen's size rather than the one asked for. Attached, both work. The
 * view is parked outside the activity's bounds so nothing is ever seen or touched, and it is
 * removed again when the book is closed.
 *
 * <b>A caller already on the main thread is never made to wait.</b> It cannot be: the work it would
 * wait for runs on the thread it is holding. Librera does call in from there -
 * {@code AbstractViewController.show()} asks for {@code findBookmarkPage} on the UI thread, and the
 * periodic save asks for {@code getBookmarkText} - so those calls return a miss immediately
 * instead. MuPDF degrades the same way for the same reason, with a 200 ms {@code tryLock} and the
 * comment "a heavy page holds the lock for seconds, waiting for it froze the ui: ANR". Losing the
 * position hint costs a slightly wrong restore; deadlocking costs the app.
 *
 * Note this takes a lock of its own and deliberately not {@link com.foobnix.sys.TempHolder#lock}.
 * That one lock guards every MuPDF native call in the process and is already its worst contention
 * point; holding it across a WebView round trip would stall TTS, search and the UI thread for as
 * long as a page takes to lay out. foliate shares no native state with MuPDF, so there is nothing
 * to coordinate.
 */
class FoliateRenderer {

    /** How long a page layout, a capture or a text extraction may take before it is given up on. */
    private static final long CALL_TIMEOUT_MS = 20000;

    /** Loading the shell imports foliate's modules; the book itself is opened separately. */
    private static final long READY_TIMEOUT_MS = 20000;

    /** Walking a whole book's sections can take a while on a long book. */
    private static final long INDEX_TIMEOUT_MS = 180000;

    private static final AtomicLong IDS = new AtomicLong();

    private final Handler handler = new Handler(Looper.getMainLooper());

    /** One call at a time: there is one WebView and one iframe inside it. */
    private final ReentrantLock callLock = new ReentrantLock();

    private final Map<String, Pending> pending = new ConcurrentHashMap<String, Pending>();

    private final Context context;

    private final String bookPath;

    private volatile WebView web;

    /** The group the WebView was parked in, so it can be taken out again. */
    private volatile ViewGroup host;

    private volatile boolean destroyed;

    /** Counts down when the shell has imported foliate and is ready to be asked to open a book. */
    private final CountDownLatch ready = new CountDownLatch(1);

    private volatile int pageWidth;

    private volatile int pageHeight;

    /**
     * Where the last open's time went, in ms: {@code create} the WebView, {@code ready} the shell
     * importing foliate, and the bridge's own {@code fetch}, {@code parse} and {@code first}
     * section. For the logs and the benchmark.
     */
    volatile JSONObject openTimes = new JSONObject();

    FoliateRenderer(final Context context, final String bookPath) {
        this.context = context;
        this.bookPath = bookPath;
    }

    /** A request waiting for the JS to answer it. */
    private static final class Pending {
        final CountDownLatch done = new CountDownLatch(1);
        volatile String result;
        volatile String error;
    }

    /** Whether the calling thread may be made to wait at all; see the class comment. */
    static boolean canWait() {
        return Looper.myLooper() != Looper.getMainLooper();
    }

    // ---------------------------------------------------------------- lifecycle

    /**
     * Creates the WebView, loads the shell and opens the book at the given page size.
     *
     * @return the number of sections, or -1 when the book could not be opened
     */
    int open(final int width, final int height) {
        if (!canWait()) {
            LOG.d("FOLIATE! open skipped on the main thread");
            return -1;
        }
        this.pageWidth = Math.max(1, width);
        this.pageHeight = Math.max(1, height);

        final long started = System.currentTimeMillis();
        final CountDownLatch created = new CountDownLatch(1);
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    create();
                } catch (Throwable e) {
                    LOG.e(e);
                } finally {
                    created.countDown();
                }
            }
        });
        if (!await(created, READY_TIMEOUT_MS, "create") || web == null) {
            return -1;
        }
        final long createdAt = System.currentTimeMillis();
        if (!await(ready, READY_TIMEOUT_MS, "ready")) {
            return -1;
        }
        final long readyAt = System.currentTimeMillis();

        final String answer = call("open", config(), CALL_TIMEOUT_MS);
        if (answer == null) {
            return -1;
        }
        try {
            final JSONObject json = new JSONObject(answer);
            final JSONObject times = json.optJSONObject("ms") == null ? new JSONObject() : json.getJSONObject("ms");
            times.put("create", createdAt - started);
            times.put("ready", readyAt - createdAt);
            times.put("total", System.currentTimeMillis() - started);
            openTimes = times;
            LOG.d("FOLIATE! open ms", times);
            return json.optInt("sections", -1);
        } catch (Throwable e) {
            LOG.e(e);
            return -1;
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void create() {
        final FoliateServer server = new FoliateServer(context, bookPath);
        final WebView local = new WebView(context);
        final WebSettings settings = local.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        // The shell is laid out by hand at the page size; neither of these may second-guess it
        settings.setUseWideViewPort(false);
        settings.setLoadWithOverviewMode(false);
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        // Everything the page may read is served by FoliateServer over its own origin, and
        // anything off it is refused there - so the shell itself, which is an https URL, still
        // loads. setBlockNetworkLoads is deliberately not used: it would be a second gate in
        // front of the interceptor that the shell's own load has to pass through too.
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setGeolocationEnabled(false);
        settings.setTextZoom(100);

        local.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(final WebView view,
                                                              final WebResourceRequest request) {
                return server.answer(request);
            }

            @Override
            public boolean shouldOverrideUrlLoading(final WebView view, final WebResourceRequest request) {
                // Only the shell's own navigation may be refused. This is also asked about every
                // subframe navigation, and the paginator lays each section out by navigating its
                // iframe to a blob: URL - refusing those cancels them, and the book never appears.
                if (request == null || !request.isForMainFrame()) {
                    return false;
                }
                final String url = request.getUrl() == null ? "" : request.getUrl().toString();
                final boolean away = !url.startsWith(FoliateServer.ORIGIN);
                if (away) {
                    LOG.d("FOLIATE! refused to navigate away", url);
                }
                return away;
            }

            @Override
            public void onReceivedError(final WebView view, final WebResourceRequest request,
                                        final android.webkit.WebResourceError error) {
                LOG.d("FOLIATE!error", request.getUrl(), error.getErrorCode(), error.getDescription());
            }

            @Override
            public void onPageFinished(final WebView view, final String url) {
                LOG.d("FOLIATE!loaded", url);
            }
        });
        // Whatever the page says about itself goes to logcat: a module that fails to import, or a
        // book whose own script throws, is otherwise invisible from here.
        local.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(final ConsoleMessage message) {
                LOG.d("FOLIATE!js", message.messageLevel(), message.message(),
                        message.sourceId() + ":" + message.lineNumber());
                return true;
            }
        });
        local.addJavascriptInterface(new Bridge(), "librera");
        // Nothing may reach it: it is not on screen and must never take a touch or the focus
        local.setClickable(false);
        local.setFocusable(false);
        local.setFocusableInTouchMode(false);
        local.setHorizontalScrollBarEnabled(false);
        local.setVerticalScrollBarEnabled(false);
        // Parked a whole page to the left of the activity. It stays VISIBLE on purpose - GONE or
        // INVISIBLE would stop Chromium compositing it, and a page that is not composited cannot
        // be drawn.
        local.setTranslationX(-2f * pageWidth);

        final ViewGroup root = contentRoot();
        if (root == null) {
            LOG.d("FOLIATE! no activity to host the WebView");
            local.destroy();
            return;
        }
        // addView, not setLayoutParams: the parent converts the params to its own type, and a
        // FrameLayout - which android.R.id.content is - crashes on measure if handed a plain pair.
        root.addView(local, 0, new ViewGroup.LayoutParams(pageWidth, pageHeight));
        host = root;
        web = local;
        LOG.d("FOLIATE!attach", LibreraApp.getResumedActivity(), root,
                pageWidth + "x" + pageHeight);
        local.post(new Runnable() {
            @Override
            public void run() {
                LOG.d("FOLIATE!attached", local.isAttachedToWindow(),
                        local.getWidth() + "x" + local.getHeight(),
                        "windowVisible=" + local.getWindowVisibility());
            }
        });
        local.loadUrl(FoliateServer.SHELL_URL);
    }

    /** The content view of the activity on screen, or null when there is none. */
    private static ViewGroup contentRoot() {
        final Activity activity = LibreraApp.getResumedActivity();
        if (activity == null || activity.isFinishing()) {
            return null;
        }
        try {
            return (ViewGroup) activity.findViewById(android.R.id.content);
        } catch (Throwable e) {
            LOG.e(e);
            return null;
        }
    }

    /** Frees the WebView. Safe from any thread. */
    void destroy() {
        destroyed = true;
        final WebView local = web;
        web = null;
        for (Pending p : pending.values()) {
            p.error = "destroyed";
            p.done.countDown();
        }
        pending.clear();
        if (local == null) {
            return;
        }
        final ViewGroup parent = host;
        host = null;
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (parent != null) {
                        parent.removeView(local);
                    }
                    local.removeJavascriptInterface("librera");
                    local.loadUrl("about:blank");
                    local.destroy();
                    LOG.d("FOLIATE! renderer destroyed");
                } catch (Throwable e) {
                    LOG.e(e);
                }
            }
        });
    }

    boolean isUsable() {
        return !destroyed && web != null;
    }

    // ---------------------------------------------------------------- the calls

    /**
     * Lays out every section in turn and reports how many pages each holds.
     *
     * @return the per-section page counts, or <code>null</code>
     */
    int[] index() {
        final String answer = call("index", new JSONObject(), INDEX_TIMEOUT_MS);
        if (answer == null) {
            return null;
        }
        try {
            final org.json.JSONArray array = new JSONObject(answer).getJSONArray("pages");
            final int[] counts = new int[array.length()];
            for (int i = 0; i < counts.length; i++) {
                counts[i] = Math.max(1, array.getInt(i));
            }
            return counts;
        } catch (Throwable e) {
            LOG.e(e);
            return null;
        }
    }

    /**
     * The same walk, also saying where each section's time went; see the bridge's index. For the
     * benchmark.
     *
     * @return the bridge's whole answer - {@code pages} and {@code profile} - or <code>null</code>
     */
    JSONObject indexProfile() {
        try {
            final JSONObject args = new JSONObject();
            args.put("profile", true);
            final String answer = call("index", args, INDEX_TIMEOUT_MS);
            return answer == null ? null : new JSONObject(answer);
        } catch (Throwable e) {
            LOG.e(e);
            return null;
        }
    }

    /**
     * Positions the book on one page of one section.
     *
     * @return how many characters of text that page holds, or -1 if it could not be positioned.
     *         A page with text that draws blank was drawn before it was ready; a page with none
     *         is the end of a chapter and is blank for good.
     */
    int goTo(final int section, final float fraction) {
        final JSONObject args = new JSONObject();
        try {
            args.put("index", section);
            args.put("fraction", fraction);
        } catch (Throwable e) {
            LOG.e(e);
            return -1;
        }
        final String answer = call("goTo", args, CALL_TIMEOUT_MS);
        if (answer == null) {
            return -1;
        }
        try {
            return Math.max(0, new JSONObject(answer).optInt("chars", 0));
        } catch (Throwable e) {
            LOG.e(e);
            return 0;
        }
    }

    /** Draws the current page again, without moving the book. */
    Bitmap redraw(final int width, final int height) {
        if (call("settle", new JSONObject(), CALL_TIMEOUT_MS) == null) {
            return null;
        }
        return capture(width, height);
    }

    /**
     * Draws whatever page the book is positioned on.
     *
     * The WebView has already been told, as part of the preceding {@code goTo}, to let two frames
     * pass before answering, so the layout it is drawn from is the one that was asked for.
     */
    Bitmap capture(final int width, final int height) {
        if (!canWait() || !isUsable()) {
            return null;
        }
        final Bitmap[] out = new Bitmap[1];
        final CountDownLatch done = new CountDownLatch(1);
        handler.post(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = null;
                try {
                    final WebView local = web;
                    if (local == null) {
                        return;
                    }
                    bitmap = Bitmap.createBitmap(width, height, AppsConfig.CURRENT_BITMAP_ARGB);
                    final Canvas canvas = new Canvas(bitmap);
                    local.draw(canvas);
                    out[0] = bitmap;
                    bitmap = null;
                } catch (Throwable e) {
                    LOG.e(e);
                } finally {
                    if (bitmap != null) {
                        bitmap.recycle();
                    }
                    done.countDown();
                }
            }
        });
        if (!await(done, CALL_TIMEOUT_MS, "capture")) {
            return null;
        }
        return out[0];
    }

    /** Re-lays the WebView out, for a page size that is not the one the book was opened at. */
    boolean resize(final int width, final int height) {
        if (width == pageWidth && height == pageHeight) {
            return true;
        }
        if (!canWait() || !isUsable()) {
            return false;
        }
        pageWidth = Math.max(1, width);
        pageHeight = Math.max(1, height);
        final CountDownLatch done = new CountDownLatch(1);
        handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    final WebView local = web;
                    final ViewGroup.LayoutParams params = local == null ? null : local.getLayoutParams();
                    if (params != null) {
                        // The params the parent generated, resized in place - replacing them with
                        // a plain pair would crash the parent's measure pass. The size has to come
                        // from the layout rather than View.layout(): it is the laid-out size the
                        // web contents viewport follows.
                        params.width = pageWidth;
                        params.height = pageHeight;
                        local.setLayoutParams(params);
                        local.setTranslationX(-2f * pageWidth);
                        local.requestLayout();
                    }
                } catch (Throwable e) {
                    LOG.e(e);
                } finally {
                    done.countDown();
                }
            }
        });
        if (!await(done, CALL_TIMEOUT_MS, "resize")) {
            return false;
        }
        // The paginator re-columnises on its own resize observer; let it settle
        return call("settle", new JSONObject(), CALL_TIMEOUT_MS) != null;
    }

    /** The CFI of the page the book is on, or null. */
    String location() {
        final String answer = call("location", new JSONObject(), CALL_TIMEOUT_MS);
        if (answer == null) {
            return null;
        }
        try {
            final String cfi = new JSONObject(answer).optString("cfi");
            return cfi == null || cfi.isEmpty() ? null : cfi;
        } catch (Throwable e) {
            LOG.e(e);
            return null;
        }
    }

    /**
     * Puts the book where a CFI points.
     *
     * @return the section and the fraction it landed on, for the page index to turn into a page,
     *         or <code>null</code> when the CFI means nothing in this book
     */
    double[] resolve(final String cfi) {
        final JSONObject args = new JSONObject();
        try {
            args.put("cfi", cfi);
        } catch (Throwable e) {
            LOG.e(e);
            return null;
        }
        final String answer = call("resolve", args, CALL_TIMEOUT_MS);
        if (answer == null) {
            return null;
        }
        try {
            final JSONObject json = new JSONObject(answer);
            final int index = json.optInt("index", -1);
            if (index < 0) {
                return null;
            }
            return new double[]{index, json.optDouble("fraction", 0)};
        } catch (Throwable e) {
            LOG.e(e);
            return null;
        }
    }

    /** The rectangles a CFI covers on the page now shown, as JSON, or null. */
    String rects(final String cfi) {
        final JSONObject args = new JSONObject();
        try {
            args.put("cfi", cfi);
        } catch (Throwable e) {
            LOG.e(e);
            return null;
        }
        return call("rects", args, CALL_TIMEOUT_MS);
    }

    /**
     * A CFI for the characters under a selection on the page now shown, or null.
     *
     * @param rects the selected rectangles, each normalized to the page
     */
    String cfiAt(final List<RectF> rects) {
        final JSONObject args = new JSONObject();
        try {
            final JSONArray array = new JSONArray();
            for (RectF rect : rects) {
                final JSONObject one = new JSONObject();
                one.put("l", rect.left);
                one.put("t", rect.top);
                one.put("r", rect.right);
                one.put("b", rect.bottom);
                array.put(one);
            }
            args.put("rects", array);
        } catch (Throwable e) {
            LOG.e(e);
            return null;
        }
        final String answer = call("cfiAt", args, CALL_TIMEOUT_MS);
        if (answer == null) {
            return null;
        }
        try {
            final String cfi = new JSONObject(answer).optString("cfi");
            return cfi == null || cfi.isEmpty() ? null : cfi;
        } catch (Throwable e) {
            LOG.e(e);
            return null;
        }
    }

    /** The current page's words with their rectangles, as JSON, or null. */
    String words() {
        return call("words", new JSONObject(), CALL_TIMEOUT_MS);
    }

    /** The current page's text as Librera's pseudo-HTML, as JSON, or null. */
    String text() {
        return call("text", new JSONObject(), CALL_TIMEOUT_MS);
    }

    /** The current page's links with their rectangles, as JSON, or null. */
    String links() {
        return call("links", new JSONObject(), CALL_TIMEOUT_MS);
    }

    /**
     * The book's table of contents with each entry's section and fraction, as JSON, or null.
     *
     * An entry that points inside a section is found by going to it, which leaves the book on
     * another page. Every chapter of an FB2 is such an entry - the whole book is one section - so
     * this can take as long as walking the book does.
     */
    String toc() {
        return call("toc", new JSONObject(), INDEX_TIMEOUT_MS);
    }

    // ---------------------------------------------------------------- plumbing

    private JSONObject config() {
        final JSONObject config = new JSONObject();
        try {
            config.put("book", FoliateServer.bookUrl());
            config.put("width", pageWidth);
            config.put("height", pageHeight);
            config.put("fontSizePx", FoliateCss.rootFontSizePx());
            config.put("before", FoliateCss.before());
            config.put("after", FoliateCss.after(bookPath));
            final int[] paginator = FoliateCss.paginatorConfig();
            // Device pixels; the page knows its own scale, so it turns them into the percentage
            // and the CSS pixels the paginator wants
            config.put("sidesPx", paginator[0]);
            config.put("bandPx", paginator[1]);
            config.put("background", String.format("#%06X", FoliateCss.backgroundColor() & 0xFFFFFF));
        } catch (Throwable e) {
            LOG.e(e);
        }
        return config;
    }

    /**
     * Sends one request to the page and waits for its answer.
     *
     * @return the answer as JSON, or <code>null</code> on a failure, a timeout, or a call from the
     *         main thread
     */
    private String call(final String method, final JSONObject args, final long timeoutMs) {
        if (!canWait()) {
            LOG.d("FOLIATE! call skipped on the main thread", method);
            return null;
        }
        if (!isUsable()) {
            return null;
        }
        callLock.lock();
        try {
            if (!isUsable()) {
                return null;
            }
            final String id = "r" + IDS.incrementAndGet();
            final Pending p = new Pending();
            pending.put(id, p);
            try {
                final String script = "window.libreraCall(" + quote(id) + "," + quote(method)
                        + "," + quote(args.toString()) + ")";
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        final WebView local = web;
                        if (local == null) {
                            p.error = "no web view";
                            p.done.countDown();
                            return;
                        }
                        try {
                            local.evaluateJavascript(script, null);
                        } catch (Throwable e) {
                            LOG.e(e);
                            p.error = String.valueOf(e.getMessage());
                            p.done.countDown();
                        }
                    }
                });
                if (!await(p.done, timeoutMs, method)) {
                    return null;
                }
                if (p.error != null) {
                    LOG.d("FOLIATE! call failed", method, p.error);
                    return null;
                }
                return p.result;
            } finally {
                pending.remove(id);
            }
        } finally {
            callLock.unlock();
        }
    }

    private static boolean await(final CountDownLatch latch, final long timeoutMs, final String what) {
        try {
            if (latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
                return true;
            }
            LOG.d("FOLIATE! timed out", what, timeoutMs);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** A JavaScript string literal; the values are ids, method names and JSON. */
    private static String quote(final String value) {
        return JSONObject.quote(value);
    }

    /**
     * What the page calls back into. Every method here runs on the WebView's JavaScript thread,
     * not the main thread and not the caller's.
     */
    private final class Bridge {

        @JavascriptInterface
        public void ready() {
            LOG.d("FOLIATE! shell ready");
            ready.countDown();
        }

        @JavascriptInterface
        public void reply(final String id, final String json) {
            final Pending p = pending.get(id);
            if (p == null) {
                return;
            }
            p.result = json;
            p.done.countDown();
        }

        @JavascriptInterface
        public void fail(final String id, final String message) {
            final Pending p = pending.get(id);
            if (p == null) {
                LOG.d("FOLIATE! unmatched failure", id, message);
                return;
            }
            p.error = message;
            p.done.countDown();
        }

        @JavascriptInterface
        public void log(final String message) {
            LOG.d("FOLIATE! js", message);
        }
    }
}
