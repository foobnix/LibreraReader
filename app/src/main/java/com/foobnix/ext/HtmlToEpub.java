package com.foobnix.ext;

import android.net.Uri;

import com.foobnix.android.utils.LOG;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Wraps one generated HTML file in the smallest EPUB that holds it.
 *
 * Several of Librera's formats are not converted to an {@code .epub} by their context but to a
 * loose {@code .html} written into {@link CacheZipUtils#CACHE_BOOK_DIR} with its images, its CSS
 * and its fonts beside it: {@code .docx}, {@code .odt}, {@code .doc}, {@code .rtf}, {@code .mht},
 * {@code .txt} and {@code .html} itself. MuPDF opens such a file directly; foliate-js does not -
 * it reads a book out of an archive - so in a build without MuPDF (see
 * {@link com.foobnix.pdf.info.Engines#IS_MUPDF}) those formats had no engine at all.
 *
 * This puts the file into a one-chapter EPUB and brings the resources it names along with it, so
 * every one of those formats opens with the engine that is there and nothing about the conversions
 * themselves has to change. The archive is written beside the HTML as {@code <name>.epub}, which
 * is on purpose: {@link CacheZipUtils#removeFiles(File[], File)} keeps the files whose name starts
 * with the cache file's, so the wrapper is cached and pruned exactly like the HTML it holds.
 */
public class HtmlToEpub {

    /** What is appended to the HTML's own name to get the archive's. */
    public static final String EXT = ".epub";

    private static final String OEBPS = "OEBPS/";

    private static final String OPF_PATH = OEBPS + "content.opf";

    /** The spine item is served as HTML: these files are loose HTML, not well-formed XHTML. */
    private static final String HTML_MIME = "text/html";

    /**
     * What a resource may be. A book's picture, its stylesheet and its fonts are worth carrying;
     * anything else a generated file happens to name - a script, another document - is not, and
     * leaving it out keeps the archive to what the page actually draws.
     */
    private static final Map<String, String> MIME = new HashMap<String, String>();

    static {
        MIME.put("png", "image/png");
        MIME.put("jpg", "image/jpeg");
        MIME.put("jpeg", "image/jpeg");
        MIME.put("jpe", "image/jpeg");
        MIME.put("gif", "image/gif");
        MIME.put("bmp", "image/bmp");
        MIME.put("webp", "image/webp");
        MIME.put("svg", "image/svg+xml");
        MIME.put("tif", "image/tiff");
        MIME.put("tiff", "image/tiff");
        MIME.put("css", "text/css");
        MIME.put("ttf", "font/ttf");
        MIME.put("otf", "font/otf");
        MIME.put("woff", "font/woff");
        MIME.put("woff2", "font/woff2");
    }

    /** {@code src=}, {@code href=}, {@code poster=} and {@code data=} on any element. */
    private static final Pattern ATTRIBUTE = Pattern.compile(
            "(?i)(?:src|href|poster|data)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')");

    /** {@code url(...)} in a {@code <style>} block or a {@code style=} attribute. */
    private static final Pattern CSS_URL = Pattern.compile(
            "(?i)url\\(\\s*[\"']?([^\"')]+)");

    private static final Pattern TITLE = Pattern.compile(
            "(?is)<title[^>]*>(.*?)</title>");

    /** An absolute reference - {@code http:}, {@code data:}, {@code file:} - which is not ours. */
    private static final Pattern SCHEME = Pattern.compile("^[A-Za-z][A-Za-z0-9+.\\-]*:");

    private HtmlToEpub() {
    }

    /**
     * The archive holding this HTML file, built when it is not there already.
     *
     * @param htmlPath the file a format context generated, with its resources beside it
     * @return the path of the {@code .epub}, or <code>null</code> when there was nothing to wrap
     *         or the archive could not be written - in which case the caller is no worse off than
     *         before and can hand the HTML itself on
     */
    public static String wrap(final String htmlPath) {
        if (htmlPath == null) {
            return null;
        }
        final File html = new File(htmlPath);
        if (!html.isFile()) {
            LOG.d("HtmlToEpub there is no file to wrap", htmlPath);
            return null;
        }
        final File epub = new File(html.getParentFile(), html.getName() + EXT);
        // The archive is written after the HTML, so an older one can only be a previous open's
        if (epub.isFile() && epub.length() > 0 && epub.lastModified() >= html.lastModified()) {
            LOG.d("HtmlToEpub cache", epub.getPath());
            return epub.getPath();
        }
        try {
            build(html, epub);
            LOG.d("HtmlToEpub wrapped", htmlPath, "as", epub.getPath(), epub.length());
            return epub.getPath();
        } catch (Throwable e) {
            LOG.e(e, htmlPath);
            // A half-written archive would be read as a fresh one on the next open
            epub.delete();
            return null;
        }
    }

    private static void build(final File html, final File epub) throws IOException {
        final String text = read(html);
        final List<String> resources = resources(html.getParentFile(), text);

        final ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(epub));
        try {
            // "mimetype" comes first and uncompressed, as the format asks
            stored(zip, "mimetype", "application/epub+zip");

            write(zip, "META-INF/container.xml",
                    "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                            + "<container version=\"1.0\""
                            + " xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">"
                            + "<rootfiles><rootfile full-path=\"" + OPF_PATH + "\""
                            + " media-type=\"application/oebps-package+xml\"/>"
                            + "</rootfiles></container>");

            write(zip, OPF_PATH, opf(html.getName(), title(text, html), resources));

            // The chapter keeps its own name, so the references inside it resolve against the
            // package document's folder exactly as they did against the cache folder. It is copied
            // rather than written back from the text above: the file is already UTF-8 - Librera
            // wrote it - and a large book is big enough that one copy of it is enough.
            copy(new FileInputStream(html), zip, OEBPS + html.getName());

            for (final String resource : resources) {
                copy(new FileInputStream(new File(html.getParentFile(), resource)),
                        zip, OEBPS + resource);
            }
        } finally {
            zip.close();
        }
    }

    private static String opf(final String chapter, final String title, final List<String> resources) {
        final StringBuilder opf = new StringBuilder();
        opf.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        opf.append("<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"2.0\"");
        opf.append(" unique-identifier=\"bookid\">");
        opf.append("<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">");
        opf.append("<dc:identifier id=\"bookid\">librera-html-");
        opf.append(chapter.hashCode()).append("</dc:identifier>");
        opf.append("<dc:title>").append(escape(title)).append("</dc:title>");
        opf.append("</metadata><manifest>");
        opf.append("<item id=\"main\" href=\"").append(escape(chapter));
        opf.append("\" media-type=\"").append(HTML_MIME).append("\"/>");
        for (int i = 0; i < resources.size(); i++) {
            final String resource = resources.get(i);
            opf.append("<item id=\"r").append(i).append("\" href=\"").append(escape(resource));
            opf.append("\" media-type=\"").append(MIME.get(extension(resource))).append("\"/>");
        }
        opf.append("</manifest><spine><itemref idref=\"main\"/></spine></package>");
        return opf.toString();
    }

    /**
     * The files this page draws, as paths relative to the folder it is in.
     *
     * foliate-js resolves a reference against the package document and then looks the result up in
     * the manifest, so a resource that is not listed there is simply not loaded - which is why this
     * reads the references out of the page rather than sweeping the cache folder, where every other
     * book's leftovers also live.
     */
    private static List<String> resources(final File dir, final String text) {
        final Set<String> found = new LinkedHashSet<String>();
        final Matcher attributes = ATTRIBUTE.matcher(text);
        while (attributes.find()) {
            add(found, dir, attributes.group(1) != null ? attributes.group(1) : attributes.group(2));
        }
        final Matcher urls = CSS_URL.matcher(text);
        while (urls.find()) {
            add(found, dir, urls.group(1));
        }
        return new ArrayList<String>(found);
    }

    private static void add(final Set<String> found, final File dir, final String reference) {
        final String path = normalize(reference);
        if (path == null || found.contains(path)) {
            return;
        }
        if (!MIME.containsKey(extension(path))) {
            return;
        }
        if (!new File(dir, path).isFile()) {
            LOG.d("HtmlToEpub reference has no file", path);
            return;
        }
        found.add(path);
    }

    /**
     * A reference as a path inside the archive, or <code>null</code> when it does not name one.
     *
     * The decoding matters: a reference is resolved by the reader as a URL and compared against the
     * manifest decoded, so that is the form the manifest and the archive entry have to be in.
     */
    private static String normalize(final String reference) {
        if (reference == null) {
            return null;
        }
        String path = reference.trim();
        final int fragment = path.indexOf('#');
        if (fragment >= 0) {
            path = path.substring(0, fragment);
        }
        final int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        if (path.isEmpty() || path.startsWith("/") || SCHEME.matcher(path).find()) {
            return null;
        }
        while (path.startsWith("./")) {
            path = path.substring(2);
        }
        path = Uri.decode(path);
        // Nothing may reach outside the folder the page was written into
        if (path.isEmpty() || path.startsWith("/") || path.contains("..") || path.contains("\\")) {
            return null;
        }
        return path;
    }

    private static String title(final String text, final File html) {
        final Matcher matcher = TITLE.matcher(text);
        if (matcher.find()) {
            final String title = matcher.group(1).replaceAll("<[^>]*>", "").trim();
            if (!title.isEmpty()) {
                return title;
            }
        }
        return html.getName();
    }

    // ---------------------------------------------------------------- the archive

    private static void stored(final ZipOutputStream zip, final String name, final String content)
            throws IOException {
        final byte[] bytes = content.getBytes("UTF-8");
        final ZipEntry entry = new ZipEntry(name);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(bytes.length);
        entry.setCompressedSize(bytes.length);
        final CRC32 crc = new CRC32();
        crc.update(bytes);
        entry.setCrc(crc.getValue());
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }

    private static void write(final ZipOutputStream zip, final String name, final String content)
            throws IOException {
        copy(new ByteArrayInputStream(content.getBytes("UTF-8")), zip, name);
    }

    private static void copy(final InputStream in, final ZipOutputStream zip, final String name)
            throws IOException {
        try {
            zip.putNextEntry(new ZipEntry(name));
            final byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                zip.write(buffer, 0, read);
            }
            zip.closeEntry();
        } finally {
            in.close();
        }
    }

    /**
     * The page as text.
     *
     * Every one of these files is written by Librera itself and in UTF-8, whatever the book it came
     * from was in, so there is no encoding to guess here.
     */
    private static String read(final File file) throws IOException {
        final InputStream in = new FileInputStream(file);
        try {
            final byte[] bytes = new byte[(int) file.length()];
            int done = 0;
            while (done < bytes.length) {
                final int read = in.read(bytes, done, bytes.length - done);
                if (read < 0) {
                    break;
                }
                done += read;
            }
            return new String(bytes, 0, done, "UTF-8");
        } finally {
            in.close();
        }
    }

    private static String extension(final String path) {
        final int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.US);
    }

    private static String escape(final String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
