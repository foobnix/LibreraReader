package com.foobnix.ext.lit;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Converts a Microsoft Reader (.lit) book to an EPUB 2 that MuPDF and foliate read: the book's
 * files with their binary HTML turned into XHTML, an OPF made from the book's own (OEB 1.0) one,
 * and an NCX table of contents - from the book's contents page when it has one, else from the
 * links or headings of its text.
 */
public final class LitEpub {

    private LitEpub() {
    }

    /** What the book says about itself. */
    public static final class Meta {
        public String title;
        public final List<String> authors = new ArrayList<String>();
        public String language;
        public String publisher;
        public String description;
        public String date;
        public String subject;
        public String identifier;
        /** The cover image, or null. */
        public byte[] cover;
        /** After {@link #convert}: what of the file could not be read, or null. */
        public String damage;
    }

    private static final Pattern ATTR = Pattern.compile("([A-Za-z_:][\\w:.-]*)=\"([^\"]*)\"");
    private static final Pattern TAGS = Pattern.compile("<[^>]*>");
    private static final Pattern STYLE = Pattern.compile("(<style[^>]*>)(.*?)(</style>)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTENT_TYPE_META = Pattern.compile("<meta\\s[^>]*http-equiv=\"content-type\"[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern LINK = Pattern.compile("<a\\s([^>]*)>(.*?)</a>", Pattern.DOTALL);
    private static final Pattern HEADING = Pattern.compile("<h([1-3])((?:\\s[^>]*)?)>(.*?)</h\\1>", Pattern.DOTALL);
    private static final Pattern TITLE = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL);
    private static final Pattern LETTER = Pattern.compile("\\p{L}");

    private static final class Book {
        final LitFile lit;
        final Meta meta = new Meta();
        final List<LitFile.Item> spine = new ArrayList<LitFile.Item>();
        final Map<String, LitFile.Item> byPath = new HashMap<String, LitFile.Item>();
        final Map<LitFile.Item, String> ids = new HashMap<LitFile.Item, String>();
        final Map<String, String> guide = new LinkedHashMap<String, String>(); // type -> path
        LitFile.Item coverItem;

        Book(LitFile lit) {
            this.lit = lit;
        }
    }

    private static final class NavPoint {
        final String label;
        final String src;

        NavPoint(String label, String src) {
            this.label = label;
            this.src = src;
        }
    }

    /** The book's metadata, and its cover when asked for, without converting it. */
    public static Meta meta(File file, boolean withCover) throws IOException {
        final Book book = open(file);
        if (withCover && book.coverItem != null) {
            book.meta.cover = book.lit.getRaw(book.coverItem);
        }
        return book.meta;
    }

