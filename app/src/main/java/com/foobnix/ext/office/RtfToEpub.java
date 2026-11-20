package com.foobnix.ext.office;

import com.foobnix.android.utils.LOG;
import com.foobnix.sys.TempHolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Rich Text Format document as an EPUB: the text in the code pages its fonts name, paragraphs
 * and their alignment, headings by their style's outline level or name, bold, italic, underlined
 * and struck-out text, superscript and subscript, links, tables, footnotes and PNG and JPEG
 * pictures. Fonts, sizes, colours and page layout are left to the reader.
 *
 * A Word document saved as RTF carries its pictures twice, as PNG or JPEG for new readers and as
 * a Windows metafile for old ones; the metafile copy is the one left out.
 */
public final class RtfToEpub {

    private static final Pattern HYPERLINK = Pattern.compile("HYPERLINK\\s+(?:\\\\l\\s+)?\"?([^\"\\s]+)");

    /** Destinations whose text is not part of the document. */
    private static final String[] SKIPPED = {"colortbl", "header", "headerl", "headerr", "headerf", "footer",
            "footerl", "footerr", "footerf", "nonshppict", "objdata", "themedata", "colorschememapping",
            "latentstyles", "datastore", "xmlnstbl", "listtable", "listoverridetable", "rsidtbl", "generator",
            "pgdsctbl", "filetbl", "revtbl", "mmathPr", "bkmkstart", "bkmkend", "fldtype", "xmlopen",
            "xmlattrname", "xmlattrvalue", "template", "pnseclvl", "operator", "company", "category",
            "keywords", "comment", "doccomm", "hlinkbase", "subject", "manager", "userprops", "annotation",
            "atnid", "atnauthor", "atrfstart", "atrfend", "panose", "falt", "fname", "passwordhash",
            "wgrffmtfilter", "aftnsep", "aftnsepc", "aftncn", "ftnsep", "ftnsepc", "ftncn", "background"};

    /** What a group knows; a group starts with its parent's. */
    private static final class State implements Cloneable {
        boolean bold, italic, underline, strike, hidden;
        int vert;
        int font = -1;
        int uc = 1;
        /** 0 the text, 1 a destination left out, 2 a picture, 3 a field's code, 4 info text */
        int dest;
        String infoKey;

