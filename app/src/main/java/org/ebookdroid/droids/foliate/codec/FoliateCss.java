package org.ebookdroid.droids.foliate.codec;

import com.foobnix.android.utils.Dips;
import com.foobnix.android.utils.LOG;
import com.foobnix.model.AppState;
import com.foobnix.pdf.info.model.BookCSS;
import com.foobnix.pdf.info.wrapper.MagicHelper;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The reading settings as a stylesheet foliate-js can use.
 *
 * {@link BookCSS#generatedCss(String, boolean)} writes CSS for MuPDF, which is a print-layout
 * engine with no stylesheet of its own and no notion of a viewport. Three of the things it emits
 * mean something different in a browser, and are translated here:
 *
 * <ul>
 * <li><b>{@code font-size:medium !important}</b> is dropped. Font size is not CSS in Librera - it
 *     is a layout argument to {@code fz_layout_document}, and that rule exists to flatten the
 *     document's own sizes so the argument wins. In a browser it would throw away every relative
 *     size, so the real size is set on the root element instead (see {@link #rootFontSizePx()}).</li>
 * <li>The <b>{@code @page}</b> margin block is dropped: a browser does not paginate from it. The
 *     margins go to the paginator instead, as {@link #paginatorConfig()}.</li>
 * <li><b>{@code src:url('/abs/path.ttf')}</b> is rewritten to {@link FoliateServer}'s font
 *     endpoint. A scheme-less absolute path resolves against the page, not the filesystem, so the
 *     user's fonts would silently not load.</li>
 * <li><b>{@code font-family:'Charis SIL'}</b> and the other fonts built into MuPDF get a generic
 *     family after them, as the WebView has no font by that name (see {@link #BUILT_IN_FONT}).</li>
 * </ul>
 *
 * The result is handed to foliate's {@code renderer.setStyles([before, after])}: {@link #before()}
 * is prepended to the section's head, so the book's own CSS still overrides it, and {@link
 * #after(String)} is appended, so the reader's settings override the book.
 */
public class FoliateCss {

    /** `@page { ... }` - the paginator gets the margins instead. */
    private static final Pattern PAGE_RULE = Pattern.compile("@page\\s*\\{[^}]*\\}");

    /** `src:url('...')` as BookCSS writes it, with a bare filesystem path inside. */
    private static final Pattern FONT_URL = Pattern.compile("src:url\\('([^']*)'\\)");

    /** The exact declaration BookCSS emits; see the class comment. */
    private static final String FONT_SIZE_HACK = "font-size:medium !important;";

    /**
     * `font-family:'Charis SIL'` and the other fonts MuPDF carries inside itself. A browser has no
     * font by those names; Android aliases Times New Roman, Arial and Courier to its own serif,
     * sans and mono, but anything else - Charis SIL - falls back to the default sans-serif.
     */
    private static final Pattern BUILT_IN_FONT = Pattern.compile(
            "font-family:'(" + Pattern.quote(BookCSS.TIMES_NEW_ROMAN) + "|" + Pattern.quote(BookCSS.ARIAL)
                    + "|" + Pattern.quote(BookCSS.COURIER) + "|" + Pattern.quote(BookCSS.CHARIS_SIL) + ")'");

    /**
     * The stylesheet prepended to every section: what the base stylesheet asset would have given a
     * browser that it does not already have itself.
     *
     * A browser needs none of that asset's `display` declarations, but it does need the elements
     * Librera invents, because they are in the book's text by the time the engine sees it:
     * {@code <t>} for an inlined footnote, {@code <pn>} for an EPUB3 page marker and
     * {@code <x-small>} for a reference-mode paragraph index are written into the cached epub by
     * {@link com.foobnix.ext.EpubExtractor}, and a book converted from FB2 carries FB2's own
     * element names. Unstyled, those would all render as plain inline text.
     */
    static String before() {
        final StringBuilder css = new StringBuilder();

        // Librera's own injected elements
        css.append("t{font-style:italic;}");
        css.append("pn{font-size:0.8em;font-style:italic;}");
        css.append("x-small{font-size:0.6em;}");

        // Headings centred unless the book says otherwise, as the base stylesheet does for MuPDF;
        // a browser's own start alignment would let them inherit the body's justify instead
        css.append("h1,h2,h3,h4,h5,h6{text-align:center;}");

        // FB2, for a book converted to epub
        css.append("section,title,subtitle,epigraph,text-author,date,poem,stanza,v,empty-line");
        css.append("{display:block;}");
        css.append("title{text-align:center;font-weight:bold;}");
        css.append("subtitle{text-align:center;font-weight:bold;}");
        css.append("epigraph,text-author{font-style:italic;}");
        css.append("emphasis{font-style:italic;}");
        css.append("strikethrough{text-decoration:line-through;}");
        css.append("letter{float:left;}");

        return css.toString();
    }

    /**
     * The rules nothing in the book may override: the page's own margins, and hyphenation.
     *
     * These go last, with <code>!important</code>, because they are not styling - they are the
     * contract between the book and the way Librera paginates it.
     */
    private static String overrides() {
        final StringBuilder css = new StringBuilder();

        // The page's margins come from the reading settings and are applied by the paginator, as
        // MuPDF applies them through @page. A book's own body margin would be added on top of
        // them, so it goes - the same thing the base stylesheet does for MuPDF with
        // `body{padding:0 !important; margin:0 !important}`.
        css.append("html,body{margin:0 !important;padding:0 !important;}");

        // Only Librera's hyphens. The conversion has already put a soft hyphen at every break it
        // wants (see HypenUtils), and `manual` is exactly "break only where you were told to" -
        // so with the setting off there is no hyphenation at all, as with MuPDF. Without this a
        // book carrying `hyphens:auto` would get the browser's hyphenation on top of Librera's.
        css.append("html,body,div,p,span,li,td,th{");
        css.append("hyphens:manual !important;-webkit-hyphens:manual !important;");
        css.append("}");

        // An image bigger than the column is clipped away by the pagination rather than shrunk
        css.append("img,svg,image{max-width:100% !important;max-height:100% !important;}");

        // The paragraph indent is for paragraphs. text-indent is inherited, so without these a
        // heading, a block image and an FB2 title would all be pushed in by it - the base
        // stylesheet stops that for MuPDF with the same !important rules.
        css.append("h1,h2,h3,h4,h5,h6,img,svg{text-indent:0 !important;}");
        css.append("title>p,subtitle{text-align:center !important;text-indent:0 !important;}");

        return css.toString();
    }

    /**
     * The reading settings, appended to the section's head so they win over the book's own CSS.
     *
     * @param bookPath the book, passed on to BookCSS as its own callers do
     */
    static String after(final String bookPath) {
        String css;
        try {
            css = BookCSS.get().generatedCss(bookPath, AppState.get().epubFoliateBaseCss, true);
        } catch (Throwable e) {
            LOG.e(e);
            return overrides();
        }
        if (css == null) {
            return overrides();
        }
        css = css.replace(FONT_SIZE_HACK, "");
        css = PAGE_RULE.matcher(css).replaceAll("");
        return withGenericFallback(rewriteFontUrls(css)) + overrides();
    }

    /** Gives each of MuPDF's built-in font names the generic family that looks most like it. */
    private static String withGenericFallback(final String css) {
        final Matcher matcher = BUILT_IN_FONT.matcher(css);
        final StringBuffer out = new StringBuffer(css.length());
        while (matcher.find()) {
            final String name = matcher.group(1);
            final String generic = BookCSS.ARIAL.equals(name) ? "sans-serif"
                    : BookCSS.COURIER.equals(name) ? "monospace" : "serif";
            matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group() + "," + generic));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /** Sends every `src:url('/abs/path.ttf')` through {@link FoliateServer}'s font endpoint. */
    private static String rewriteFontUrls(final String css) {
        final Matcher matcher = FONT_URL.matcher(css);
        final StringBuffer out = new StringBuffer(css.length());
        while (matcher.find()) {
            final String path = matcher.group(1);
            final String replacement = path != null && path.startsWith("/")
                    ? "src:url('" + FoliateServer.fontUrl(path) + "')"
                    : matcher.group();
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    /**
     * The em size in device pixels: foliate-js's own font size, kept apart from MuPDF's
     * ({@code Dips.spToPx(fontSizeSpFoliate)}). The page is captured at device pixels, so the bridge
     * divides this by {@code window.devicePixelRatio} to get the CSS pixels to set on the root.
     */
    static int rootFontSizePx() {
        try {
            return Math.max(1, Dips.spToPx(BookCSS.get().fontSizeSpFoliate));
        } catch (Throwable e) {
            LOG.e(e);
            return 16;
        }
    }

    /**
     * The page margins in device pixels: foliate-js's own, kept apart from MuPDF's.
     *
     * BookCSS stores a margin in tenths of an em and writes `@page` with twice that - so a stored
     * 10 is `em(20)`, two ems (see {@code BookCSS.em} and the `@page` block it builds). The
     * paginator takes its own in pixels and percentages, so the em is resolved here against the
     * font size rather than being passed through as a bare number.
     *
     * @return `{sides, band}` - the left and right margins added together, and one of the top and
     *         bottom ones; the paginator has a single value for the vertical pair
     */
    static int[] paginatorConfig() {
        final BookCSS css = BookCSS.get();
        final float em = rootFontSizePx();
        // `margin * 2 / 10` ems, as @page gets it
        final float sides = (css.marginLeftFoliate + css.marginRightFoliate) * 0.2f * em;
        // @page takes a line off the bottom margin; the paginator has one value for both, so the
        // two are averaged rather than one of them being dropped
        final float band = (css.marginTopFoliate + Math.max(0, css.marginBottomFoliate - 1)) * 0.5f * 0.2f * em;
        return new int[]{Math.max(0, Math.round(sides)), Math.max(0, Math.round(band))};
    }

    /** The page background, so a captured page matches the reader's own colours. */
    static int backgroundColor() {
        try {
            return MagicHelper.getBgColor();
        } catch (Throwable e) {
            LOG.e(e);
            return 0xFFFFFFFF;
        }
    }

    static String encode(final String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