    /** Writes the book as an EPUB to {@code epub}, and returns its metadata (without the cover). */
    public static Meta convert(File file, File epub) throws IOException {
        final Book book = open(file);

        // the documents, as XHTML
        final Map<LitFile.Item, String> html = new LinkedHashMap<LitFile.Item, String>();
        for (LitFile.Item item : book.lit.getItems()) {
            if (item.isHtml()) {
                try {
                    html.put(item, xhtml(book.lit.getHtml(item)));
                } catch (IOException e) {
                    html.put(item, xhtml("<html><body></body></html>"));
                }
            }
        }

        final List<NavPoint> toc = toc(book, html);

        final File tmp = new File(epub.getPath() + ".tmp");
        final ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)));
        try {
            final byte[] mimetype = LitUnBinary.utf8("application/epub+zip");
            final ZipEntry m = new ZipEntry("mimetype");
            m.setMethod(ZipEntry.STORED);
            m.setSize(mimetype.length);
            final CRC32 crc = new CRC32();
            crc.update(mimetype);
            m.setCrc(crc.getValue());
            zip.putNextEntry(m);
            zip.write(mimetype);
            zip.closeEntry();

            put(zip, "META-INF/container.xml", LitUnBinary.utf8("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">"
                    + "<rootfiles><rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/></rootfiles>"
                    + "</container>"));
            put(zip, "OEBPS/content.opf", LitUnBinary.utf8(opf(book)));
            put(zip, "OEBPS/toc.ncx", LitUnBinary.utf8(ncx(book, toc)));

            for (LitFile.Item item : book.lit.getItems()) {
                final byte[] bytes;
                if (item.isHtml()) {
                    bytes = LitUnBinary.utf8(html.get(item));
                } else {
                    try {
                        bytes = book.lit.getRaw(item);
                    } catch (IOException e) {
                        continue;
                    }
                }
                put(zip, "OEBPS/" + item.path, bytes);
            }
        } finally {
            zip.close();
        }
        if (epub.exists() && !epub.delete()) {
            throw new IOException("Cannot replace " + epub);
        }
        if (!tmp.renameTo(epub)) {
            throw new IOException("Cannot write " + epub);
        }
        book.meta.damage = book.lit.getDamage();
        return book.meta;
    }

    private static void put(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }

    // ------------------------------------------------------------------ the book

    private static Book open(File file) throws IOException {
        final Book book = new Book(new LitFile(file));

        // file names any reader resolves: no spaces, %, commas or quotes to escape in links
        final Set<String> used = new HashSet<String>();
        used.add("content.opf");
        used.add("toc.ncx");
        int n = 0;
        for (LitFile.Item item : book.lit.getItems()) {
            item.path = unique(safe(item.path), used);
            book.byPath.put(item.path, item);
            book.ids.put(item, "item" + (++n));
        }

        String opf = "";
        try {
            opf = book.lit.getOpf();
        } catch (IOException e) {
            // the manifest alone still makes a book
        }

        final Meta meta = book.meta;
        meta.title = dc(opf, "Title");
        final Matcher creators = Pattern.compile("<dc:Creator[^>]*>(.*?)</dc:Creator>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(opf);
        while (creators.find()) {
            final String author = plain(creators.group(1));
            if (!author.isEmpty() && !meta.authors.contains(author)) {
                meta.authors.add(author);
            }
        }
        meta.language = dc(opf, "Language");
        meta.publisher = dc(opf, "Publisher");
        meta.description = dc(opf, "Description");
        meta.date = dc(opf, "Date");
        meta.subject = dc(opf, "Subject");
        meta.identifier = dc(opf, "Identifier");

        // the OPF's item ids, to its spine
        final Map<String, LitFile.Item> opfIds = new HashMap<String, LitFile.Item>();
        for (Map<String, String> a : elements(opf, "item")) {
            final LitFile.Item item = book.byPath.get(decode(a.get("href")));
            if (item != null && a.get("id") != null) {
                opfIds.put(a.get("id"), item);
            }
        }
        for (Map<String, String> a : elements(opf, "itemref")) {
            final LitFile.Item item = opfIds.get(a.get("idref"));
            if (item != null && item.isHtml() && !book.spine.contains(item)) {
                book.spine.add(item);
            }
        }
        if (book.spine.isEmpty()) {
            for (LitFile.Item item : book.lit.getItems()) {
                if ("spine".equals(item.state)) {
                    book.spine.add(item);
                }
            }
        }
        if (book.spine.isEmpty()) {
            for (LitFile.Item item : book.lit.getItems()) {
                if (item.isHtml()) {
                    book.spine.add(item);
                }
            }
        }

        for (Map<String, String> a : elements(opf, "reference")) {
            final String type = a.get("type");
            final String href = a.get("href");
            if (type != null && href != null && !book.guide.containsKey(type.toLowerCase(Locale.US))) {
                book.guide.put(type.toLowerCase(Locale.US), decode(href));
            }
        }

        // the cover: Reader's own cover images first
        for (String type : new String[]{"other.ms-coverimage-standard", "other.ms-coverimage", "cover", "other.ms-titleimage-standard", "other.ms-thumbimage-standard", "other.ms-thumbimage"}) {
            final String path = book.guide.get(type);
            final LitFile.Item item = path == null ? null : book.byPath.get(stripFragment(path));
            if (item != null && "images".equals(item.state)) {
                book.coverItem = item;
                break;
            }
        }
        if (book.coverItem == null) {
            for (LitFile.Item item : book.lit.getItems()) {
                if ("images".equals(item.state) && (item.path + item.internal).toLowerCase(Locale.US).contains("cover")) {
                    book.coverItem = item;
                    break;
                }
            }
        }
        return book;
    }

    private static String safe(String path) {
        final StringBuilder s = new StringBuilder(path.length());
        for (int i = 0; i < path.length(); i++) {
            final char c = path.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '.' || c == '-' || c == '_' || c == '/') {
                s.append(c);
            } else {
                s.append('_');
            }
        }
        String r = s.toString().replaceAll("/+", "/");
        while (r.startsWith("/")) {
            r = r.substring(1);
        }
        return r.isEmpty() || r.endsWith("/") ? r + "file" : r;
    }

    private static String unique(String path, Set<String> used) {
        String r = path;
        final int dot = path.lastIndexOf('.') > path.lastIndexOf('/') ? path.lastIndexOf('.') : path.length();
        for (int i = 2; !used.add(r.toLowerCase(Locale.US)); i++) {
            r = path.substring(0, dot) + "_" + i + path.substring(dot);
        }
        return r;
    }

    private static String dc(String opf, String name) {
        final Matcher m = Pattern.compile("<dc:" + name + "[^>]*>(.*?)</dc:" + name + ">", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(opf);
        if (!m.find()) {
            return null;
        }
        final String v = plain(m.group(1));
        return v.isEmpty() ? null : v;
    }

    private static List<Map<String, String>> elements(String xml, String name) {
        final List<Map<String, String>> list = new ArrayList<Map<String, String>>();
        final Matcher m = Pattern.compile("<" + name + "(\\s[^>]*)?/?>").matcher(xml);
        while (m.find()) {
            list.add(attrs(m.group(1) == null ? "" : m.group(1)));
        }
        return list;
    }

    private static Map<String, String> attrs(String s) {
        final Map<String, String> map = new HashMap<String, String>();
        final Matcher m = ATTR.matcher(s);
        while (m.find()) {
            map.put(m.group(1).toLowerCase(Locale.US), unescape(m.group(2)));
        }
        return map;
    }

    private static String unescape(String s) {
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&");
    }

    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Text of an XML fragment: tags dropped, entities read, white space collapsed. */
    private static String plain(String fragment) {
        return unescape(TAGS.matcher(fragment).replaceAll(" ")).replace(' ', ' ').replaceAll("\\s+", " ").trim();
    }

    private static String decode(String href) {
        if (href == null) {
            return null;
        }
        try {
            return URLDecoder.decode(href.replace("+", "%2B"), "UTF-8");
        } catch (Exception e) {
            return href;
        }
    }

    private static String stripFragment(String path) {
        final int hash = path.indexOf('#');
        return hash < 0 ? path : path.substring(0, hash);
    }

    /** Joins a relative link to the directory of the document it is in. */
    private static String resolve(String docPath, String href) {
        final int slash = docPath.lastIndexOf('/');
        final String dir = slash < 0 ? "" : docPath.substring(0, slash + 1);
        final String joined = LitFile.normpath(dir + href);
        return joined.equals(".") ? "" : joined;
    }

    // ------------------------------------------------------------------ XHTML

    private static String xhtml(String html) {
        String s = html.trim();

        // Word wraps its CSS in <!-- -->, which an XML parser would keep as text
        final Matcher style = STYLE.matcher(s);
        final StringBuffer sb = new StringBuffer();
        while (style.find()) {
            final String css = style.group(2).replace("&lt;!--", "").replace("--&gt;", "");
            style.appendReplacement(sb, Matcher.quoteReplacement(style.group(1) + css + style.group(3)));
        }
        style.appendTail(sb);
        s = sb.toString();

        // the text is UTF-8 now, whatever the book's HTML said
        s = CONTENT_TYPE_META.matcher(s).replaceAll("");

        // one root element: nothing before <html> or after </html>
        final int open = s.indexOf("<html");
        final int close = s.lastIndexOf("</html>");
        if (open >= 0 && close > open) {
            s = s.substring(open, close + 7);
        } else {
            s = "<html>" + (s.contains("<body") ? s : "<body>" + s + "</body>") + "</html>";
        }
        if (!s.startsWith("<html xmlns")) {
            s = "<html xmlns=\"http://www.w3.org/1999/xhtml\"" + s.substring(5);
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + s;
    }

    // ------------------------------------------------------------------ table of contents

    private static List<NavPoint> toc(Book book, Map<LitFile.Item, String> html) {
        final Set<String> spinePaths = new HashSet<String>();
        for (LitFile.Item item : book.spine) {
            spinePaths.add(item.path);
        }

        // the book's own contents page
        final String tocPath = book.guide.get("toc");
        if (tocPath != null) {
            final LitFile.Item page = book.byPath.get(stripFragment(tocPath));
            if (page != null && html.containsKey(page)) {
                final List<NavPoint> links = links(page, html.get(page), spinePaths, false);
                if (!links.isEmpty()) {
                    return links;
                }
            }
        }

        // a list of links in the text to its parts, as a contents page at its start
        for (LitFile.Item item : book.spine) {
            final List<NavPoint> links = links(item, html.get(item), spinePaths, true);
            if (links.size() >= 3) {
                return links;
            }
        }

        // the headings, each given an id to go to
        final List<NavPoint> headings = new ArrayList<NavPoint>();
        int n = 0;
        for (LitFile.Item item : book.spine) {
            final String doc = html.get(item);
            if (doc == null) {
                continue;
            }
            final Matcher m = HEADING.matcher(doc);
            final StringBuffer sb = new StringBuffer();
            while (m.find()) {
                final String label = plain(m.group(3));
                String attrs = m.group(2) == null ? "" : m.group(2);
                if (!label.isEmpty() && LETTER.matcher(label).find()) {
                    String id = attrs(attrs).get("id");
                    if (id == null) {
                        id = "lit-toc-" + (++n);
                        attrs = " id=\"" + id + "\"" + attrs;
                    }
                    headings.add(new NavPoint(clip(label), item.path + "#" + id));
                }
                m.appendReplacement(sb, Matcher.quoteReplacement("<h" + m.group(1) + attrs + ">" + m.group(3) + "</h" + m.group(1) + ">"));
            }
            m.appendTail(sb);
            html.put(item, sb.toString());
        }
        if (!headings.isEmpty()) {
            if (!book.spine.isEmpty() && !headings.get(0).src.startsWith(book.spine.get(0).path + "#")) {
                headings.add(0, new NavPoint(titleOf(html.get(book.spine.get(0)), book.meta.title, 1), book.spine.get(0).path));
            }
            return headings;
        }

        // one entry for each document
        final List<NavPoint> docs = new ArrayList<NavPoint>();
        int i = 0;
        for (LitFile.Item item : book.spine) {
            docs.add(new NavPoint(titleOf(html.get(item), book.spine.size() == 1 ? book.meta.title : null, ++i), item.path));
        }
        return docs;
    }

    private static String clip(String label) {
        return label.length() > 120 ? label.substring(0, 120).trim() + "…" : label;
    }

    private static String titleOf(String doc, String fallback, int index) {
        if (doc != null) {
            final Matcher m = TITLE.matcher(doc);
            if (m.find()) {
                final String t = plain(m.group(1));
                if (!t.isEmpty()) {
                    return t;
                }
            }
        }
        return fallback != null && !fallback.isEmpty() ? fallback : "" + index;
    }

    /**
     * The links of a document that go into the book, with their text: {@code sameDocOnly} keeps
     * the ones to the document itself, a contents list at the head of a single-file book.
     */
    private static List<NavPoint> links(LitFile.Item page, String doc, Set<String> spinePaths, boolean inText) {
        final List<NavPoint> list = new ArrayList<NavPoint>();
        if (doc == null) {
            return list;
        }
        final Set<String> seen = new HashSet<String>();
        final Matcher m = LINK.matcher(doc);
        while (m.find()) {
            final String href = attrs(m.group(1)).get("href");
            if (href == null || href.isEmpty() || href.contains("://") || href.startsWith("mailto:")) {
                continue;
            }
            final String decoded = decode(href);
            final String target;
            if (decoded.startsWith("#")) {
                target = page.path + decoded;
            } else {
                final int hash = decoded.indexOf('#');
                final String path = resolve(page.path, hash < 0 ? decoded : decoded.substring(0, hash));
                target = path + (hash < 0 ? "" : decoded.substring(hash));
            }
            if (!spinePaths.contains(stripFragment(target))) {
                continue;
            }
            final String label = plain(m.group(2));
            if (label.isEmpty() || (inText && (label.length() < 2 || !LETTER.matcher(label).find()))) {
                continue;
            }
            if (seen.add(target)) {
                list.add(new NavPoint(clip(label), target));
            }
        }
        return list;
    }

    // ------------------------------------------------------------------ OPF and NCX

    private static String mediaType(LitFile.Item item) {
        if (item.isHtml()) {
            return "application/xhtml+xml";
        }
        final String mime = item.mimeType;
        final String p = item.path.toLowerCase(Locale.US);
        if ("css".equals(item.state) || mime.contains("css") || p.endsWith(".css")) {
            return "text/css";
        }
        if (mime.equals("image/jpg") || mime.equals("image/pjpeg")) {
            return "image/jpeg";
        }
        if (mime.contains("/") && !mime.contains("x-oeb")) {
            return mime;
        }
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (p.endsWith(".gif")) {
            return "image/gif";
        }
        if (p.endsWith(".png")) {
            return "image/png";
        }
        if (p.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }

    private static String opf(Book book) {
        final Meta meta = book.meta;
        final StringBuilder s = new StringBuilder();
        s.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        s.append("<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"2.0\" unique-identifier=\"bookid\">\n");
        s.append("<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:opf=\"http://www.idpf.org/2007/opf\">\n");
        s.append("<dc:title>").append(escape(meta.title != null ? meta.title : "Untitled")).append("</dc:title>\n");
        for (String author : meta.authors) {
            s.append("<dc:creator opf:role=\"aut\">").append(escape(author)).append("</dc:creator>\n");
        }
        s.append("<dc:language>").append(escape(meta.language != null ? meta.language : "en")).append("</dc:language>\n");
        s.append("<dc:identifier id=\"bookid\">").append(escape(meta.identifier != null ? meta.identifier : "urn:uuid:" + UUID.randomUUID())).append("</dc:identifier>\n");
        if (meta.publisher != null) {
            s.append("<dc:publisher>").append(escape(meta.publisher)).append("</dc:publisher>\n");
        }
        if (meta.description != null) {
            s.append("<dc:description>").append(escape(meta.description)).append("</dc:description>\n");
        }
        if (meta.date != null) {
            s.append("<dc:date>").append(escape(meta.date)).append("</dc:date>\n");
        }
        if (meta.subject != null) {
            s.append("<dc:subject>").append(escape(meta.subject)).append("</dc:subject>\n");
        }
        if (book.coverItem != null) {
            s.append("<meta name=\"cover\" content=\"").append(book.ids.get(book.coverItem)).append("\"/>\n");
        }
        s.append("</metadata>\n<manifest>\n");
        s.append("<item id=\"ncx\" href=\"toc.ncx\" media-type=\"application/x-dtbncx+xml\"/>\n");
        for (LitFile.Item item : book.lit.getItems()) {
            s.append("<item id=\"").append(book.ids.get(item)).append("\" href=\"").append(escape(LitFile.quoteHref(item.path)))
             .append("\" media-type=\"").append(mediaType(item)).append("\"/>\n");
        }
        s.append("</manifest>\n<spine toc=\"ncx\">\n");
        for (LitFile.Item item : book.spine) {
            s.append("<itemref idref=\"").append(book.ids.get(item)).append("\"/>\n");
        }
        s.append("</spine>\n");
        if (book.coverItem != null) {
            s.append("<guide><reference type=\"cover\" title=\"Cover\" href=\"").append(escape(LitFile.quoteHref(book.coverItem.path))).append("\"/></guide>\n");
        }
        s.append("</package>\n");
        return s.toString();
    }

    private static String ncx(Book book, List<NavPoint> toc) {
        final StringBuilder s = new StringBuilder();
        s.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        s.append("<ncx xmlns=\"http://www.daisy.org/z3986/2005/ncx/\" version=\"2005-1\">\n");
        s.append("<head><meta name=\"dtb:uid\" content=\"bookid\"/><meta name=\"dtb:depth\" content=\"1\"/>"
                + "<meta name=\"dtb:totalPageCount\" content=\"0\"/><meta name=\"dtb:maxPageNumber\" content=\"0\"/></head>\n");
        s.append("<docTitle><text>").append(escape(book.meta.title != null ? book.meta.title : "")).append("</text></docTitle>\n<navMap>\n");
        int order = 0;
        for (NavPoint p : toc) {
            order++;
            s.append("<navPoint id=\"nav").append(order).append("\" playOrder=\"").append(order).append("\"><navLabel><text>")
             .append(escape(p.label)).append("</text></navLabel><content src=\"").append(escape(LitFile.quoteHref(p.src))).append("\"/></navPoint>\n");
        }
        s.append("</navMap>\n</ncx>\n");
        return s.toString();
    }
}
