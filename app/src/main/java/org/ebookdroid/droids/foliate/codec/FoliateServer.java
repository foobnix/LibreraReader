package org.ebookdroid.droids.foliate.codec;

import android.content.Context;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import com.foobnix.android.utils.LOG;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Serves the reader shell, foliate-js, the book and the user's fonts to the WebView.
 *
 * It all goes through one private <code>https://</code> origin rather than <code>file://</code>
 * because foliate-js is a set of ES modules, and an <code>import</code> is refused from a
 * <code>file://</code> page - the origin is opaque, so the module fetch is cross-origin. A made-up
 * https host served from {@code shouldInterceptRequest} gives the page a real origin, which also
 * means the book and the fonts can be handed over without any file-access settings.
 */
class FoliateServer {

    /** The origin the shell is served from. Not a real host; nothing leaves the device. */
    static final String ORIGIN = "https://librera.foliate";

    static final String SHELL_URL = ORIGIN + "/librera.html";

    private static final String ASSET_DIR = "foliate";

    private static final String BOOK_PATH = "/book.epub";

    private static final String FONT_PATH = "/font";

    private static final Map<String, String> MIME = new HashMap<String, String>();

    static {
        MIME.put("html", "text/html");
        MIME.put("js", "text/javascript");
        MIME.put("mjs", "text/javascript");
        MIME.put("css", "text/css");
        MIME.put("json", "application/json");
        MIME.put("epub", "application/epub+zip");
        MIME.put("ttf", "font/ttf");
        MIME.put("otf", "font/otf");
        MIME.put("woff", "font/woff");
        MIME.put("woff2", "font/woff2");
        MIME.put("png", "image/png");
        MIME.put("jpg", "image/jpeg");
        MIME.put("jpeg", "image/jpeg");
        MIME.put("gif", "image/gif");
        MIME.put("svg", "image/svg+xml");
        MIME.put("xhtml", "application/xhtml+xml");
    }

    private final Context context;

    /** The book this WebView is serving: the path the format context prepared. */
    private final String bookPath;

    FoliateServer(final Context context, final String bookPath) {
        this.context = context;
        this.bookPath = bookPath;
    }

    /** The URL the page fetches the book from. */
    static String bookUrl() {
        return ORIGIN + BOOK_PATH;
    }

    /** The URL a `@font-face` rule loads a filesystem font through. */
    static String fontUrl(final String path) {
        return ORIGIN + FONT_PATH + "?p=" + FoliateCss.encode(path);
    }

    /**
     * @return the response, or <code>null</code> to let the WebView handle the request itself -
     *         which, for anything not on this origin, means refusing it
     */
    WebResourceResponse answer(final WebResourceRequest request) {
        if (request == null || request.getUrl() == null) {
            return null;
        }
        final String url = request.getUrl().toString();
        LOG.d("FOLIATE!req", url);
        if (!url.startsWith(ORIGIN)) {
            // foliate reads the book inside the page and hands its sections and resources to the
            // DOM as blob: URLs, so those are the book's own content and the WebView serves them
            // from memory - there is nothing here to serve and nothing to refuse.
            if (url.startsWith("blob:") || url.startsWith("data:") || url.startsWith("about:")) {
                return null;
            }
            // Anything else would be a real fetch: a book that tries to pull in a remote script,
            // a font or a tracker gets an empty response rather than network access.
            LOG.d("FOLIATE! blocked", url);
            return deny();
        }

        String path = request.getUrl().getPath();
        if (path == null || path.isEmpty() || "/".equals(path)) {
            path = "/librera.html";
        }

        try {
            if (BOOK_PATH.equals(path)) {
                return book();
            }
            if (FONT_PATH.equals(path)) {
                return font(request.getUrl().getEncodedQuery());
            }
            return asset(path);
        } catch (Throwable e) {
            LOG.e(e, url);
            return deny();
        }
    }

    private WebResourceResponse book() throws Exception {
        final File file = new File(bookPath);
        if (!file.isFile()) {
            LOG.d("FOLIATE! book is missing", bookPath);
            return deny();
        }
        return response("application/epub+zip", new FileInputStream(file));
    }

    private WebResourceResponse font(final String query) throws Exception {
        if (query == null || !query.startsWith("p=")) {
            return deny();
        }
        final String path = decode(query.substring(2));
        final File file = new File(path);
        // Only a real file, and only a font: the query is built from the user's own settings, but
        // it arrives here as page input, so it is not trusted to name any file on the device.
        final String ext = extension(path);
        if (!file.isFile() || !MIME.containsKey(ext) || !isFontExt(ext)) {
            LOG.d("FOLIATE! font refused", path);
            return deny();
        }
        return response(MIME.get(ext), new FileInputStream(file));
    }

    private static boolean isFontExt(final String ext) {
        return "ttf".equals(ext) || "otf".equals(ext) || "woff".equals(ext) || "woff2".equals(ext);
    }

    private WebResourceResponse asset(final String path) throws Exception {
        // Everything servable lives under assets/foliate, so a path cannot climb out of it
        final String name = ASSET_DIR + "/" + path.replace("..", "").replaceAll("^/+", "");
        final InputStream stream = context.getAssets().open(name);
        return response(mime(path), stream);
    }

    private static WebResourceResponse response(final String mime, final InputStream data) {
        final WebResourceResponse response = new WebResourceResponse(mime, "UTF-8", data);
        final Map<String, String> headers = new HashMap<String, String>();
        // One origin, served locally; the shell's modules and the book's own resources are all on it
        headers.put("Access-Control-Allow-Origin", ORIGIN);
        headers.put("Cache-Control", "no-store");
        response.setResponseHeaders(headers);
        return response;
    }

    private static WebResourceResponse deny() {
        final WebResourceResponse response = new WebResourceResponse(
                "text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
        response.setStatusCodeAndReasonPhrase(404, "Not Found");
        return response;
    }

    private static String mime(final String path) {
        final String mime = MIME.get(extension(path));
        return mime != null ? mime : "application/octet-stream";
    }

    private static String extension(final String path) {
        final int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.US);
    }

    private static String decode(final String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
