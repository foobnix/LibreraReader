package com.foobnix.ext.office;

import com.foobnix.android.utils.LOG;
import com.foobnix.sys.TempHolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.foobnix.ext.office.CompoundFile.readInt;
import static com.foobnix.ext.office.CompoundFile.readShort;

/**
 * A Word 97-2003 document ({@code .doc}) as an EPUB, read straight from its binary format: the
 * text from the piece table, paragraphs with their alignment, headings by their style, bold,
 * italic, underlined and struck-out text, superscript and subscript, links, tables, footnotes and
 * the pictures that stand in the text (PNG and JPEG). Pictures that float over the page, text
 * boxes, headers and footers are left out, and so are fonts, sizes and page layout.
 *
 * Word 6 and Word 95 files are read as their plain text.
 */
public final class DocToEpub {

    private static final Pattern HYPERLINK = Pattern.compile("HYPERLINK\\s+(?:\\\\l\\s+)?\"?([^\"\\s]+)");

    private static final Charset CP1252 = RtfToEpub.charset(1252);

    /** A run of the file with the same formatting: [start, end) in file offsets. */
    private static final class Run {
        final int start;
        final int end;
        final byte[] sprms;
        final int offset;
        final int length;

        Run(int start, int end, byte[] sprms, int offset, int length) {
            this.start = start;
            this.end = end;
            this.sprms = sprms;
            this.offset = offset;
            this.length = length;
        }
    }

    /** A paragraph's properties: what is used of them. */
    private static final class Para {
        int istd;
        int align = Paragraph.ALIGN_LEFT;
        boolean inTable;
        boolean rowEnd;
        int outline = -1;
        boolean list;
    }

    /** A character's properties: what is used of them. */
    private static final class Chars {
        boolean bold, italic, underline, strike, hidden, special;
        int vert;
        int picture = -1;
        boolean ole;
    }

    private final CompoundFile cfb;
    private byte[] word;
    private byte[] table;
    private byte[] dataStream;

    private char[] text;
    private int[] fcOf;
    private int ccpText;
    private int ccpFtn;

    private final List<Run> chpx = new ArrayList<Run>();
    private final List<Run> papx = new ArrayList<Run>();
    private final Map<Integer, Integer> headingOfStyle = new HashMap<Integer, Integer>();
    private final Map<Integer, Integer> footnoteAt = new HashMap<Integer, Integer>();
    private int[] footnoteText = new int[0];

    private final EpubBook book = new EpubBook();
    private final DocBuilder builder = new DocBuilder(book);

    private DocToEpub(CompoundFile cfb) {
        this.cfb = cfb;
    }

    /** Whether the file is a Compound File, the container .doc files are. */
    public static boolean isDoc(String path) {
        try {
            final InputStream in = new FileInputStream(path);
            try {
                final byte[] header = new byte[8];
                return in.read(header) == 8 && CompoundFile.isCompoundFile(header);
            } finally {
                in.close();
            }
        } catch (IOException e) {
            return false;
        }
    }

    /** Converts; false when the file is not a Word document that can be read. */
    public static boolean convert(String path, File epub) {
        try {
            final DocToEpub converter = new DocToEpub(CompoundFile.open(path));
            if (!converter.run()) {
                return false;
            }
            converter.book.write(epub);
            return true;
        } catch (Throwable e) {
            LOG.e(e, path);
            return false;
        }
    }

    private boolean run() throws IOException {
        word = cfb.stream("WordDocument");
        if (word == null || word.length < 0x200 || readShort(word, 0) != 0xA5EC) {
            return false;
        }
        final int nFib = readShort(word, 2);
        final int flags = readShort(word, 0x0A);
        if ((flags & 0x0100) != 0) {
            LOG.d("DocToEpub", "encrypted");
            return false;
        }
        readSummary();
        if (nFib < 0xC1) {
            return plainWord95();
        }
        table = cfb.stream((flags & 0x0200) != 0 ? "1Table" : "0Table");
        if (table == null) {
            return false;
        }
        dataStream = cfb.stream("Data");

        // the FIB: the counts of characters, then the places of the tables in the table stream
        final int csw = readShort(word, 32);
        final int lwStart = 32 + 2 + csw * 2 + 2;
        final int cslw = readShort(word, 32 + 2 + csw * 2);
        ccpText = readInt(word, lwStart + 3 * 4);
        ccpFtn = Math.max(0, readInt(word, lwStart + 4 * 4));
        final int fcLcb = lwStart + cslw * 4 + 2;

        if (!readPieces(fc(fcLcb, 33), lcb(fcLcb, 33))) {
            return false;
        }
        readStyles(fc(fcLcb, 1), lcb(fcLcb, 1));
        readBins(fc(fcLcb, 12), lcb(fcLcb, 12), chpx, false);
        readBins(fc(fcLcb, 13), lcb(fcLcb, 13), papx, true);
        readFootnotes(fc(fcLcb, 2), lcb(fcLcb, 2), fc(fcLcb, 3), lcb(fcLcb, 3));

        convertText();
        builder.finish();
        return !TempHolder.get().loadingCancelled.get();
    }