        State copy() {
            try {
                return (State) clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }

    private static final int TEXT = 0;
    private static final int SKIP = 1;
    private static final int PICT = 2;
    private static final int FIELD_CODE = 3;
    private static final int INFO = 4;

    private final byte[] rtf;
    private int pos;

    private final EpubBook book = new EpubBook();
    private final DocBuilder builder = new DocBuilder(book);

    private State state = new State();
    private final List<State> stack = new ArrayList<State>();

    private Charset defaultCharset = charset(1252);
    private final Map<Integer, Integer> fontCharsets = new HashMap<Integer, Integer>();
    private int defaultFont = -1;

    /** Style number to heading level. */
    private final Map<Integer, Integer> styleHeadings = new HashMap<Integer, Integer>();

    private Paragraph paragraph = new Paragraph();
    private int paragraphStyle = 0;
    private int outlineLevel = -1;
    private int align = Paragraph.ALIGN_LEFT;
    private boolean inTable;

    // a table: rows of cells of XHTML
    private final List<List<String>> rows = new ArrayList<List<String>>();
    private List<String> row;
    private StringBuilder cell;

    // the group depths where a field's result, a footnote, a picture, info, a style start
    private int fieldDepth = -1;
    private String fieldLink;
    private final StringBuilder fieldCode = new StringBuilder();
    private final List<Integer> fields = new ArrayList<Integer>();
    private final List<String> fieldLinks = new ArrayList<String>();
    private int pictDepth = -1;
    private String pictKind;
    private final ByteArrayOutputStream pict = new ByteArrayOutputStream();
    private int pictNibble = -1;
    private int footnoteDepth = -1;
    private Paragraph footnoteOwner;
    private EpubBookNotes footnoteContent;
    private final StringBuilder infoText = new StringBuilder();
    private int stylesheetDepth = -1;
    private int styleEntryDepth = -1;
    private int styleNumber;
    private int styleOutline;
    private final StringBuilder styleName = new StringBuilder();
    private int fontTableDepth = -1;
    private int fontNumber = -1;

    private final ByteArrayOutputStream pendingBytes = new ByteArrayOutputStream();
    private int skipChars;

    private final boolean firstImageOnly;
    private byte[] firstImage;

    /** The paragraphs of a footnote, gathered apart from the main text. */
    private static final class EpubBookNotes {
        final StringBuilder xhtml = new StringBuilder();
    }

    private RtfToEpub(byte[] rtf, boolean firstImageOnly) {
        this.rtf = rtf;
        this.firstImageOnly = firstImageOnly;
    }

    /** Converts; false when the file is not RTF. */
    public static boolean convert(String path, File epub) {
        try {
            final byte[] data = read(path);
            if (!isRtf(data)) {
                return false;
            }
            final RtfToEpub converter = new RtfToEpub(data, false);
            converter.parse();
            if (TempHolder.get().loadingCancelled.get()) {
                return false;
            }
            converter.book.write(epub);
            return true;
        } catch (Throwable e) {
            LOG.e(e, path);
            return false;
        }
    }

    /** The first PNG or JPEG picture of the document, for its cover; null when there is none. */
    public static byte[] coverImage(String path) {
        try {
            final byte[] data = read(path);
            if (!isRtf(data)) {
                return null;
            }
            final RtfToEpub converter = new RtfToEpub(data, true);
            converter.parse();
            return converter.firstImage;
        } catch (Throwable e) {
            LOG.e(e, path);
            return null;
        }
    }

    public static boolean isRtf(byte[] data) {
        return data != null && data.length > 5 && data[0] == '{' && data[1] == '\\' && data[2] == 'r'
                && data[3] == 't' && data[4] == 'f';
    }

    // ---- the tokens ------------------------------------------------------------------------------

    private void parse() {
        final int length = rtf.length;
        while (pos < length) {
            if (firstImageOnly && firstImage != null) {
                return;
            }
            if ((pos & 0xFFFF) == 0 && TempHolder.get().loadingCancelled.get()) {
                return;
            }
            final int c = rtf[pos] & 0xFF;
            if (c == '{') {
                pos++;
                flushBytes();
                skipChars = 0;
                stack.add(state);
                state = state.copy();
                if (state.dest == PICT) {
                    state.dest = SKIP; // a picture's properties, as its id: not its data
                }
            } else if (c == '}') {
                pos++;
                flushBytes();
                skipChars = 0;
                endGroup();
            } else if (c == '\\') {
                pos++;
                control();
            } else if (c == '\r' || c == '\n') {
                pos++;
            } else {
                pos++;
                if (state.dest == PICT) {
                    hex(c);
                } else if (skipChars > 0) {
                    skipChars--;
                } else {
                    flushBytes();
                    character(String.valueOf((char) c), c >= 0x80);
                }
            }
        }
        flushBytes();
        endParagraph();
        closeTable();
        builder.finish();
    }

    private void control() {
        if (pos >= rtf.length) {
            return;
        }
        final int c = rtf[pos] & 0xFF;
        if (!isLetter(c)) {
            pos++;
            symbol(c);
            return;
        }
        final int start = pos;
        while (pos < rtf.length && isLetter(rtf[pos] & 0xFF)) {
            pos++;
        }
        final String word = new String(rtf, start, pos - start, Charset.forName("US-ASCII"));
        boolean hasParam = false;
        int param = 0;
        boolean negative = false;
        if (pos < rtf.length && rtf[pos] == '-') {
            negative = true;
            pos++;
        }
        while (pos < rtf.length && rtf[pos] >= '0' && rtf[pos] <= '9') {
            hasParam = true;
            param = param * 10 + (rtf[pos] - '0');
            pos++;
        }
        if (negative) {
            param = -param;
        }
        if (pos < rtf.length && rtf[pos] == ' ') {
            pos++; // the space that ends a control word is part of it
        }
        if (!"u".equals(word) && skipChars > 0) {
            skipChars--;
            return;
        }
        if (!"u".equals(word)) {
            flushBytes();
        }
        controlWord(word, hasParam, param);
    }

    private void symbol(int c) {
        if (c == '\'') {
            if (pos + 2 <= rtf.length) {
                final int value = hexValue(rtf[pos]) * 16 + hexValue(rtf[pos + 1]);
                pos += 2;
                if (skipChars > 0) {
                    skipChars--;
                    return;
                }
                if (state.dest == TEXT || state.dest == FIELD_CODE || state.dest == INFO || inStyleOrFont()) {
                    pendingBytes.write(value);
                }
            }
            return;
        }
        if (skipChars > 0) {
            skipChars--;
            return;
        }
        flushBytes();
        switch (c) {
            case '*':
                // an optional destination: left out unless it is one known here
                state.dest = state.dest == TEXT ? -1 : state.dest;
                return;
            case '~':
                character(" ", false);
                return;
            case '-':
                character("­", false);
                return;
            case '_':
                character("‑", false);
                return;
            case '\r':
            case '\n':
                controlWord("par", false, 0);
                return;
            case '\\':
            case '{':
            case '}':
                character(String.valueOf((char) c), false);
                return;
            case '\t':
                controlWord("tab", false, 0);
                return;
            default:
                // \| \: and the like: index and formula marks
        }
    }

    private void controlWord(String word, boolean hasParam, int param) {
        // an optional destination, \*\word: known ones are read, the rest left out
        if (state.dest == -1) {
            state.dest = TEXT;
            if (!"shppict".equals(word) && !"fldinst".equals(word) && !"pict".equals(word)
                    && !"footnote".equals(word)) {
                state.dest = SKIP;
                return;
            }
        }
        if (state.dest == SKIP) {
            return;
        }
        if (state.dest == PICT) {
            pictWord(word, param);
            return;
        }
        switch (word) {
            case "rtf":
            case "ansi":
                return;
            case "ansicpg":
                defaultCharset = charset(param);
                return;
            case "mac":
                defaultCharset = charset(10000);
                return;
            case "deff":
                defaultFont = param;
                return;
            case "fonttbl":
                fontTableDepth = stack.size();
                state.dest = SKIP_TEXT_KEEP_WORDS;
                return;
            case "stylesheet":
                stylesheetDepth = stack.size();
                return;
            case "info":
                return;
            case "title":
            case "author":
                state.dest = INFO;
                state.infoKey = word;
                infoText.setLength(0);
                return;
            case "pict":
                state.dest = PICT;
                pictDepth = stack.size();
                pictKind = null;
                pict.reset();
                pictNibble = -1;
                return;
            case "shppict":
            case "result":
                return;
            case "object":
                return;
            case "field":
                fields.add(stack.size());
                fieldLinks.add(null);
                return;
            case "fldinst":
                state.dest = FIELD_CODE;
                fieldCode.setLength(0);
                return;
            case "fldrslt":
                if (!fieldLinks.isEmpty()) {
                    fieldDepth = stack.size();
                    fieldLink = fieldLinks.get(fieldLinks.size() - 1);
                }
                return;
            case "footnote":
                startFootnote();
                return;
            case "chftn":
                return; // the note's number: one of Librera's own is put in
            case "bin":
                pos = Math.min(rtf.length, pos + Math.max(0, param));
                return;
            case "u":
                if (state.dest == TEXT || state.dest == FIELD_CODE || state.dest == INFO) {
                    flushBytes();
                    character(String.valueOf((char) (param < 0 ? param + 65536 : param)), false);
                }
                skipChars = state.uc;
                return;
            case "uc":
                state.uc = Math.max(0, param);
                return;
            default:
        }

        if (fontTableDepth >= 0 && stack.size() >= fontTableDepth) {
            if ("f".equals(word)) {
                fontNumber = param;
            } else if ("fcharset".equals(word) && fontNumber >= 0) {
                fontCharsets.put(fontNumber, param);
            }
            return;
        }
        if (stylesheetDepth >= 0 && stack.size() > stylesheetDepth) {
            styleWord(word, hasParam, param);
            return;
        }

        switch (word) {
            case "par":
            case "sect":
            case "page":
                endParagraph();
                return;
            case "line":
            case "lbr":
                paragraph.lineBreak();
                return;
            case "tab":
                if (state.dest == TEXT && !state.hidden) {
                    paragraph.tab();
                }
                return;
            case "emdash":
                character("—", false);
                return;
            case "endash":
                character("–", false);
                return;
            case "bullet":
                character("•", false);
                return;
            case "lquote":
                character("‘", false);
                return;
            case "rquote":
                character("’", false);
                return;
            case "ldblquote":
                character("“", false);
                return;
            case "rdblquote":
                character("”", false);
                return;
            case "emspace":
            case "enspace":
            case "qmspace":
                character(" ", false);
                return;
            case "pard":
                align = Paragraph.ALIGN_LEFT;
                paragraphStyle = 0;
                outlineLevel = -1;
                inTable = false;
                return;
            case "plain":
                state.bold = state.italic = state.underline = state.strike = state.hidden = false;
                state.vert = Paragraph.VERT_NONE;
                state.font = -1;
                return;
            case "b":
                state.bold = !hasParam || param != 0;
                return;
            case "i":
                state.italic = !hasParam || param != 0;
                return;
            case "ul":
            case "uld":
            case "uldb":
            case "uldash":
            case "ulth":
            case "ulw":
            case "ulwave":
                state.underline = !hasParam || param != 0;
                return;
            case "ulnone":
                state.underline = false;
                return;
            case "strike":
            case "striked":
                state.strike = !hasParam || param != 0;
                return;
            case "super":
                state.vert = Paragraph.VERT_SUPER;
                return;
            case "sub":
                state.vert = Paragraph.VERT_SUB;
                return;
            case "nosupersub":
                state.vert = Paragraph.VERT_NONE;
                return;
            case "v":
                state.hidden = !hasParam || param != 0;
                return;
            case "f":
                state.font = param;
                return;
            case "qc":
                align = Paragraph.ALIGN_CENTER;
                return;
            case "qr":
                align = Paragraph.ALIGN_RIGHT;
                return;
            case "qj":
                align = Paragraph.ALIGN_JUSTIFY;
                return;
            case "ql":
                align = Paragraph.ALIGN_LEFT;
                return;
            case "s":
                paragraphStyle = param;
                return;
            case "outlinelevel":
                outlineLevel = param;
                return;
            case "intbl":
                inTable = true;
                return;
            case "cell":
            case "nestcell":
                endCell();
                return;
            case "row":
            case "nestrow":
                endRow();
                return;
            case "trowd":
                return;
            default:
                if (isSkippedDestination(word)) {
                    state.dest = SKIP;
                }
        }
    }

    /** The font table's text is the fonts' names: not shown, but its words are read. */
    private static final int SKIP_TEXT_KEEP_WORDS = 5;

    private boolean inStyleOrFont() {
        return state.dest == SKIP_TEXT_KEEP_WORDS
                || (stylesheetDepth >= 0 && stack.size() > stylesheetDepth);
    }

    private static boolean isSkippedDestination(String word) {
        for (String skipped : SKIPPED) {
            if (skipped.equals(word)) {
                return true;
            }
        }
        return false;
    }

    // ---- what the groups hold --------------------------------------------------------------------

    private void endGroup() {
        final int depth = stack.size();
        if (pictDepth >= 0 && depth == pictDepth && state.dest == PICT) {
            endPicture();
            pictDepth = -1;
        }
        if (state.dest == FIELD_CODE && !fieldLinks.isEmpty()) {
            final Matcher link = HYPERLINK.matcher(fieldCode);
            if (link.find() && link.group(1).contains(":")) {
                fieldLinks.set(fieldLinks.size() - 1, link.group(1));
            }
        }
        if (state.dest == INFO && state.infoKey != null) {
            if ("title".equals(state.infoKey)) {
                book.setTitle(infoText.toString());
            } else {
                book.setAuthor(infoText.toString());
            }
        }
        if (fieldDepth >= 0 && depth == fieldDepth) {
            fieldDepth = -1;
            fieldLink = null;
        }
        if (!fields.isEmpty() && depth == fields.get(fields.size() - 1)) {
            fields.remove(fields.size() - 1);
            fieldLinks.remove(fieldLinks.size() - 1);
        }
        if (footnoteDepth >= 0 && depth == footnoteDepth) {
            endFootnote();
        }
        if (styleEntryDepth >= 0 && depth == styleEntryDepth) {
            endStyle();
        }
        if (stylesheetDepth >= 0 && depth == stylesheetDepth) {
            stylesheetDepth = -1;
        }
        if (fontTableDepth >= 0 && depth == fontTableDepth) {
            fontTableDepth = -1;
        }
        if (fontTableDepth >= 0 && depth == fontTableDepth + 1) {
            fontNumber = -1;
        }
        if (!stack.isEmpty()) {
            state = stack.remove(stack.size() - 1);
        }
    }

    private void character(String text, boolean undecoded) {
        if (undecoded) {
            pendingBytes.write(text.charAt(0));
            return;
        }
        if (state.dest == FIELD_CODE) {
            fieldCode.append(text);
            return;
        }
        if (state.dest == INFO) {
            infoText.append(text);
            return;
        }
        if (stylesheetDepth >= 0 && stack.size() > stylesheetDepth) {
            styleName.append(text);
            return;
        }
        if (state.dest != TEXT || state.hidden) {
            return;
        }
        final Paragraph.Style style = new Paragraph.Style();
        style.bold = state.bold;
        style.italic = state.italic;
        style.underline = state.underline;
        style.strike = state.strike;
        style.vert = state.vert;
        style.href = fieldDepth >= 0 ? fieldLink : null;
        paragraph.text(text, style);
    }

    private void flushBytes() {
        if (pendingBytes.size() == 0) {
            return;
        }
        final byte[] bytes = pendingBytes.toByteArray();
        pendingBytes.reset();
        character(new String(bytes, currentCharset()), false);
    }

    private Charset currentCharset() {
        final int font = state.font >= 0 ? state.font : defaultFont;
        final Integer fcharset = fontCharsets.get(font);
        if (fcharset != null) {
            final Charset charset = charset(codepageOf(fcharset));
            if (charset != null) {
                return charset;
            }
        }
        return defaultCharset != null ? defaultCharset : Charset.forName("ISO-8859-1");
    }

    private void endParagraph() {
        flushBytes();
        if (footnoteDepth >= 0) {
            if (!paragraph.isBlank()) {
                footnoteContent.xhtml.append(paragraph.toXhtml());
            }
            paragraph = new Paragraph();
            return;
        }
        paragraph.align = align;
        paragraph.heading = heading();
        if (inTable) {
            if (cell == null) {
                cell = new StringBuilder();
            }
            if (!paragraph.isBlank()) {
                cell.append(paragraph.toXhtml());
            }
        } else {
            closeTable();
            builder.paragraph(paragraph);
        }
        paragraph = new Paragraph();
    }

    private int heading() {
        if (outlineLevel >= 0 && outlineLevel < 9) {
            return Math.min(6, outlineLevel + 1);
        }
        final Integer level = styleHeadings.get(paragraphStyle);
        return level == null ? 0 : level;
    }

    private void endCell() {
        flushBytes();
        if (cell == null) {
            cell = new StringBuilder();
        }
        if (!paragraph.isBlank()) {
            paragraph.align = align;
            cell.append(paragraph.toXhtml());
        }
        paragraph = new Paragraph();
        if (row == null) {
            row = new ArrayList<String>();
        }
        row.add(cell.toString());
        cell = null;
        inTable = true;
    }

    private void endRow() {
        if (row != null) {
            rows.add(row);
        }
        row = null;
        cell = null;
    }

    private void closeTable() {
        if (row != null && !row.isEmpty()) {
            rows.add(row);
        }
        row = null;
        cell = null;
        if (rows.isEmpty()) {
            return;
        }
        final StringBuilder table = new StringBuilder("<table>");
        for (List<String> cells : rows) {
            table.append("<tr>");
            for (String content : cells) {
                table.append("<td>").append(content).append("</td>");
            }
            table.append("</tr>");
        }
        table.append("</table>");
        rows.clear();
        builder.block(table.toString());
    }

    private void startFootnote() {
        if (footnoteDepth >= 0 || firstImageOnly) {
            state.dest = SKIP; // a note in a note
            return;
        }
        flushBytes();
        footnoteDepth = stack.size();
        footnoteOwner = paragraph;
        footnoteContent = new EpubBookNotes();
        paragraph = new Paragraph();
    }

    private void endFootnote() {
        flushBytes();
        if (!paragraph.isBlank()) {
            footnoteContent.xhtml.append(paragraph.toXhtml());
        }
        paragraph = footnoteOwner;
        footnoteDepth = -1;
        paragraph.markup(book.footnote(footnoteContent.xhtml.toString()));
        footnoteContent = null;
    }

    // ---- the stylesheet ----------------------------------------------------------------------------

    private void styleWord(String word, boolean hasParam, int param) {
        if (styleEntryDepth < 0 && stack.size() == stylesheetDepth + 1) {
            styleEntryDepth = stack.size();
            styleNumber = 0;
            styleOutline = -1;
            styleName.setLength(0);
        }
        if ("s".equals(word)) {
            styleNumber = param;
        } else if ("cs".equals(word) || "ds".equals(word) || "ts".equals(word)) {
            styleNumber = Integer.MIN_VALUE; // a character, section or table style
        } else if ("outlinelevel".equals(word)) {
            styleOutline = param;
        }
    }

    private void endStyle() {
        styleEntryDepth = -1;
        if (styleNumber == Integer.MIN_VALUE) {
            return;
        }
        int level = 0;
        if (styleOutline >= 0 && styleOutline < 9) {
            level = Math.min(6, styleOutline + 1);
        } else {
            String name = styleName.toString().trim().toLowerCase(Locale.US);
            if (name.endsWith(";")) {
                name = name.substring(0, name.length() - 1).trim();
            }
            if (name.startsWith("heading ")) {
                try {
                    level = Math.max(1, Math.min(6, Integer.parseInt(name.substring(8).trim())));
                } catch (NumberFormatException e) {
                    level = 0;
                }
            } else if ("title".equals(name)) {
                level = 1;
            }
        }
        if (level > 0) {
            styleHeadings.put(styleNumber, level);
        }
        styleName.setLength(0);
    }

    // ---- pictures ------------------------------------------------------------------------------------

    private void pictWord(String word, int param) {
        if ("pngblip".equals(word)) {
            pictKind = "png";
        } else if ("jpegblip".equals(word)) {
            pictKind = "jpg";
        } else if ("wmetafile".equals(word) || "emfblip".equals(word) || "macpict".equals(word)
                || "dibitmap".equals(word) || "wbitmap".equals(word) || "pmmetafile".equals(word)) {
            pictKind = null;
        } else if ("bin".equals(word)) {
            final int length = Math.max(0, Math.min(param, rtf.length - pos));
            pict.write(rtf, pos, length);
            pos += length;
        }
    }

    private void hex(int c) {
        final int value = hexValue((byte) c);
        if (value < 0) {
            return;
        }
        if (pictNibble < 0) {
            pictNibble = value;
        } else {
            pict.write(pictNibble * 16 + value);
            pictNibble = -1;
        }
    }

    private void endPicture() {
        if (pictKind == null || pict.size() == 0) {
            return;
        }
        final byte[] data = pict.toByteArray();
        pict.reset();
        if (firstImageOnly) {
            if (EpubBook.imageExtension(data, pictKind) != null) {
                firstImage = data;
            }
            return;
        }
        final String href = book.image(data, pictKind);
        if (href != null && !state.hidden) {
            paragraph.image(href);
        }
    }

    // ---- helpers --------------------------------------------------------------------------------------

    private static boolean isLetter(int c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static int hexValue(byte b) {
        if (b >= '0' && b <= '9') {
            return b - '0';
        }
        if (b >= 'a' && b <= 'f') {
            return b - 'a' + 10;
        }
        if (b >= 'A' && b <= 'F') {
            return b - 'A' + 10;
        }
        return -1;
    }

    /** The code page of a font's \fcharset. */
    private static int codepageOf(int fcharset) {
        switch (fcharset) {
            case 77:
                return 10000;
            case 128:
                return 932;
            case 129:
                return 949;
            case 134:
                return 936;
            case 136:
                return 950;
            case 161:
                return 1253;
            case 162:
                return 1254;
            case 163:
                return 1258;
            case 177:
                return 1255;
            case 178:
                return 1256;
            case 186:
                return 1257;
            case 204:
                return 1251;
            case 222:
                return 874;
            case 238:
                return 1250;
            default:
                return 1252;
        }
    }

    static Charset charset(int codepage) {
        final String name;
        switch (codepage) {
            case 932:
                name = "Shift_JIS";
                break;
            case 936:
                name = "GBK";
                break;
            case 949:
                name = "EUC-KR";
                break;
            case 950:
                name = "Big5";
                break;
            case 874:
                name = "x-windows-874";
                break;
            case 10000:
                name = "x-MacRoman";
                break;
            case 65001:
                name = "UTF-8";
                break;
            default:
                name = "windows-" + codepage;
        }
        try {
            return Charset.forName(name);
        } catch (Exception e) {
            return Charset.forName("windows-1252");
        }
    }

    private static byte[] read(String path) throws IOException {
        final InputStream in = new FileInputStream(path);
        return DocxToEpub.bytes(in);
    }
}
