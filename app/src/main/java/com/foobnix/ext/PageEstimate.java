package com.foobnix.ext;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * How many printed pages a book with no pages of its own would fill.
 * <p>
 * A reflowable book is as many pages as its font and screen make it, so it is measured in
 * standard pages instead: {@link #CHARS_PER_PAGE} characters with the spaces, the page
 * publishers count by - some 300 words of English, 250 of Russian.
 * <p>
 * The text is never read, only sized, so a library of thousands of books is measured in moments:
 * the uncompressed size of an EPUB's chapters from the zip's directory, the length a MOBI's header
 * gives its text, the characters a Word document's header counts, the size of the file. A byte
 * of that is not a character: markup takes its share, which depends on the format, and a letter
 * of Russian or Greek takes two bytes in UTF-8, of Hindi or Thai three - which the language of
 * the book tells. Chinese and Japanese take three bytes too, but a character of theirs fills as
 * much of a page as three letters do, so they count as written.
 * <p>
 * Measured against the characters counted one by one in some 300 books, it comes within a
 * quarter of them for most. Pure Java, so it runs in the scan and in unit tests alike. Nothing is
 * thrown: a book that cannot be measured is 0.
 */
public class PageEstimate {

    public static final int CHARS_PER_PAGE = 1800;

    /** The extensions of the formats whose pages are estimated. */
    public static final Set<String> FORMATS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(//
            "epub", "fb2", "txt", "md", "html", "htm", "xhtml", "docx", "odt", "doc", "rtf", "mobi", "azw", "azw3")));

    /** Languages written in a script UTF-8 gives two bytes a letter. */
    private static final Set<String> TWO_BYTES = new HashSet<>(Arrays.asList(//
            "ru", "uk", "be", "bg", "sr", "mk", "kk", "ky", "tg", "mn", "el", "he", "iw", "yi", "ar", "fa", "ur", "hy", "ps", "sd", "ug"));

    /** Languages written in a script UTF-8 gives three bytes a letter, and that is not Chinese or Japanese. */
    private static final Set<String> THREE_BYTES = new HashSet<>(Arrays.asList(//
            "hi", "mr", "ne", "bn", "ta", "te", "kn", "ml", "gu", "pa", "or", "si", "th", "lo", "km", "my", "ka", "am", "bo"));

    /** Whether the pages of a book at this path are estimated, rather than counted by its engine. */
    public static boolean isEstimated(String path) {
        if (path == null) {
            return false;
        }
        String name = path.toLowerCase(Locale.US);
        if (name.endsWith(".zip")) {
            // A book zipped on its own, book.fb2.zip, is read out of the zip
            name = name.substring(0, name.length() - 4);
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 && FORMATS.contains(name.substring(dot + 1));
    }

    /**
     * {@link #isEstimated(String)}, or a zip of any name whose book - child, the extension the
     * library keeps of what it found inside - is in a format whose pages are estimated.
     */
    public static boolean isEstimated(String path, String child) {
        if (isEstimated(path)) {
            return true;
        }
        return path != null && child != null && path.toLowerCase(Locale.US).endsWith(".zip")
                && FORMATS.contains(child.toLowerCase(Locale.US));
    }

    /**
     * The standard pages of the book, or 0 when its format is not estimated or it cannot be read.
     *
     * @param lang   the language of the book, as its metadata gives it, or null
     * @param sample text of the book's own - its title and author - to tell the script by when
     *               there is no language
     */
    public static int of(String path, String lang, String sample) {
        try {
            return pages(chars(path, bytesPerChar(lang, sample)));
        } catch (Throwable e) {
            return 0;
        }
    }

    public static int pages(long chars) {
        if (chars <= 0) {
            return 0;
        }
        return (int) Math.max(1, (chars + CHARS_PER_PAGE - 1) / CHARS_PER_PAGE);
    }

    /**
     * The bytes of UTF-8 a character of the book's text takes, on average with the spaces and
     * punctuation, which take one.
     */
    static double bytesPerChar(String lang, String sample) {
        if (lang != null && lang.length() >= 2) {
            String code = lang.toLowerCase(Locale.US).substring(0, 2);
            if (TWO_BYTES.contains(code)) {
                return 1.8;
            }
            if (THREE_BYTES.contains(code)) {
                return 2.6;
            }
            return 1;
        }
        if (sample != null) {
            for (int i = 0; i < sample.length(); i++) {
                char c = sample.charAt(i);
                if (c >= 0x0370 && c <= 0x07FF) {
                    // Greek, Cyrillic, Armenian, Hebrew, Arabic
                    return 1.8;
                }
                if (c >= 0x0900 && c <= 0x1CFF) {
                    return 2.6;
                }
            }
        }
        return 1;
    }

    /** The characters of text in the book, by its sizes. */
    static long chars(String path, double bytesPerChar) throws IOException {
        String name = path.toLowerCase(Locale.US);
        File file = new File(path);
        if (!file.isFile()) {
            return 0;
        }
        // The share of a byte that is markup, by format, measured on books of every format
        if (name.endsWith(".epub")) {
            return zip(file, null, 0.25, bytesPerChar);
        } else if (name.endsWith(".docx")) {
            return zip(file, "word/document.xml", 1.3, bytesPerChar);
        } else if (name.endsWith(".odt")) {
            return zip(file, "content.xml", 1.0, bytesPerChar);
        } else if (name.endsWith(".fb2")) {
            return fb2(file, bytesPerChar);
        } else if (name.endsWith(".html") || name.endsWith(".htm") || name.endsWith(".xhtml")) {
            return (long) (file.length() / (0.6 + sniff(file, bytesPerChar)));
        } else if (name.endsWith(".txt") || name.endsWith(".md")) {
            return (long) (file.length() / (0.02 + sniff(file, bytesPerChar)));
        } else if (name.endsWith(".rtf")) {
            // A letter outside ASCII is written \'e0, four bytes
            return (long) (file.length() / (0.2 + (bytesPerChar > 1 ? 4 : 1)));
        } else if (name.endsWith(".doc")) {
            return doc(file, bytesPerChar);
        } else if (name.endsWith(".mobi") || name.endsWith(".azw") || name.endsWith(".azw3")) {
            return mobi(file, bytesPerChar);
        }
        return 0;
    }

    /**
     * The uncompressed size of the text of a zip, from its directory: nothing is inflated. One
     * entry, or every chapter of an EPUB; an EPUB of pictures alone, a comic, is a page a chapter.
     */
    private static long zip(File file, String entryName, double markup, double bytesPerChar) throws IOException {
        long bytes = 0;
        int documents = 0;
        try (ZipFile zip = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (isText(entry, entryName)) {
                    documents++;
                    bytes += Math.max(0, entry.getSize());
                }
            }
        }
        long chars = (long) (bytes / (markup + bytesPerChar));
        if (entryName == null && chars < (long) documents * CHARS_PER_PAGE / 4) {
            // Chapters that hardly hold any text are pictures, a page each
            return (long) documents * CHARS_PER_PAGE;
        }
        return chars;
    }

    private static boolean isText(ZipEntry entry, String entryName) {
        if (entry.isDirectory()) {
            return false;
        }
        String name = entry.getName();
        if (entryName != null) {
            return entryName.equals(name);
        }
        String lower = name.toLowerCase(Locale.US);
        // Adobe's early EPUBs name their chapters .xml
        return !lower.startsWith("meta-inf") && (lower.endsWith(".xhtml") || lower.endsWith(".html") || lower.endsWith(".htm")
                || lower.endsWith(".xml"));
    }

    /**
     * An FB2 carries its pictures inline, as base64 after the text - often more of the file than
     * the text is. Where the text ends is found by halves, a kilobyte read at each step, and only
     * what comes before it counts. An FB2 in a single-byte encoding, as most Russian ones are,
     * takes a byte a letter whatever its language.
     */
    static long fb2(File file, double bytesPerChar) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long length = raf.length();
            byte[] head = new byte[(int) Math.min(200, length)];
            raf.readFully(head);
            String prolog = new String(head, "ISO-8859-1").toLowerCase(Locale.US);
            int encoding = prolog.indexOf("encoding=");
            if (encoding >= 0 && !prolog.startsWith("utf-8", encoding + 10) && !prolog.startsWith("utf8", encoding + 10)) {
                bytesPerChar = 1;
            }
            long text = length;
            byte[] window = new byte[1024];
            if (length > 64 * 1024 && isPicture(raf, length - window.length, window)) {
                long lo = 0, hi = length - window.length;
                while (hi - lo > 4096) {
                    long mid = (lo + hi) / 2;
                    if (isPicture(raf, mid, window)) {
                        hi = mid;
                    } else {
                        lo = mid;
                    }
                }
                text = lo;
            }
            return (long) (text / (0.15 + bytesPerChar));
        }
    }

    /** Whether this kilobyte of an FB2 is a picture's, or the start of the pictures: base64, or a binary element. */
    private static boolean isPicture(RandomAccessFile raf, long at, byte[] window) throws IOException {
        raf.seek(at);
        int n = raf.read(window);
        String text = new String(window, 0, Math.max(0, n), "ISO-8859-1");
        if (text.contains("<binary") || text.contains("</body>")) {
            return true;
        }
        for (int i = 0; i < n; i++) {
            int b = window[i] & 0xFF;
            boolean base64 = (b >= 'A' && b <= 'Z') || (b >= 'a' && b <= 'z') || (b >= '0' && b <= '9') || b == '+' || b == '/'
                    || b == '=' || b == '\r' || b == '\n' || b == ' ' || b == '\t' || b == '<' || b == '>';
            if (!base64) {
                return false;
            }
        }
        return true;
    }

    /**
     * The bytes a character takes in a text file, by the first 4 KB of it: ASCII is one, a
     * file in a single-byte encoding is one whatever its language, and UTF-8 is what the language
     * makes it.
     */
    static double sniff(File file, double bytesPerChar) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] head = new byte[(int) Math.min(4096, raf.length())];
            raf.readFully(head);
            if (head.length >= 2 && ((head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xFE || (head[0] & 0xFF) == 0xFE && (head[1] & 0xFF) == 0xFF)) {
                return 2;
            }
            int bytes = 0, chars = 0, follow = 0;
            for (byte value : head) {
                int b = value & 0xFF;
                if (follow > 0) {
                    if ((b & 0xC0) != 0x80) {
                        // Not UTF-8: a byte a letter
                        return 1;
                    }
                    follow--;
                } else if (b >= 0x80) {
                    if ((b & 0xE0) == 0xC0) {
                        follow = 1;
                    } else if ((b & 0xF0) == 0xE0) {
                        follow = 2;
                    } else if ((b & 0xF8) == 0xF0) {
                        follow = 3;
                    } else {
                        return 1;
                    }
                    chars++;
                } else {
                    chars++;
                }
                bytes++;
            }
            return chars == 0 ? bytesPerChar : (double) bytes / chars;
        }
    }

    /**
     * A Word 97-2003 document keeps the number of characters of its main text in the header of its
     * WordDocument stream (ccpText). A .doc that is RTF or HTML under another name, as many are, is
     * measured as what it is.
     */
    static long doc(File file, double bytesPerChar) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] head = new byte[512];
            raf.readFully(head);
            if (head[0] == '{' && head[1] == '\\' && head[2] == 'r') {
                return (long) (file.length() / (0.2 + (bytesPerChar > 1 ? 4 : 1)));
            }
            if ((head[0] & 0xFF) != 0xD0 || (head[1] & 0xFF) != 0xCF || (head[2] & 0xFF) != 0x11 || (head[3] & 0xFF) != 0xE0) {
                return (long) (file.length() / (0.6 + bytesPerChar));
            }
            int sectorSize = 1 << le16(head, 0x1E);
            int[] difat = new int[109];
            for (int i = 0; i < 109; i++) {
                difat[i] = le32(head, 0x4C + i * 4);
            }
            byte[] sector = new byte[sectorSize];
            int directory = le32(head, 0x30);
            for (int walked = 0; walked < 64 && directory >= 0; walked++) {
                raf.seek((long) (directory + 1) * sectorSize);
                raf.readFully(sector);
                for (int e = 0; e + 128 <= sectorSize; e += 128) {
                    int nameLength = le16(sector, e + 0x40);
                    if (nameLength == 26 && "WordDocument".equals(new String(sector, e, 24, "UTF-16LE"))) {
                        int start = le32(sector, e + 0x74);
                        raf.seek((long) (start + 1) * sectorSize);
                        byte[] fib = new byte[0x50];
                        raf.readFully(fib);
                        int ident = le16(fib, 0);
                        if (ident != 0xA5EC && ident != 0xA5DC) {
                            return 0;
                        }
                        return Math.max(0, le32(fib, 0x4C));
                    }
                }
                directory = next(raf, difat, sectorSize, directory);
            }
            return 0;
        }
    }

    /** The sector after this one in its chain, by the allocation table. */
    private static int next(RandomAccessFile raf, int[] difat, int sectorSize, int sector) throws IOException {
        int perSector = sectorSize / 4;
        int index = sector / perSector;
        if (index >= difat.length || difat[index] < 0) {
            return -1;
        }
        raf.seek((long) (difat[index] + 1) * sectorSize + (long) (sector % perSector) * 4);
        byte[] entry = new byte[4];
        raf.readFully(entry);
        return le32(entry, 0);
    }

    /**
     * A MOBI, AZW or AZW3 gives the length of its text, uncompressed, in its first record. A book in
     * windows-1252 takes a byte a letter.
     */
    static long mobi(File file, double bytesPerChar) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            byte[] head = new byte[86];
            raf.readFully(head);
            String type = new String(head, 60, 8, "ISO-8859-1");
            if (!type.equals("BOOKMOBI") && !type.equals("TEXtREAd")) {
                return 0;
            }
            long record0 = be32(head, 78) & 0xFFFFFFFFL;
            byte[] header = new byte[32];
            raf.seek(record0);
            raf.readFully(header);
            long textLength = be32(header, 4) & 0xFFFFFFFFL;
            if (new String(header, 16, 4, "ISO-8859-1").equals("MOBI") && be32(header, 28) == 1252) {
                bytesPerChar = 1;
            }
            return (long) (textLength / (0.1 + bytesPerChar));
        }
    }

    private static int le16(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8;
    }

    private static int le32(byte[] b, int at) {
        return (b[at] & 0xFF) | (b[at + 1] & 0xFF) << 8 | (b[at + 2] & 0xFF) << 16 | (b[at + 3] & 0xFF) << 24;
    }

    private static int be32(byte[] b, int at) {
        return (b[at] & 0xFF) << 24 | (b[at + 1] & 0xFF) << 16 | (b[at + 2] & 0xFF) << 8 | (b[at + 3] & 0xFF);
    }
}
