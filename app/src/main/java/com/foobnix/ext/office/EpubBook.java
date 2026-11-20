package com.foobnix.ext.office;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * An EPUB put together from what a document converter hands it: blocks of XHTML, headings,
 * images and footnotes. It is what the office formats - {@code .docx}, {@code .doc}, {@code .rtf} -
 * are turned into, so that they open as any other book does, with either engine.
 *
 * A heading of the first or second level starts a chapter of its own and is an entry of the table
 * of contents, which therefore never points into the middle of a file (an entry with a #fragment is
 * one foliate has to measure). A document without headings is still cut into files of a readable
 * size, at a block boundary.
 *
 * The XHTML has to be well formed - foliate parses every section as XML - so all text goes
 * through {@link #escape(String)}, which also leaves out the characters XML does not allow.
 */
public final class EpubBook {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    /** A chapter is cut after a block once it has grown past this many characters. */
    private static final int CHAPTER_CHARS = 200 * 1024;

    private static final String CSS = ""
            + "body { margin: 0; }\n"
            + "p { margin: 0 0 0.3em 0; }\n"
            + ".center { text-align: center; }\n"
            + ".right { text-align: right; }\n"
            + ".justify { text-align: justify; }\n"
            + "img { max-width: 100%; height: auto; }\n"
            + "table { border-collapse: collapse; margin: 0.5em 0; }\n"
            + "td, th { border: 1px solid #888; padding: 0.2em 0.4em; vertical-align: top; }\n"
            + "td p, th p { margin: 0; }\n"
            + "sup a { text-decoration: none; }\n"
            + ".notes p { margin: 0 0 0.5em 0; }\n";

    private static final String NOTES = "notes.xhtml";

    private static final class Chapter {
        final String file;
        final StringBuilder body = new StringBuilder();
        String title;

        Chapter(String file) {
            this.file = file;
        }
    }

    private static final class Image {
        final String href;
        final String mime;
        final byte[] data;

        Image(String href, String mime, byte[] data) {
            this.href = href;
            this.mime = mime;
            this.data = data;
        }
    }

    private final List<Chapter> chapters = new ArrayList<Chapter>();
    private final List<Image> images = new ArrayList<Image>();
    private final Map<Integer, List<Image>> imagesByHash = new HashMap<Integer, List<Image>>();
    private final StringBuilder notes = new StringBuilder();
    private int noteCount;

    private String title;
    private String author;
    private String language;

    public EpubBook() {
        newChapter();
    }

    public void setTitle(String title) {
        this.title = clean(title);
    }

    public void setAuthor(String author) {
        this.author = clean(author);
    }

    public void setLanguage(String language) {
        this.language = clean(language);
    }

    public String getTitle() {
        return title;
    }

    /** Whether nothing has been written into the book yet. */
    public boolean isEmpty() {
        for (Chapter chapter : chapters) {
            if (chapter.body.length() > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * A heading. Levels outside 1..6 are kept within them.
     *
     * @param inline the heading's content, already XHTML
     * @param text   the same as plain text, for the table of contents
     */
    public void heading(int level, String inline, String text) {
        level = Math.max(1, Math.min(6, level));
        final String plain = clean(text);
        if (level <= 2 && plain.length() > 0) {
            Chapter current = current();
            if (current.body.length() > 0 || current.title != null) {
                current = newChapter();
            }
            current.title = plain;
        }
        current().body.append("<h").append(level).append('>').append(inline).append("</h").append(level).append(">\n");
    }

    /** A paragraph, a table, a list - any block, already XHTML. */
    public void block(String xhtml) {
        if (xhtml == null || xhtml.length() == 0) {
            return;
        }
        Chapter current = current();
        current.body.append(xhtml).append('\n');
        if (current.body.length() > CHAPTER_CHARS) {
            newChapter();
        }
    }

    /**
     * An image, kept once however many times the document shows it.
     *
     * @return where the chapters find it, or null when it is not a picture a reader can show
     */
    public String image(byte[] data, String nameOrType) {
        if (data == null || data.length == 0) {
            return null;
        }
        final String extension = imageExtension(data, nameOrType);
        if (extension == null) {
            return null;
        }
        final int hash = Arrays.hashCode(data);
        List<Image> same = imagesByHash.get(hash);
        if (same != null) {
            for (Image image : same) {
                if (Arrays.equals(image.data, data)) {
                    return image.href;
                }
            }
        } else {
            same = new ArrayList<Image>();
            imagesByHash.put(hash, same);
        }
        final String href = "images/image" + (images.size() + 1) + "." + extension;
        final Image image = new Image(href, mime(extension), data);
        images.add(image);
        same.add(image);
        return href;
    }

    /**
     * A footnote. Its text goes to the notes at the end of the book and the reference to it is
     * returned, to be put where the note is called: "[n]" raised, the form Librera's own notes
     * code looks for.
     *
     * @param content the note's content, already XHTML blocks
     */
    public String footnote(String content) {
        final int n = ++noteCount;
        notes.append("<div id=\"note").append(n).append("\"><p><a href=\"#ref").append(n)
             .append("\">[").append(n).append("]</a></p>\n").append(content == null ? "" : content).append("</div>\n");
        return "<sup><a id=\"ref" + n + "\" href=\"" + NOTES + "#note" + n + "\">[" + n + "]</a></sup>";
    }

    /** Writes the book. */
    public void write(File file) throws IOException {
        final OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        final ZipOutputStream zip = new ZipOutputStream(out);
        try {
            stored(zip, "mimetype", "application/epub+zip".getBytes(UTF8));
            text(zip, "META-INF/container.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n"
                    + "<rootfiles><rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/></rootfiles>\n"
                    + "</container>\n");

            final List<Chapter> written = new ArrayList<Chapter>();
            for (Chapter chapter : chapters) {
                if (chapter.body.length() > 0 || chapter.title != null) {
                    written.add(chapter);
                }
            }
            if (written.isEmpty()) {
                written.add(chapters.get(0));
            }
            final String bookTitle = bookTitle(written);

            text(zip, "OEBPS/style.css", CSS);
            for (Chapter chapter : written) {
                text(zip, "OEBPS/" + chapter.file, page(chapter.title != null ? chapter.title : bookTitle, chapter.body));
            }
            if (noteCount > 0) {
                text(zip, "OEBPS/" + NOTES, page(bookTitle, new StringBuilder("<div class=\"notes\">\n").append(notes).append("</div>")));
            }
            for (Image image : images) {
                // already compressed: storing them saves the time for nothing
                stored(zip, "OEBPS/" + image.href, image.data);
            }
            final String id = "urn:uuid:" + UUID.nameUUIDFromBytes((bookTitle + written.size() + images.size()).getBytes(UTF8));
            text(zip, "OEBPS/toc.ncx", ncx(id, bookTitle, written));
            text(zip, "OEBPS/nav.xhtml", nav(bookTitle, written));
            text(zip, "OEBPS/content.opf", opf(id, bookTitle, written));
        } finally {
            zip.close();
        }
    }

    private String bookTitle(List<Chapter> written) {
        if (title != null && title.length() > 0) {
            return title;
        }
        for (Chapter chapter : written) {
            if (chapter.title != null) {
                return chapter.title;
            }
        }
        return "Document";
    }

    private Chapter current() {
        return chapters.get(chapters.size() - 1);
    }

    private Chapter newChapter() {
        final Chapter chapter = new Chapter(String.format(Locale.US, "chapter%04d.xhtml", chapters.size() + 1));
        chapters.add(chapter);
        return chapter;
    }

    private static String page(String title, CharSequence body) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\">\n"
                + "<head><title>" + escape(title) + "</title>"
                + "<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\"/></head>\n"
                + "<body>\n" + body + "</body>\n</html>\n";
    }

    private String opf(String id, String bookTitle, List<Chapter> written) {
        final StringBuilder manifest = new StringBuilder();
        final StringBuilder spine = new StringBuilder();
        manifest.append("<item id=\"ncx\" href=\"toc.ncx\" media-type=\"application/x-dtbncx+xml\"/>\n");
        manifest.append("<item id=\"nav\" href=\"nav.xhtml\" media-type=\"application/xhtml+xml\" properties=\"nav\"/>\n");
        manifest.append("<item id=\"css\" href=\"style.css\" media-type=\"text/css\"/>\n");
        int n = 0;
        for (Chapter chapter : written) {
            n++;
            manifest.append("<item id=\"c").append(n).append("\" href=\"").append(chapter.file)
                    .append("\" media-type=\"application/xhtml+xml\"/>\n");
            spine.append("<itemref idref=\"c").append(n).append("\"/>\n");
        }
        if (noteCount > 0) {
            manifest.append("<item id=\"notes\" href=\"").append(NOTES).append("\" media-type=\"application/xhtml+xml\"/>\n");
            spine.append("<itemref idref=\"notes\" linear=\"no\"/>\n");
        }
        int i = 0;
        for (Image image : images) {
            manifest.append("<item id=\"i").append(++i).append("\" href=\"").append(image.href)
                    .append("\" media-type=\"").append(image.mime).append("\"/>\n");
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<package xmlns=\"http://www.idpf.org/2007/opf\" version=\"3.0\" unique-identifier=\"bookid\">\n"
                + "<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n"
                + "<dc:identifier id=\"bookid\">" + escape(id) + "</dc:identifier>\n"
                + "<dc:title>" + escape(bookTitle) + "</dc:title>\n"
                + (author != null && author.length() > 0 ? "<dc:creator>" + escape(author) + "</dc:creator>\n" : "")
                + "<dc:language>" + escape(language != null && language.length() > 0 ? language : "und") + "</dc:language>\n"
                + "<meta property=\"dcterms:modified\">2000-01-01T00:00:00Z</meta>\n"
                + "</metadata>\n"
                + "<manifest>\n" + manifest + "</manifest>\n"
                + "<spine toc=\"ncx\">\n" + spine + "</spine>\n"
                + "</package>\n";
    }

    private static String ncx(String id, String bookTitle, List<Chapter> written) {
        final StringBuilder points = new StringBuilder();
        int n = 0;
        for (Chapter chapter : written) {
            if (chapter.title == null) {
                continue;
            }
            n++;
            points.append("<navPoint id=\"p").append(n).append("\" playOrder=\"").append(n).append("\">")
                  .append("<navLabel><text>").append(escape(chapter.title)).append("</text></navLabel>")
                  .append("<content src=\"").append(chapter.file).append("\"/></navPoint>\n");
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<ncx xmlns=\"http://www.daisy.org/z3986/2005/ncx/\" version=\"2005-1\">\n"
                + "<head><meta name=\"dtb:uid\" content=\"" + escape(id) + "\"/></head>\n"
                + "<docTitle><text>" + escape(bookTitle) + "</text></docTitle>\n"
                + "<navMap>\n" + points + "</navMap>\n</ncx>\n";
    }

    private static String nav(String bookTitle, List<Chapter> written) {
        final StringBuilder items = new StringBuilder();
        for (Chapter chapter : written) {
            if (chapter.title != null) {
                items.append("<li><a href=\"").append(chapter.file).append("\">").append(escape(chapter.title)).append("</a></li>\n");
            }
        }
        if (items.length() == 0) {
            items.append("<li><a href=\"").append(written.get(0).file).append("\">").append(escape(bookTitle)).append("</a></li>\n");
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:epub=\"http://www.idpf.org/2007/ops\">\n"
                + "<head><title>" + escape(bookTitle) + "</title></head>\n<body>\n"
                + "<nav epub:type=\"toc\" id=\"toc\"><ol>\n" + items + "</ol></nav>\n</body>\n</html>\n";
    }

    private static void text(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(UTF8));
        zip.closeEntry();
    }

    private static void stored(ZipOutputStream zip, String name, byte[] data) throws IOException {
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

    /** The extension of a picture readers show, from its first bytes or else its name or type. */
    static String imageExtension(byte[] data, String nameOrType) {
        if (data.length > 8 && (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G') {
            return "png";
        }
        if (data.length > 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (data.length > 6 && data[0] == 'G' && data[1] == 'I' && data[2] == 'F' && data[3] == '8') {
            return "gif";
        }
        if (data.length > 12 && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') {
            return "webp";
        }
        if (data.length > 2 && data[0] == 'B' && data[1] == 'M') {
            return "bmp";
        }
        if (nameOrType != null) {
            final String lower = nameOrType.toLowerCase(Locale.US);
            if (lower.endsWith("svg") || lower.endsWith("svg+xml")) {
                return "svg";
            }
        }
        // EMF, WMF, TIFF and the like: no engine here draws them
        return null;
    }

    private static String mime(String extension) {
        if ("jpg".equals(extension)) {
            return "image/jpeg";
        }
        if ("svg".equals(extension)) {
            return "image/svg+xml";
        }
        return "image/" + extension;
    }

    private static String clean(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll("\\s+", " ").trim();
    }

    /** Text as XHTML: the markup characters escaped, the ones XML does not allow left out. */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        final StringBuilder out = new StringBuilder(text.length() + 16);
        appendEscaped(out, text);
        return out.toString();
    }

    public static void appendEscaped(StringBuilder out, CharSequence text) {
        final int length = text.length();
        for (int i = 0; i < length; i++) {
            final char c = text.charAt(i);
            switch (c) {
                case '&':
                    out.append("&amp;");
                    break;
                case '<':
                    out.append("&lt;");
                    break;
                case '>':
                    out.append("&gt;");
                    break;
                case '"':
                    out.append("&quot;");
                    break;
                default:
                    if (c == '\t' || c == '\n' || c == '\r' || (c >= 0x20 && c < 0xD800) || (c >= 0xE000 && c < 0xFFFE)) {
                        out.append(c);
                    } else if (Character.isHighSurrogate(c) && i + 1 < length && Character.isLowSurrogate(text.charAt(i + 1))) {
                        out.append(c).append(text.charAt(++i));
                    }
                    // anything else - control characters, a lone surrogate - is not allowed in XML
            }
        }
    }
}