    private int fc(int fcLcb, int index) {
        return readInt(word, fcLcb + index * 8);
    }

    private int lcb(int fcLcb, int index) {
        return readInt(word, fcLcb + index * 8 + 4);
    }

    // ---- the piece table ------------------------------------------------------------------------

    private boolean readPieces(int fcClx, int lcbClx) {
        if (fcClx < 0 || lcbClx <= 0 || fcClx + lcbClx > table.length) {
            return false;
        }
        int pos = fcClx;
        final int end = fcClx + lcbClx;
        // Prc: property changes for the pieces, not needed here
        while (pos < end && table[pos] == 0x01) {
            pos += 3 + readShort(table, pos + 1);
        }
        if (pos >= end || table[pos] != 0x02) {
            return false;
        }
        final int lcb = readInt(table, pos + 1);
        final int plc = pos + 5;
        if (lcb < 4 || plc + lcb > table.length) {
            return false;
        }
        final int n = (lcb - 4) / 12;
        final int total = Math.max(0, Math.min(ccpText + ccpFtn, 64 * 1024 * 1024));
        text = new char[total];
        fcOf = new int[total];
        Arrays.fill(fcOf, -1);
        final int pcds = plc + (n + 1) * 4;
        for (int i = 0; i < n; i++) {
            final int cpStart = readInt(table, plc + i * 4);
            final int cpEnd = readInt(table, plc + (i + 1) * 4);
            final int fcValue = readInt(table, pcds + i * 8 + 2);
            final boolean compressed = (fcValue & 0x40000000) != 0;
            final int fc = compressed ? (fcValue & 0x3FFFFFFF) / 2 : fcValue & 0x3FFFFFFF;
            for (int cp = Math.max(0, cpStart); cp < cpEnd && cp < total; cp++) {
                final int offset = compressed ? fc + (cp - cpStart) : fc + 2 * (cp - cpStart);
                if (compressed) {
                    if (offset >= word.length) {
                        break;
                    }
                    text[cp] = compressedChar(word[offset] & 0xFF);
                } else {
                    if (offset + 1 >= word.length) {
                        break;
                    }
                    text[cp] = (char) readShort(word, offset);
                }
                fcOf[cp] = offset;
            }
        }
        return true;
    }

    private static char compressedChar(int b) {
        if (b < 0x80) {
            return (char) b;
        }
        return new String(new byte[]{(byte) b}, CP1252).charAt(0);
    }

    // ---- formatting -----------------------------------------------------------------------------

    /** The runs of character or paragraph formatting, from the pages their bin table names. */
    private void readBins(int fc, int lcb, List<Run> runs, boolean paragraphs) {
        if (fc < 0 || lcb < 4 || fc + lcb > table.length) {
            return;
        }
        final int n = (lcb - 4) / 8;
        for (int i = 0; i < n; i++) {
            final int pn = readInt(table, fc + (n + 1) * 4 + i * 4) & 0x3FFFFF;
            final int page = pn * 512;
            if (page + 512 > word.length) {
                continue;
            }
            final int crun = word[page + 511] & 0xFF;
            for (int r = 0; r < crun; r++) {
                final int start = readInt(word, page + r * 4);
                final int end = readInt(word, page + (r + 1) * 4);
                if (paragraphs) {
                    final int bOffset = (word[page + (crun + 1) * 4 + r * 13] & 0xFF) * 2;
                    if (bOffset == 0) {
                        runs.add(new Run(start, end, word, 0, 0));
                        continue;
                    }
                    int at = page + bOffset;
                    int cb = word[at] & 0xFF;
                    int size;
                    if (cb == 0) {
                        at++;
                        size = (word[at] & 0xFF) * 2;
                    } else {
                        size = cb * 2 - 1;
                    }
                    at++;
                    size = Math.max(0, Math.min(size, page + 511 - at));
                    runs.add(new Run(start, end, word, at, size));
                } else {
                    final int bOffset = (word[page + (crun + 1) * 4 + r] & 0xFF) * 2;
                    if (bOffset == 0) {
                        runs.add(new Run(start, end, word, 0, 0));
                        continue;
                    }
                    final int at = page + bOffset;
                    final int size = Math.min(word[at] & 0xFF, page + 511 - at - 1);
                    runs.add(new Run(start, end, word, at + 1, Math.max(0, size)));
                }
            }
        }
        java.util.Collections.sort(runs, (a, b) -> a.start < b.start ? -1 : (a.start == b.start ? 0 : 1));
    }

