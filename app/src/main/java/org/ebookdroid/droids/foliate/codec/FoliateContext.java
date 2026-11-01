package org.ebookdroid.droids.foliate.codec;

import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.foobnix.android.utils.LOG;
import com.foobnix.pdf.info.AppsConfig;
import com.foobnix.pdf.info.Engines;
import com.foobnix.LibreraApp;

import org.ebookdroid.core.codec.AbstractCodecContext;
import org.ebookdroid.core.codec.CodecDocument;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reflowable books laid out by foliate-js in a WebView instead of by MuPDF.
 *
 * It is the second engine for every format Librera converts to an {@code .epub}. The book itself is
 * prepared by the format context exactly as before - see
 * {@link org.ebookdroid.droids.mupdf.codec.PdfContext#openReflowable(String, String)}, which is
 * where the two engines part ways - so the conversion, the hyphenation, the inlined footnotes and
 * the cache keys are all shared.
 */
public class FoliateContext extends AbstractCodecContext {

    /** The oldest WebView foliate-js runs on, Chromium's major version. */
    public static final int MIN_WEBVIEW = 85;

    private static String webViewVersion;

    /**
     * Whether this engine can be used right now.
     *
     * It needs an activity on screen: the WebView it paginates in has to be attached to a window
     * or Chromium never navigates the iframe the book is laid out in (see
     * {@link FoliateRenderer}). While a book is being read there always is one. Away from the
     * reader - the library's own covers, a background scan - there is not, and the book falls back
     * to MuPDF, which is no loss: a reflowable book's cover is read out of the archive rather than
     * rendered (see {@code ImageExtractor.proccessCoverPage}).
     */
    public static boolean isAvailable() {
        return LibreraApp.context != null && LibreraApp.getResumedActivity() != null
                && isWebViewSupported();
    }

    /**
     * Whether the WebView can run foliate-js. Below this one its modules do not even parse, as
     * they are written in a syntax no polyfill reaches, and a book would wait out the timeouts
     * before going to MuPDF. A flavour without MuPDF has nothing to go to, so there it is always
     * tried, and so is a WebView that does not say what it is.
     */
    public static boolean isWebViewSupported() {
        if (!Engines.IS_MUPDF) {
            return true;
        }
        final String version = webViewVersion();
        final int dot = version.indexOf('.');
        try {
            return version.isEmpty()
                    || Integer.parseInt(dot > 0 ? version.substring(0, dot) : version) >= MIN_WEBVIEW;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    /** The version of the system's WebView, such as 91.0.4472.114, or empty if there is none. */
    public static synchronized String webViewVersion() {
        if (webViewVersion == null) {
            webViewVersion = readWebViewVersion();
        }
        return webViewVersion;
    }

    private static String readWebViewVersion() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final PackageInfo info = WebView.getCurrentWebViewPackage();
                return info != null && info.versionName != null ? info.versionName : "";
            }
            final Matcher matcher = Pattern.compile("Chrome/([0-9.]+)")
                    .matcher(WebSettings.getDefaultUserAgent(LibreraApp.context));
            return matcher.find() ? matcher.group(1) : "";
        } catch (Throwable e) {
            LOG.e(e);
            return "";
        }
    }

    @Override
    public CodecDocument openDocumentInner(final String fileName, final String password) {
        final FoliateDocument document = new FoliateDocument(this, fileName);
        if (document.openEngine()) {
            return document;
        }
        document.disposeEngine();
        return null;
    }

    @Override
    public Bitmap.Config getBitmapConfig() {
        return AppsConfig.CURRENT_BITMAP_ARGB;
    }

    @Override
    public boolean isParallelPageAccessAvailable() {
        // One WebView, one iframe: every call is serialized inside FoliateRenderer
        return false;
    }
}
