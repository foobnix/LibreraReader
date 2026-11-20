package com.foobnix.ext.chm;

import com.foobnix.ext.FontSizes;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.DocumentType;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Entities;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.XmlDeclaration;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 * Turns a CHM into an EPUB, which every one of Librera's engines reads: MuPDF where it is in the
 * build and foliate-js everywhere, which only reads books out of an archive.
 * <p>
 * The pages come in the order of the table of contents in the CHM's {@code .hhc} file, which also
 * becomes the EPUB's NCX and navigation document. A CHM without one is read from its default topic
 * on, page after page in the order its links lead to them. Every page is rewritten as well-formed
 * XHTML - help files are old, loose HTML, and foliate parses a section as XML, where an
 * {@code &nbsp;} or an {@code <o:p>} would blank it - and every page, picture and stylesheet gets
 * a flat name of its own, with the links between them rewritten to match; CHM names ignore case
 * and may be written as {@code ms-its:book.chm::/page.htm}.
 */
public final class ChmToEpub {

    private static final String OEBPS = "OEBPS/";
    private static final String XHTML_NS = "http://www.w3.org/1999/xhtml";
    private static final Charset UTF8 = Charset.forName("UTF-8");

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
        MIME.put("css", "text/css");
        MIME.put("ttf", "font/ttf");
        MIME.put("otf", "font/otf");
        MIME.put("woff", "font/woff");
        MIME.put("woff2", "font/woff2");
    }

    private static final Pattern CSS_URL = Pattern.compile(
            "(?i)url\\(\\s*([\"']?)([^\"')]+)\\1\\s*\\)");
    private static final Pattern CSS_IMPORT = Pattern.compile(
            "(?i)@import\\s+([\"'])([^\"']+)\\1");
    private static final Pattern HREF = Pattern.compile(
            "(?i)<a\\s[^>]*?href\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))");
    private static final Pattern INVALID_XML = Pattern.compile(
            "[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\uFFFE\\uFFFF]");
    private static final Pattern SCHEME = Pattern.compile("^[A-Za-z][A-Za-z0-9+.\\-]*:");
    private static final Pattern NCNAME = Pattern.compile("^[A-Za-z_][A-Za-z0-9_.\\-]*$");

    /** One line of the table of contents. */
    public static final class TocEntry {
        public final String title;
        /** The CHM page it opens, lower-cased, or null for a heading that opens nothing. */
        String page;
        String fragment;
        /** What it points at in the EPUB, once the pages have their new names. */
        public String href;
        public final List<TocEntry> children = new ArrayList<TocEntry>();

        TocEntry(final String title) {
            this.title = title;
        }
    }

    /** What was written, for the caller and the tests. */
    public static final class Result {
        public String title;
        /** The book's body text size in CSS pixels, which was made the reader's 1rem. */
        public float basePx;
        public int pages;
        public int resources;
        public final List<TocEntry> toc = new ArrayList<TocEntry>();
    }

    private final ChmFile chm;
    private final String fallbackTitle;

    /** Lower-cased CHM name to the entry, for the files that go into the book. */
    private final Map<String, ChmFile.Entry> files = new LinkedHashMap<String, ChmFile.Entry>();
    /** Lower-cased CHM name to its name in the EPUB. */
    private final Map<String, String> names = new HashMap<String, String>();
    private final Map<String, String> pageTitles = new HashMap<String, String>();
    private final Set<String> usedIds = new HashSet<String>();

    /** The book's absolute font sizes, made relative to its body text. */
    private final FontSizes sizes = new FontSizes();

    private ChmToEpub(final ChmFile chm, final String fallbackTitle) {
        this.chm = chm;
        this.fallbackTitle = fallbackTitle;
    }

    /**
     * Converts {@code chmFile} into {@code epub}, which is written to a temporary file beside it
     * and only takes its name once it is complete.
     */
    public static Result convert(final File chmFile, final File epub) throws IOException {
        final ChmFile chm = new ChmFile(chmFile);
        try {
            String name = chmFile.getName();
            final int dot = name.lastIndexOf('.');
            if (dot > 0) {
                name = name.substring(0, dot);
            }
            final ChmToEpub converter = new ChmToEpub(chm, name);
            final File temp = new File(epub.getPath() + ".tmp");
            try {
                final Result result = converter.write(temp);
                if (epub.exists() && !epub.delete()) {
                    throw new IOException("cannot replace " + epub);
                }
                if (!temp.renameTo(epub)) {
                    throw new IOException("cannot rename " + temp + " to " + epub);
                }
                return result;
            } finally {
                temp.delete();
            }
        } finally {
            chm.close();
        }
    }

    /**
     * The title and the language of the book, from its #SYSTEM file, read without decompressing
     * anything: either may be null.
     */
    public static String[] getTitleAndLanguage(final File chmFile) {
        try {
            final ChmFile chm = new ChmFile(chmFile);
            try {
                return new String[]{chm.getTitle(), chm.getLcid() != 0 ? language(chm.getLcid()) : null};
            } finally {
                chm.close();
            }
        } catch (Exception e) {
            return new String[]{null, null};
        }
    }

    // ------------------------------------------------------------------ the book

    private Result write(final File out) throws IOException {
        final List<String> pages = new ArrayList<String>();
        final List<String> resources = new ArrayList<String>();
        for (ChmFile.Entry e : chm.getEntries()) {
            final String n = e.name;
            if (!n.startsWith("/") || n.endsWith("/") || n.startsWith("/#") || n.startsWith("/$")) {
                continue;
            }
            final String key = n.toLowerCase(Locale.US);
            final String ext = ext(key);
            if (isPage(ext)) {
                files.put(key, e);
                pages.add(key);
            } else if (MIME.containsKey(ext)) {
                files.put(key, e);
                resources.add(key);
            }
        }

        final Result result = new Result();
        final List<TocEntry> toc = readToc();
        final List<String> spine = spine(toc, pages);
        if (spine.isEmpty()) {
            throw new IOException("CHM has no pages");
        }

        int n = 1;
        for (String page : spine) {
            names.put(page, String.format(Locale.US, "p%04d.xhtml", n++));
        }
        n = 1;
        for (String res : resources) {
            names.put(res, String.format(Locale.US, "r%04d.%s", n++, ext(res)));
        }

        if (toc.isEmpty()) {
            toc.addAll(linksOf(spine.get(0), spine));
        }
        resolveToc(toc, spine);
        result.toc.addAll(toc);

        String title = chm.getTitle();
        if (title == null || title.length() == 0) {
            title = fallbackTitle;
        }
        result.title = title;

        // The stylesheets are read before anything is written: the size most of the text is set
        // in has to be known before any size can be made relative to it
        final Map<String, String> stylesheets = new HashMap<String, String>();
        for (String res : resources) {
            if ("css".equals(ext(res))) {
                try {
                    final String css = ChmText.decode(chm.read(files.get(res)), chm.getLcid());
                    stylesheets.put(res, css);
                    sizes.weighStylesheet(css);
                } catch (IOException ex) {
                    // Left out below as well
                }
            }
        }
        result.basePx = sizes.pickBase();

        final ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(out));
        try {
            stored(zip, "mimetype", "application/epub+zip".getBytes(UTF8));
            put(zip, "META-INF/container.xml", ("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                    + "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">"
                    + "<rootfiles><rootfile full-path=\"" + OEBPS + "content.opf\""
                    + " media-type=\"application/oebps-package+xml\"/></rootfiles></container>").getBytes(UTF8));

            final List<String> writtenResources = new ArrayList<String>();
            for (String res : resources) {
                final ChmFile.Entry e = files.get(res);
                byte[] data;
                if ("css".equals(ext(res))) {
                    if (!stylesheets.containsKey(res)) {
                        continue;
                    }
                    data = css(stylesheets.get(res), res).getBytes(UTF8);
                } else {
                    try {
                        data = chm.read(e);
                    } catch (IOException ex) {
                        // One broken picture is not worth the whole book
                        continue;
                    }
                }
                put(zip, OEBPS + names.get(res), data);
                writtenResources.add(res);
            }

            for (String page : spine) {
                byte[] html;
                try {
                    html = chm.read(files.get(page));
                } catch (IOException ex) {
                    // The page stays in the spine, so the links and the contents still find it
                    html = new byte[0];
                }
                put(zip, OEBPS + names.get(page), page(html, page).getBytes(UTF8));
            }

            put(zip, OEBPS + "toc.ncx", ncx(title, toc).getBytes(UTF8));
            put(zip, OEBPS + "nav.xhtml", nav(title, toc).getBytes(UTF8));
            put(zip, OEBPS + "content.opf", opf(title, spine, writtenResources).getBytes(UTF8));

            result.pages = spine.size();
            result.resources = writtenResources.size();
        } finally {
            zip.close();
        }
        return result;
    }

    private static boolean isPage(final String ext) {
        return ext.equals("htm") || ext.equals("html") || ext.equals("xhtml") || ext.equals("xhtm")
                || ext.equals("shtml") || ext.equals("stm");
    }

    private static String ext(final String name) {
        final int slash = name.lastIndexOf('/');
        final int dot = name.lastIndexOf('.');
        return dot > slash ? name.substring(dot + 1).toLowerCase(Locale.US) : "";
    }

    // ------------------------------------------------------------------ table of contents

    private List<TocEntry> readToc() {
        final List<TocEntry> roots = new ArrayList<TocEntry>();
        final String hhc = chm.getContentsFile();
        if (hhc == null) {
            return roots;
        }
        final Document doc;
        try {
            doc = Jsoup.parse(ChmText.decode(chm.read(chm.find(hhc)), chm.getLcid()));
        } catch (Exception e) {
            return roots;
        }
        // Each entry is an <object type="text/sitemap">, nested as deep as the lists around it
        final List<TocEntry> stack = new ArrayList<TocEntry>();
        final List<Integer> depths = new ArrayList<Integer>();
        for (Element object : doc.getElementsByTag("object")) {
            if (!"text/sitemap".equalsIgnoreCase(object.attr("type").trim())) {
                continue;
            }
            String name = null;
            String local = null;
            for (Element param : object.getElementsByTag("param")) {
                final String key = param.attr("name").trim();
                if (name == null && key.equalsIgnoreCase("Name")) {
                    name = param.attr("value").trim();
                } else if (local == null && key.equalsIgnoreCase("Local")) {
                    local = param.attr("value").trim();
                }
            }
            if (name == null || name.length() == 0) {
                if (local == null) {
                    continue;
                }
                name = local;
            }
            final TocEntry entry = new TocEntry(name);
            if (local != null && local.length() > 0) {
                final String[] target = target(local, hhc);
                if (target != null && files.containsKey(target[0]) && isPage(ext(target[0]))) {
                    entry.page = target[0];
                    entry.fragment = target[1];
                }
            }
            int depth = 0;
            for (Element p = object.parent(); p != null; p = p.parent()) {
                if (p.tagName().equals("ul") || p.tagName().equals("ol")) {
                    depth++;
                }
            }
            while (!depths.isEmpty() && depths.get(depths.size() - 1) >= depth) {
                depths.remove(depths.size() - 1);
                stack.remove(stack.size() - 1);
            }
            if (stack.isEmpty()) {
                roots.add(entry);
            } else {
                stack.get(stack.size() - 1).children.add(entry);
            }
            stack.add(entry);
            depths.add(depth);
        }
        return roots;
    }

    /** Headings that open nothing open their first page; the ones under which there is none go. */
    private void resolveToc(final List<TocEntry> list, final List<String> spine) {
        for (int i = 0; i < list.size(); i++) {
            final TocEntry e = list.get(i);
            resolveToc(e.children, spine);
            if (e.page == null || !names.containsKey(e.page)) {
                final TocEntry first = firstWithPage(e.children);
                if (first != null) {
                    e.page = first.page;
                    e.fragment = first.fragment;
                } else {
                    e.page = null;
                }
            }
            if (e.page == null) {
                list.remove(i);
                list.addAll(i, e.children);
                i--;
                continue;
            }
            e.href = names.get(e.page) + (e.fragment != null ? "#" + e.fragment : "");
        }
    }

    private static TocEntry firstWithPage(final List<TocEntry> list) {
        for (TocEntry e : list) {
            if (e.page != null) {
                return e;
            }
            final TocEntry f = firstWithPage(e.children);
            if (f != null) {
                return f;
            }
        }
        return null;
    }

    private static void pagesOf(final List<TocEntry> list, final Set<String> out) {
        for (TocEntry e : list) {
            if (e.page != null) {
                out.add(e.page);
            }
            pagesOf(e.children, out);
        }
    }

    /** A book with no .hhc: its contents are the links of its first page. */
    private List<TocEntry> linksOf(final String page, final List<String> spine) {
        final List<TocEntry> list = new ArrayList<TocEntry>();
        final Set<String> seen = new HashSet<String>();
        try {
            final Document doc = Jsoup.parse(ChmText.decode(chm.read(files.get(page)), chm.getLcid()));
            for (Element a : doc.getElementsByTag("a")) {
                final String text = a.text().trim();
                final String[] target = target(a.attr("href"), page);
                if (text.length() == 0 || target == null || !names.containsKey(target[0])
                        || !isPage(ext(target[0])) || target[0].equals(page)) {
                    continue;
                }
                final String key = target[0] + "#" + target[1];
                if (!seen.add(key)) {
                    continue;
                }
                final TocEntry e = new TocEntry(text);
                e.page = target[0];
                e.fragment = target[1];
                list.add(e);
            }
        } catch (Exception e) {
            list.clear();
        }
        if (list.size() < 2) {
            list.clear();
            for (String p : spine) {
                final String t = pageTitles.get(p);
                final TocEntry e = new TocEntry(t != null && t.length() > 0 ? t : nameOf(p));
                e.page = p;
                list.add(e);
            }
        }
        return list;
    }

    private static String nameOf(final String key) {
        String n = key.substring(key.lastIndexOf('/') + 1);
        final int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    // ------------------------------------------------------------------ reading order

    /**
     * The pages in the order of the table of contents, the default topic first when it is not in
     * it; then the pages they link to, in the order the links lead to them; then whatever no link
     * reaches, by name.
     */
    private List<String> spine(final List<TocEntry> toc, final List<String> pages) {
        final LinkedHashSet<String> order = new LinkedHashSet<String>();
        final String start = chm.getDefaultTopic();
        final Set<String> listed = new LinkedHashSet<String>();
        pagesOf(toc, listed);

        if (!listed.isEmpty()) {
            if (start != null && files.containsKey(start.toLowerCase(Locale.US))) {
                order.add(start.toLowerCase(Locale.US));
            }
            order.addAll(listed);
            // The pages only links reach, after the listed ones
            final List<String> queue = new ArrayList<String>(order);
            for (int i = 0; i < queue.size(); i++) {
                for (String link : pageLinks(queue.get(i))) {
                    if (order.add(link)) {
                        queue.add(link);
                    }
                }
            }
        } else {
            String first = start != null ? start.toLowerCase(Locale.US) : null;
            if (first == null || !files.containsKey(first)) {
                first = null;
                for (String p : sortedByName(pages)) {
                    final String base = nameOf(p);
                    if (base.equals("index") || base.equals("default") || base.equals("main")
                            || base.equals("toc") || base.equals("contents")) {
                        first = p;
                        break;
                    }
                }
            }
            if (first != null) {
                // Breadth first, in link order: a page of chapters reads chapter by chapter, and a
                // chain of "next" links page by page
                order.add(first);
                final List<String> queue = new ArrayList<String>(order);
                for (int i = 0; i < queue.size(); i++) {
                    for (String link : pageLinks(queue.get(i))) {
                        if (order.add(link)) {
                            queue.add(link);
                        }
                    }
                }
            }
        }
        for (String p : sortedByName(pages)) {
            order.add(p);
        }
        return new ArrayList<String>(order);
    }

    /** The pages of the book this one links to, in the order of its links. */
    private List<String> pageLinks(final String page) {
        final List<String> out = new ArrayList<String>();
        final ChmFile.Entry e = files.get(page);
        if (e == null) {
            return out;
        }
        final String text;
        try {
            text = ChmText.decode(chm.read(e), chm.getLcid());
        } catch (IOException ex) {
            return out;
        }
        final Matcher t = Pattern.compile("(?is)<title[^>]*>(.*?)</title>").matcher(text);
        if (t.find()) {
            pageTitles.put(page, Jsoup.parse(t.group(1)).text().trim());
        }
        sizes.weighPage(text);
        final Matcher m = HREF.matcher(text);
        final Set<String> seen = new HashSet<String>();
        while (m.find()) {
            String href = m.group(1) != null ? m.group(1) : m.group(2) != null ? m.group(2) : m.group(3);
            href = Entities.unescape(href);
            final String[] target = target(href, page);
            if (target != null && files.containsKey(target[0]) && isPage(ext(target[0]))
                    && seen.add(target[0])) {
                out.add(target[0]);
            }
        }
        return out;
    }

    private static List<String> sortedByName(final List<String> pages) {
        final List<String> sorted = new ArrayList<String>(pages);
        Collections.sort(sorted, NATURAL);
        return sorted;
    }

    /** Names compared with their numbers as numbers, so that f2 comes before f10. */
    static final Comparator<String> NATURAL = new Comparator<String>() {
        @Override
        public int compare(final String a, final String b) {
            int i = 0;
            int j = 0;
            while (i < a.length() && j < b.length()) {
                final char x = a.charAt(i);
                final char y = b.charAt(j);
                if (Character.isDigit(x) && Character.isDigit(y)) {
                    int ie = i;
                    while (ie < a.length() && Character.isDigit(a.charAt(ie))) {
                        ie++;
                    }
                    int je = j;
                    while (je < b.length() && Character.isDigit(b.charAt(je))) {
                        je++;
                    }
                    final String na = a.substring(i, ie).replaceFirst("^0+(?=.)", "");
                    final String nb = b.substring(j, je).replaceFirst("^0+(?=.)", "");
                    if (na.length() != nb.length()) {
                        return na.length() - nb.length();
                    }
                    final int c = na.compareTo(nb);
                    if (c != 0) {
                        return c;
                    }
                    i = ie;
                    j = je;
                } else {
                    if (x != y) {
                        return x - y;
                    }
                    i++;
                    j++;
                }
            }
            return (a.length() - i) - (b.length() - j);
        }
    };

    // ------------------------------------------------------------------ references

    /**
     * What a reference in {@code from} points at: the lower-cased CHM name and the fragment, or
     * null for a reference outside the book - another site, a script, a page of a different CHM.
     */
    static String[] target(String ref, final String from) {
        if (ref == null) {
            return null;
        }
        ref = ref.trim().replace('\\', '/');
        if (ref.length() == 0) {
            return null;
        }
        final String lower = ref.toLowerCase(Locale.US);
        if (lower.startsWith("ms-its:") || lower.startsWith("mk:@msitstore:") || lower.startsWith("its:")) {
            final int sep = ref.indexOf("::");
            if (sep < 0) {
                return null;
            }
            ref = ref.substring(sep + 2);
            if (!ref.startsWith("/")) {
                ref = "/" + ref;
            }
        } else if (SCHEME.matcher(ref).find()) {
            return null;
        }
        String fragment = null;
        final int hash = ref.indexOf('#');
        if (hash >= 0) {
            fragment = ref.substring(hash + 1);
            ref = ref.substring(0, hash);
        }
        final int query = ref.indexOf('?');
        if (query >= 0) {
            ref = ref.substring(0, query);
        }
        if (ref.length() == 0) {
            // Within the page itself
            return new String[]{from, fragment};
        }
        if (ref.indexOf('%') >= 0) {
            try {
                ref = URLDecoder.decode(ref.replace("+", "%2B"), "UTF-8");
            } catch (Exception e) {
                // Kept as written
            }
        }
        String path;
        if (ref.startsWith("/")) {
            path = ref;
        } else {
            path = from.substring(0, from.lastIndexOf('/') + 1) + ref;
        }
        // Resolve . and ..; a path that climbs out of the book stays at its root
        final List<String> parts = new ArrayList<String>();
        for (String part : path.split("/")) {
            if (part.length() == 0 || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                if (!parts.isEmpty()) {
                    parts.remove(parts.size() - 1);
                }
                continue;
            }
            parts.add(part);
        }
        final StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            sb.append('/').append(part);
        }
        return new String[]{sb.toString().toLowerCase(Locale.US), fragment};
    }

    /** The reference rewritten for the EPUB, or null when the file is not in it. */
    private String rewrite(final String ref, final String from) {
        final String[] target = target(ref, from);
        if (target == null) {
            return null;
        }
        final String name = names.get(target[0]);
        if (name == null) {
            return null;
        }
        if (target[0].equals(from) && target[1] != null) {
            return "#" + target[1];
        }
        return name + (target[1] != null ? "#" + target[1] : "");
    }

    private static boolean isExternal(final String ref) {
        if (ref == null) {
            return false;
        }
        final String lower = ref.trim().toLowerCase(Locale.US);
        return lower.startsWith("http:") || lower.startsWith("https:") || lower.startsWith("mailto:")
                || lower.startsWith("ftp:") || lower.startsWith("news:");
    }

    private String css(final String text, final String from) {
        final StringBuffer sb = new StringBuffer();
        Matcher m = CSS_URL.matcher(text.replaceFirst("(?i)^\\s*@charset[^;]*;", ""));
        while (m.find()) {
            final String to = rewrite(m.group(2).trim(), from);
            m.appendReplacement(sb, Matcher.quoteReplacement(to != null ? "url(\"" + to + "\")" : "none"));
        }
        m.appendTail(sb);
        final String withUrls = sb.toString();
        final StringBuffer sb2 = new StringBuffer();
        m = CSS_IMPORT.matcher(withUrls);
        while (m.find()) {
            final String to = rewrite(m.group(2).trim(), from);
            m.appendReplacement(sb2, Matcher.quoteReplacement(to != null ? "@import \"" + to + "\"" : "@import \"\""));
        }
        m.appendTail(sb2);
        return sizes.css(sb2.toString()).replace("<!--", "").replace("-->", "");
    }

    // ------------------------------------------------------------------ pages

    private String page(final byte[] bytes, final String from) {
        final String text = INVALID_XML.matcher(ChmText.decode(bytes, chm.getLcid())).replaceAll("");
        final Document doc = Jsoup.parse(text);
        usedIds.clear();

        removeNodes(doc);
        doc.select("script, object, embed, applet, iframe, frame, frameset, param, base, meta, xml").remove();
        doc.select("form, noscript").unwrap();
        for (Element link : doc.select("link")) {
            final String to = "stylesheet".equalsIgnoreCase(link.attr("rel").trim())
                    ? rewrite(link.attr("href"), from) : null;
            if (to == null) {
                link.remove();
            } else {
                link.clearAttributes();
                link.attr("rel", "stylesheet");
                link.attr("type", "text/css");
                link.attr("href", to);
            }
        }

        for (Element e : doc.getAllElements()) {
            if (e == doc) {
                continue;
            }
            if (e.tagName().indexOf(':') >= 0) {
                // Office's <o:p> and the like: an undeclared prefix is an XML error
                e.tagName("span");
                e.clearAttributes();
                continue;
            }
            final List<String> drop = new ArrayList<String>();
            for (Attribute a : e.attributes()) {
                final String key = a.getKey().toLowerCase(Locale.US);
                if ((key.indexOf(':') >= 0 && !key.equals("xml:lang")) || key.startsWith("xmlns")
                        || key.startsWith("on")) {
                    drop.add(a.getKey());
                }
            }
            for (String key : drop) {
                e.removeAttr(key);
            }
            fixReferences(e, from);
            if (e.tagName().equals("font") && e.hasAttr("size")) {
                final float px = FontSizes.fontTagPx(e.attr("size"));
                e.removeAttr("size");
                if (px > 0) {
                    final String style = e.attr("style");
                    e.attr("style", (style.length() > 0 ? style + ";" : "") + "font-size:" + sizes.rem(px));
                }
            }
            if (e.hasAttr("id")) {
                final String id = e.attr("id");
                if (!usedIds.add(id)) {
                    e.removeAttr("id");
                }
            }
        }
        // <a name="x"> is what a CHM's #x links land on; an XHTML document finds ids
        for (Element a : doc.select("a[name]")) {
            final String name = a.attr("name").trim();
            if (!a.hasAttr("id") && NCNAME.matcher(name).matches() && usedIds.add(name)) {
                a.attr("id", name);
            }
            a.removeAttr("name");
        }
        for (Element style : doc.getElementsByTag("style")) {
            final String css = css(style.data(), from);
            style.empty();
            style.appendChild(new DataNode(css));
            style.clearAttributes();
            style.attr("type", "text/css");
        }

        final Element html = doc.child(0);
        final String lang = html.attr("lang");
        final String dir = html.attr("dir");
        html.clearAttributes();
        html.attr("xmlns", XHTML_NS);
        if (lang.length() > 0) {
            html.attr("lang", lang);
            html.attr("xml:lang", lang);
        }
        if (dir.length() > 0) {
            html.attr("dir", dir);
        }
        final Element head = doc.head();
        if (head.getElementsByTag("title").isEmpty()) {
            head.prependElement("title");
        }
        final Element title = head.getElementsByTag("title").first();
        if (title.text().trim().length() == 0) {
            final String t = pageTitles.get(from);
            title.text(t != null && t.length() > 0 ? t : nameOf(from));
        }

        doc.outputSettings()
                .syntax(Document.OutputSettings.Syntax.xml)
                .escapeMode(Entities.EscapeMode.xhtml)
                .charset("UTF-8")
                .prettyPrint(false);
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" + html.outerHtml();
    }

    /** Comments may hold "--", which XML does not allow, and a doctype is written again. */
    private static void removeNodes(final Node node) {
        for (int i = node.childNodeSize() - 1; i >= 0; i--) {
            final Node child = node.childNode(i);
            if (child instanceof Comment || child instanceof DocumentType || child instanceof XmlDeclaration) {
                child.remove();
            } else {
                removeNodes(child);
            }
        }
    }

    private void fixReferences(final Element e, final String from) {
        final String tag = e.tagName();
        if (e.hasAttr("href") && (tag.equals("a") || tag.equals("area"))) {
            final String href = e.attr("href");
            if (href.trim().startsWith("#")) {
                // Kept: a fragment of this page
            } else if (isExternal(href)) {
                e.attr("href", href.trim());
            } else {
                final String to = rewrite(href, from);
                if (to != null) {
                    e.attr("href", to);
                } else {
                    e.removeAttr("href");
                }
            }
            e.removeAttr("target");
        }
        if (e.hasAttr("src")) {
            final String src = e.attr("src");
            final String to = isExternal(src) ? null : rewrite(src, from);
            if (to != null) {
                e.attr("src", to);
            } else if (tag.equals("img")) {
                // A picture the book does not have would only be a broken frame; its text stays
                final String alt = e.attr("alt").trim();
                if (alt.length() > 0) {
                    e.after(new org.jsoup.nodes.TextNode(alt));
                }
                e.remove();
                return;
            } else {
                e.removeAttr("src");
            }
        }
        if (e.hasAttr("background")) {
            final String to = rewrite(e.attr("background"), from);
            e.removeAttr("background");
            if (to != null) {
                final String style = e.attr("style");
                e.attr("style", (style.length() > 0 ? style + ";" : "") + "background-image:url(\"" + to + "\")");
            }
        }
        if (e.hasAttr("style")) {
            e.attr("style", css(e.attr("style"), from).replace('"', '\''));
        }
    }

    // ------------------------------------------------------------------ package

    private String opf(final String title, final List<String> spine, final List<String> resources) {
        final StringBuilder s = new StringBuilder();
        s.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        s.append("<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" unique-identifier=\"uid\">\n");
        s.append("<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n");
        s.append("<dc:identifier id=\"uid\">urn:librera:chm:").append(Integer.toHexString((title + spine.size()).hashCode())).append("</dc:identifier>\n");
        s.append("<dc:title>").append(xml(title)).append("</dc:title>\n");
        s.append("<dc:language>").append(language(chm.getLcid())).append("</dc:language>\n");
        s.append("<meta property=\"dcterms:modified\">2000-01-01T00:00:00Z</meta>\n");
        s.append("</metadata>\n<manifest>\n");
        s.append("<item id=\"ncx\" href=\"toc.ncx\" media-type=\"application/x-dtbncx+xml\"/>\n");
        s.append("<item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>\n");
        int i = 1;
        for (String p : spine) {
            s.append("<item id=\"p").append(i++).append("\" href=\"").append(names.get(p))
                    .append("\" media-type=\"application/xhtml+xml\"/>\n");
        }
        i = 1;
        for (String r : resources) {
            s.append("<item id=\"r").append(i++).append("\" href=\"").append(names.get(r))
                    .append("\" media-type=\"").append(MIME.get(ext(r))).append("\"/>\n");
        }
        s.append("</manifest>\n<spine toc=\"ncx\">\n");
        for (i = 1; i <= spine.size(); i++) {
            s.append("<itemref idref=\"p").append(i).append("\"/>\n");
        }
        s.append("</spine>\n</package>\n");
        return s.toString();
    }

    private String ncx(final String title, final List<TocEntry> toc) {
        final StringBuilder s = new StringBuilder();
        s.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        s.append("<ncx xmlns=\"http://www.daisy.org/z3986/2005/ncx/\" version=\"2005-1\">\n");
        s.append("<head><meta name=\"dtb:depth\" content=\"").append(Math.max(1, depth(toc))).append("\"/></head>\n");
        s.append("<docTitle><text>").append(xml(title)).append("</text></docTitle>\n<navMap>\n");
        ncxPoints(s, toc, new int[]{1});
        s.append("</navMap>\n</ncx>\n");
        return s.toString();
    }

    private static void ncxPoints(final StringBuilder s, final List<TocEntry> list, final int[] order) {
        for (TocEntry e : list) {
            final int n = order[0]++;
            s.append("<navPoint id=\"n").append(n).append("\" playOrder=\"").append(n).append("\">");
            s.append("<navLabel><text>").append(xml(e.title)).append("</text></navLabel>");
            s.append("<content src=\"").append(xml(e.href)).append("\"/>\n");
            ncxPoints(s, e.children, order);
            s.append("</navPoint>\n");
        }
    }

    private String nav(final String title, final List<TocEntry> toc) {
        final StringBuilder s = new StringBuilder();
        s.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        s.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\">");
        s.append("<head><title>").append(xml(title)).append("</title></head><body>");
        s.append("<nav epub:type=\"toc\" id=\"toc\"><h1>").append(xml(title)).append("</h1>");
        if (toc.isEmpty()) {
            // An EPUB 3 navigation document needs one entry at least
            s.append("<ol><li><a href=\"p0001.xhtml\">").append(xml(title)).append("</a></li></ol>");
        } else {
            navList(s, toc);
        }
        s.append("</nav></body></html>\n");
        return s.toString();
    }

    private static void navList(final StringBuilder s, final List<TocEntry> list) {
        s.append("<ol>");
        for (TocEntry e : list) {
            s.append("<li><a href=\"").append(xml(e.href)).append("\">").append(xml(e.title)).append("</a>");
            if (!e.children.isEmpty()) {
                navList(s, e.children);
            }
            s.append("</li>");
        }
        s.append("</ol>");
    }

    private static int depth(final List<TocEntry> list) {
        int d = 0;
        for (TocEntry e : list) {
            d = Math.max(d, 1 + depth(e.children));
        }
        return d;
    }

    private static String language(final int lcid) {
        switch (lcid & 0x3FF) {
            case 0x19: return "ru";
            case 0x22: return "uk";
            case 0x23: return "be";
            case 0x02: return "bg";
            case 0x07: return "de";
            case 0x0C: return "fr";
            case 0x0A: return "es";
            case 0x10: return "it";
            case 0x15: return "pl";
            case 0x05: return "cs";
            case 0x16: return "pt";
            case 0x13: return "nl";
            case 0x1D: return "sv";
            case 0x06: return "da";
            case 0x14: return "no";
            case 0x0B: return "fi";
            case 0x0E: return "hu";
            case 0x1F: return "tr";
            case 0x08: return "el";
            case 0x0D: return "he";
            case 0x01: return "ar";
            case 0x11: return "ja";
            case 0x12: return "ko";
            case 0x04: return (lcid == 0x0804 || lcid == 0x1004) ? "zh-CN" : "zh-TW";
            default: return "en";
        }
    }

    private static String xml(final String s) {
        if (s == null) {
            return "";
        }
        return INVALID_XML.matcher(s).replaceAll("").replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    // ------------------------------------------------------------------ zip

    private static void stored(final ZipOutputStream zip, final String name, final byte[] data) throws IOException {
        final ZipEntry entry = new ZipEntry(name);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(data.length);
        entry.setCompressedSize(data.length);
        final CRC32 crc = new CRC32();
        crc.update(data);
        entry.setCrc(crc.getValue());
        zip.putNextEntry(entry);
        zip.write(data);
        zip.closeEntry();
    }

    private static void put(final ZipOutputStream zip, final String name, final byte[] data) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data);
        zip.closeEntry();
    }
}