    private static Run find(List<Run> runs, int fc, Run last) {
        if (last != null && fc >= last.start && fc < last.end) {
            return last;
        }
        int low = 0;
        int high = runs.size() - 1;
        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final Run run = runs.get(mid);
            if (fc < run.start) {
                high = mid - 1;
            } else if (fc >= run.end) {
                low = mid + 1;
            } else {
                return run;
            }
        }
        return null;
    }

    private Para para(int fc) {
        final Para para = new Para();
        final Run run = find(papx, fc, null);
        if (run == null || run.length < 2) {
            return para;
        }
        para.istd = readShort(run.sprms, run.offset);
        sprms(run.sprms, run.offset + 2, run.offset + run.length, para, null);
        return para;
    }

    private Run lastChars;

    private Chars chars(int fc) {
        final Chars chars = new Chars();
        final Run run = find(chpx, fc, lastChars);
        lastChars = run;
        if (run != null && run.length > 0) {
            sprms(run.sprms, run.offset, run.offset + run.length, null, chars);
        }
        return chars;
    }

    /** The properties a list of sprms sets, the ones used here. */
    private static void sprms(byte[] b, int pos, int end, Para para, Chars chars) {
        while (pos + 2 <= end) {
            final int sprm = readShort(b, pos);
            pos += 2;
            final int size;
            switch (sprm >>> 13) {
                case 0:
                case 1:
                    size = 1;
                    break;
                case 2:
                case 4:
                case 5:
                    size = 2;
                    break;
                case 3:
                    size = 4;
                    break;
                case 7:
                    size = 3;
                    break;
                default: // 6: the operand says how long it is
                    if (pos >= end) {
                        return;
                    }
                    if (sprm == 0xD608 || sprm == 0xD606) {
                        size = pos + 2 <= end ? readShort(b, pos) + 1 : end - pos;
                    } else {
                        size = (b[pos] & 0xFF) + 1;
                    }
            }
            if (pos + size > end) {
                return;
            }
            final int op = b[pos] & 0xFF;
            if (chars != null) {
                switch (sprm) {
                    case 0x0835:
                        chars.bold = toggle(op);
                        break;
                    case 0x0836:
                        chars.italic = toggle(op);
                        break;
                    case 0x0837:
                    case 0x2A53:
                        chars.strike = toggle(op);
                        break;
                    case 0x083C:
                        chars.hidden = toggle(op);
                        break;
                    case 0x2A3E:
                        chars.underline = op != 0;
                        break;
                    case 0x2A48:
                        chars.vert = op == 1 ? Paragraph.VERT_SUPER : op == 2 ? Paragraph.VERT_SUB : Paragraph.VERT_NONE;
                        break;
                    case 0x6A03:
                        chars.picture = readInt(b, pos);
                        break;
                    case 0x0855:
                        chars.special = op != 0;
                        break;
                    case 0x080A:
                        chars.ole = op != 0;
                        break;
                    default:
                }
            }
            if (para != null) {
                switch (sprm) {
                    case 0x2403:
                    case 0x2461:
                        para.align = op == 1 ? Paragraph.ALIGN_CENTER : op == 2 ? Paragraph.ALIGN_RIGHT
                                : op == 3 || op == 4 ? Paragraph.ALIGN_JUSTIFY : Paragraph.ALIGN_LEFT;
                        break;
                    case 0x2416:
                        para.inTable = op != 0;
                        break;
                    case 0x2417:
                        para.rowEnd = op != 0;
                        break;
                    case 0x2640:
                        para.outline = op;
                        break;
                    case 0x460B:
                        para.list = readShort(b, pos) != 0;
                        break;
                    case 0x4600:
                        para.istd = readShort(b, pos);
                        break;
                    default:
                }
            }
            pos += size;
        }
    }

    /** 0 off, 1 on; 0x80 as the style has it, 0x81 the other way - taken as off and on. */
    private static boolean toggle(int op) {
        return op == 1 || op == 0x81;
    }

    private void readStyles(int fc, int lcb) {
        if (fc < 0 || lcb < 4 || fc + lcb > table.length) {
            return;
        }
        final int end = fc + lcb;
        final int cbStshi = readShort(table, fc);
        final int stshi = fc + 2;
        final int count = readShort(table, stshi);
        final int cbBase = readShort(table, stshi + 2);
        int pos = stshi + cbStshi;
        for (int istd = 0; istd < count && pos + 2 <= end; istd++) {
            final int cbStd = readShort(table, pos);
            final int std = pos + 2;
            pos = std + cbStd;
            if (cbStd == 0 || pos > end) {
                continue;
            }
            final int sti = readShort(table, std) & 0x0FFF;
            int level = sti >= 1 && sti <= 9 ? sti : 0;
            final int nameAt = std + cbBase;
            if (level == 0 && nameAt + 2 <= pos) {
                final int length = readShort(table, nameAt);
                if (length > 0 && nameAt + 2 + length * 2 <= pos) {
                    final String name = new String(table, nameAt + 2, length * 2, Charset.forName("UTF-16LE"))
                            .trim().toLowerCase(Locale.US);
                    if (name.startsWith("heading ")) {
                        try {
                            level = Math.max(1, Math.min(9, Integer.parseInt(name.substring(8).trim())));
                        } catch (NumberFormatException e) {
                            level = 0;
                        }
                    } else if ("title".equals(name)) {
                        level = 1;
                    }
                }
            }
            if (sti == 62) {
                level = 1; // Title
            }
            if (level > 0) {
                headingOfStyle.put(istd, Math.min(6, level));
            }
        }
    }

    private void readFootnotes(int fcRef, int lcbRef, int fcTxt, int lcbTxt) {
        if (ccpFtn <= 0 || fcRef < 0 || lcbRef < 4 || fcRef + lcbRef > table.length
                || fcTxt < 0 || lcbTxt < 4 || fcTxt + lcbTxt > table.length) {
            return;
        }
        final int n = (lcbRef - 4) / 6;
        for (int i = 0; i < n; i++) {
            footnoteAt.put(readInt(table, fcRef + i * 4), i);
        }
        final int texts = lcbTxt / 4;
        footnoteText = new int[texts];
        for (int i = 0; i < texts; i++) {
            footnoteText[i] = readInt(table, fcTxt + i * 4);
        }
    }

    // ---- the text ---------------------------------------------------------------------------------

    private void convertText() {
        Paragraph paragraph = new Paragraph();
        final List<List<String>> rows = new ArrayList<List<String>>();
        List<String> row = new ArrayList<String>();
        StringBuilder cell = new StringBuilder();

        // fields: the code is hidden, the result shown, a HYPERLINK's result is a link
        final List<StringBuilder> codes = new ArrayList<StringBuilder>();
        final List<Boolean> inCode = new ArrayList<Boolean>();
        final List<String> links = new ArrayList<String>();

        final int end = Math.min(ccpText, text.length);
        for (int cp = 0; cp < end; cp++) {
            if ((cp & 0x3FFF) == 0 && TempHolder.get().loadingCancelled.get()) {
                return;
            }
            final char c = text[cp];
            final int fc = fcOf[cp];
            if (c == 0x13) {
                codes.add(new StringBuilder());
                inCode.add(true);
                links.add(null);
                continue;
            }
            if (c == 0x14 && !codes.isEmpty()) {
                inCode.set(inCode.size() - 1, false);
                final Matcher link = HYPERLINK.matcher(codes.get(codes.size() - 1));
                if (link.find() && link.group(1).contains(":")) {
                    links.set(links.size() - 1, link.group(1));
                }
                continue;
            }
            if (c == 0x15 && !codes.isEmpty()) {
                codes.remove(codes.size() - 1);
                inCode.remove(inCode.size() - 1);
                links.remove(links.size() - 1);
                continue;
            }
            if (!inCode.isEmpty() && inCode.get(inCode.size() - 1)) {
                codes.get(codes.size() - 1).append(c);
                continue;
            }

            if (c == 0x0D || c == 0x07 || c == 0x0C || c == 0x0E) {
                final Para para = para(fc);
                paragraph.align = para.align;
                if (para.inTable || c == 0x07) {
                    if (c == 0x07 && para.rowEnd) {
                        rows.add(row);
                        row = new ArrayList<String>();
                        cell = new StringBuilder();
                    } else {
                        if (!paragraph.isBlank()) {
                            cell.append(paragraph.toXhtml());
                        }
                        if (c == 0x07) {
                            row.add(cell.toString());
                            cell = new StringBuilder();
                        }
                    }
                } else {
                    flushTable(rows, row);
                    row = new ArrayList<String>();
                    paragraph.heading = heading(para);
                    builder.paragraph(paragraph);
                }
                paragraph = new Paragraph();
                continue;
            }

            final Chars chars = fc >= 0 ? chars(fc) : new Chars();
            if (chars.hidden) {
                continue;
            }
            if (c == 0x01 && chars.special) {
                if (chars.picture >= 0 && !chars.ole) {
                    paragraph.image(picture(chars.picture));
                }
                continue;
            }
            if (c == 0x02 && footnoteAt.containsKey(cp)) {
                paragraph.markup(book.footnote(footnote(footnoteAt.get(cp))));
                continue;
            }
            if (c == 0x0B) {
                paragraph.lineBreak();
                continue;
            }
            if (c == 0x09) {
                paragraph.tab();
                continue;
            }
            if (c < 0x20 && c != 0x1E && c != 0x1F) {
                continue; // drawn objects, note separators and other marks
            }
            final Paragraph.Style style = new Paragraph.Style();
            style.bold = chars.bold;
            style.italic = chars.italic;
            style.underline = chars.underline;
            style.strike = chars.strike;
            style.vert = chars.vert;
            style.href = links.isEmpty() ? null : lastLink(links);
            paragraph.text(c == 0x1E ? "‑" : c == 0x1F ? "­" : String.valueOf(c), style);
        }
        if (!paragraph.isBlank()) {
            builder.paragraph(paragraph);
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }
        flushTable(rows, new ArrayList<String>());
    }

    private static String lastLink(List<String> links) {
        for (int i = links.size() - 1; i >= 0; i--) {
            if (links.get(i) != null) {
                return links.get(i);
            }
        }
        return null;
    }

    private void flushTable(List<List<String>> rows, List<String> row) {
        if (!row.isEmpty()) {
            rows.add(row);
        }
        if (rows.isEmpty()) {
            return;
        }
        final StringBuilder out = new StringBuilder("<table>");
        for (List<String> cells : rows) {
            out.append("<tr>");
            for (String content : cells) {
                out.append("<td>").append(content).append("</td>");
            }
            out.append("</tr>");
        }
        out.append("</table>");
        rows.clear();
        builder.block(out.toString());
    }

    private int heading(Para para) {
        if (para.outline >= 0 && para.outline < 9) {
            return Math.min(6, para.outline + 1);
        }
        final Integer level = headingOfStyle.get(para.istd);
        return level == null ? 0 : level;
    }

    /** The text of a footnote, as paragraphs. */
    private String footnote(int index) {
        if (index + 1 >= footnoteText.length) {
            return "";
        }
        final int start = ccpText + footnoteText[index];
        final int end = Math.min(text.length, ccpText + footnoteText[index + 1]);
        final StringBuilder out = new StringBuilder();
        Paragraph p = new Paragraph();
        for (int cp = Math.max(0, start); cp < end; cp++) {
            final char c = text[cp];
            if (c == 0x0D) {
                if (!p.isBlank()) {
                    out.append(p.toXhtml());
                }
                p = new Paragraph();
            } else if (c == 0x0B) {
                p.lineBreak();
            } else if (c == 0x09) {
                p.tab();
            } else if (c >= 0x20) {
                final Chars chars = fcOf[cp] >= 0 ? chars(fcOf[cp]) : new Chars();
                final Paragraph.Style style = new Paragraph.Style();
                style.bold = chars.bold;
                style.italic = chars.italic;
                style.vert = chars.vert;
                p.text(String.valueOf(c), style);
            }
        }
        if (!p.isBlank()) {
            out.append(p.toXhtml());
        }
        return out.toString();
    }

    /** A picture in the text: its PICF in the Data stream, and the PNG or JPEG BLIP within. */
    private String picture(int offset) {
        if (dataStream == null || offset < 0 || offset + 8 > dataStream.length) {
            return null;
        }
        final int lcb = readInt(dataStream, offset);
        final int cbHeader = readShort(dataStream, offset + 4);
        if (lcb <= cbHeader || cbHeader < 0x44 || (long) offset + lcb > dataStream.length) {
            return null;
        }
        final int end = offset + lcb;
        for (int pos = offset + cbHeader; pos + 8 <= end; pos++) {
            final int verInstance = readShort(dataStream, pos);
            final int type = readShort(dataStream, pos + 2);
            final int length = readInt(dataStream, pos + 4);
            if ((verInstance & 0xF) != 0 || length <= 17 || (long) pos + 8 + length > end) {
                continue;
            }
            final int instance = verInstance >>> 4;
            int header;
            if (type == 0xF01E) { // PNG
                header = instance == 0x6E1 ? 33 : 17;
            } else if (type == 0xF01D || type == 0xF02A) { // JPEG
                header = instance == 0x46B || instance == 0x6E3 ? 33 : 17;
            } else {
                continue;
            }
            final int start = pos + 8 + header;
            final int size = length - header;
            if (size <= 0) {
                continue;
            }
            final byte[] image = Arrays.copyOfRange(dataStream, start, start + size);
            final String href = book.image(image, type == 0xF01E ? "png" : "jpg");
            if (href != null) {
                return href;
            }
        }
        return null;
    }

    // ---- Word 6 and 95, and the document's title ----------------------------------------------

    /** The text between fcMin and fcMac, one byte a character, in paragraphs. */
    private boolean plainWord95() {
        final int fcMin = readInt(word, 0x18);
        final int fcMac = readInt(word, 0x1C);
        if (fcMin < 0 || fcMac <= fcMin || fcMac > word.length) {
            return false;
        }
        final String all = new String(word, fcMin, fcMac - fcMin, CP1252);
        Paragraph p = new Paragraph();
        for (int i = 0; i < all.length(); i++) {
            final char c = all.charAt(i);
            if (c == 0x0D || c == 0x07 || c == 0x0C) {
                builder.paragraph(p);
                p = new Paragraph();
            } else if (c == 0x0B) {
                p.lineBreak();
            } else if (c == 0x09) {
                p.tab();
            } else if (c >= 0x20) {
                p.text(String.valueOf(c), null);
            }
        }
        builder.paragraph(p);
        builder.finish();
        return true;
    }

    /** Title and author from the summary information, where the document has them. */
    private void readSummary() {
        try {
            final byte[] s = cfb.stream("\u0005SummaryInformation");
            if (s == null || s.length < 48) {
                return;
            }
            final int section = readInt(s, 44);
            if (section < 0 || section + 8 > s.length) {
                return;
            }
            final int count = readInt(s, section + 4);
            Charset charset = CP1252;
            final Map<Integer, Integer> offsets = new HashMap<Integer, Integer>();
            for (int i = 0; i < count && section + 8 + i * 8 + 8 <= s.length; i++) {
                offsets.put(readInt(s, section + 8 + i * 8), readInt(s, section + 12 + i * 8));
            }
            final Integer codepage = offsets.get(1);
            if (codepage != null && section + codepage + 6 <= s.length) {
                final int value = readShort(s, section + codepage + 4);
                charset = value == 1200 ? Charset.forName("UTF-16LE") : RtfToEpub.charset(value);
            }
            final String title = summaryString(s, section, offsets.get(2), charset);
            final String author = summaryString(s, section, offsets.get(4), charset);
            if (title != null) {
                book.setTitle(title);
            }
            if (author != null) {
                book.setAuthor(author);
            }
        } catch (Exception e) {
            LOG.e(e);
        }
    }

    private static String summaryString(byte[] s, int section, Integer offset, Charset charset) {
        if (offset == null) {
            return null;
        }
        final int at = section + offset;
        if (at < 0 || at + 8 > s.length) {
            return null;
        }
        final int type = readInt(s, at);
        final int length = readInt(s, at + 4);
        if (type == 0x1E && length > 0 && at + 8 + length <= s.length) {
            return trimNul(new String(s, at + 8, length, charset));
        }
        if (type == 0x1F && length > 0 && at + 8 + length * 2 <= s.length) {
            return trimNul(new String(s, at + 8, length * 2, Charset.forName("UTF-16LE")));
        }
        return null;
    }

    private static String trimNul(String value) {
        final int nul = value.indexOf('\0');
        final String trimmed = (nul >= 0 ? value.substring(0, nul) : value).trim();
        return trimmed.length() == 0 ? null : trimmed;
    }
}
