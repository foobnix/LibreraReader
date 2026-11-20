package com.foobnix.ext.office;

import com.foobnix.android.utils.LOG;
import com.foobnix.ext.XmlParser;
import com.foobnix.sys.TempHolder;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A Word document ({@code .docx}, Office Open XML) as an EPUB: the text with its paragraphs,
 * headings, lists and tables, bold, italic, underlined and struck-out text, superscript and
 * subscript, links, pictures, text boxes and footnotes. Layout - fonts, sizes, colours, page
 * set-up - is left to the reader.
 */
public final class DocxToEpub {

    private static final String WORD = "word/";

    /** What a style says, with what it is based on resolved. */
    private static final class StyleInfo {
        String name;
        String basedOn;
        int outline = -1;
        Boolean bold;
        Boolean italic;
        Boolean underline;
        /** The list a style makes its paragraphs items of, as "List Bullet" does. */
        String numId;
        int ilvl;
    }

    private final ZipFile zip;
    private final EpubBook book = new EpubBook();
    private final DocBuilder builder = new DocBuilder(book);

    private final Map<String, String> relations = new HashMap<String, String>();
    private final Map<String, StyleInfo> styles = new HashMap<String, StyleInfo>();
    /** numId to the formats of its levels, "bullet", "decimal", ... */
    private final Map<String, Map<Integer, String>> numbering = new HashMap<String, Map<Integer, String>>();
    private final Map<String, XmlNode> footnotes = new HashMap<String, XmlNode>();
    private final Map<String, XmlNode> endnotes = new HashMap<String, XmlNode>();
    private final Map<String, String> imageCache = new HashMap<String, String>();

    /** Text boxes met inside a paragraph, put after it. */
    private final List<XmlNode> textBoxes = new ArrayList<XmlNode>();

    private DocxToEpub(ZipFile zip) {
        this.zip = zip;
    }

    /** Converts; false when the file is not a Word document that can be read. */
    public static boolean convert(String docx, File epub) {
        ZipFile zip = null;
        try {
            zip = new ZipFile(docx);
            final DocxToEpub converter = new DocxToEpub(zip);
            if (!converter.run()) {
                return false;
            }
            converter.book.write(epub);
            return true;
        } catch (Throwable e) {
            LOG.e(e, docx);
            return false;
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException e) {
                    LOG.e(e);
                }
            }
        }
    }

    private boolean run() throws Exception {
        final String main = mainDocument();
        if (main == null || zip.getEntry(main) == null) {
            return false;
        }
        readRelations(main);
        readStyles();
        readNumbering();
        footnotes.putAll(readNotes("word/footnotes.xml", "w:footnote"));
        endnotes.putAll(readNotes("word/endnotes.xml", "w:endnote"));
        readProperties();

        final InputStream in = zip.getInputStream(zip.getEntry(main));
        try {
            final XmlPullParser xpp = XmlParser.buildPullParser();
            xpp.setInput(in, null);
            int event = xpp.getEventType();
            boolean body = false;
            while (event != XmlPullParser.END_DOCUMENT) {
                if (TempHolder.get().loadingCancelled.get()) {
                    return false;
                }
                if (event == XmlPullParser.START_TAG) {
                    if ("w:body".equals(xpp.getName())) {
                        body = true;
                    } else if (body) {
                        block(XmlNode.read(xpp));
                    }
                }
                event = xpp.next();
            }
        } finally {
            in.close();
        }
        builder.finish();
        return true;
    }

    /** The main part, from the package's relations; word/document.xml when they do not say. */
    private String mainDocument() throws Exception {
        final XmlNode rels = readXml("_rels/.rels");
        if (rels != null) {
            for (XmlNode rel : rels.children()) {
                final String type = rel.attr("Type");
                if (type != null && type.endsWith("/officeDocument")) {
                    return resolve("", rel.attr("Target"));
                }
            }
        }
        return "word/document.xml";
    }

    // ---- the document ------------------------------------------------------------------------

    private void block(XmlNode node) {
        if ("w:p".equals(node.name)) {
            paragraph(node);
        } else if ("w:tbl".equals(node.name)) {
            builder.block(table(node));
        } else if ("w:sdt".equals(node.name)) {
            final XmlNode content = node.child("w:sdtContent");
            if (content != null) {
                for (XmlNode child : content.children()) {
                    block(child);
                }
            }
        } else if ("w:customXml".equals(node.name) || "w:ins".equals(node.name)
                || "w:smartTag".equals(node.name)) {
            for (XmlNode child : node.children()) {
                block(child);
            }
        }
        // w:sectPr, w:bookmarkStart and the like show nothing
    }

    private void paragraph(XmlNode p) {
        final Paragraph paragraph = toParagraph(p);
        builder.paragraph(paragraph);
        flushTextBoxes();
    }

    private void flushTextBoxes() {
        if (textBoxes.isEmpty()) {
            return;
        }
        final List<XmlNode> boxes = new ArrayList<XmlNode>(textBoxes);
        textBoxes.clear();
        for (XmlNode box : boxes) {
            for (XmlNode child : box.children()) {
                block(child);
            }
        }
    }

    private Paragraph toParagraph(XmlNode p) {
        final Paragraph paragraph = new Paragraph();
        final Paragraph.Style base = new Paragraph.Style();
        final XmlNode pPr = p.child("w:pPr");
        if (pPr != null) {
            final String styleId = pPr.childAttr("w:pStyle", "w:val");
            final StyleInfo style = style(styleId);
            paragraph.heading = headingLevel(styleId, pPr);
            if (paragraph.heading == 0 && style != null) {
                // the paragraph style's own look, as "Quote" in italics; a heading has its own
                base.bold = Boolean.TRUE.equals(resolvedBold(styleId));
                base.italic = Boolean.TRUE.equals(resolvedItalic(styleId));
            }
            paragraph.align = alignment(pPr.childAttr("w:jc", "w:val"));
            final XmlNode numPr = pPr.child("w:numPr");
            String numId = numPr == null ? null : numPr.childAttr("w:numId", "w:val");
            int level = numPr == null ? 0 : toInt(numPr.childAttr("w:ilvl", "w:val"), 0);
            if (numId == null) {
                // a list style: the numbering is the style's
                final StyleInfo listStyle = styleWithList(styleId);
                if (listStyle != null) {
                    numId = listStyle.numId;
                    level = numPr != null && numPr.child("w:ilvl") != null ? level : listStyle.ilvl;
                }
            }
            if (paragraph.heading == 0) {
                if (numId != null && !"0".equals(numId)) {
                    paragraph.listLevel = level + 1;
                    final Map<Integer, String> formats = numbering.get(numId);
                    final String format = formats == null ? null : formats.get(level);
                    paragraph.ordered = format != null && !"bullet".equals(format) && !"none".equals(format);
                }
            }
        } else {
            paragraph.heading = headingLevel(null, null);
        }
        content(p, paragraph, base);
        return paragraph;
    }

    /** The runs of a paragraph and what holds them: links, inserted text, fields, controls. */
    private void content(XmlNode parent, Paragraph paragraph, Paragraph.Style style) {
        for (XmlNode node : parent.children()) {
            final String name = node.name;
            if ("w:r".equals(name)) {
                run(node, paragraph, style);
            } else if ("w:hyperlink".equals(name)) {
                final Paragraph.Style link = style.copy();
                final String id = node.attr("r:id");
                final String target = id == null ? null : relations.get(id);
                if (target != null && target.contains(":")) {
                    link.href = target;
                }
                content(node, paragraph, link);
            } else if ("w:ins".equals(name) || "w:smartTag".equals(name) || "w:fldSimple".equals(name)
                    || "w:customXml".equals(name) || "w:dir".equals(name) || "w:bdo".equals(name)) {
                content(node, paragraph, style);
            } else if ("w:sdt".equals(name)) {
                final XmlNode inner = node.child("w:sdtContent");
                if (inner != null) {
                    content(inner, paragraph, style);
                }
            } else if ("mc:AlternateContent".equals(name)) {
                // the same content twice: the newer form and a fallback for older readers
                final XmlNode choice = node.child("mc:Choice");
                content(choice != null ? choice : node.child("mc:Fallback"), paragraph, style);
            } else if ("m:oMath".equals(name) || "m:oMathPara".equals(name)) {
                paragraph.text(mathText(node), style);
            }
            // w:del, w:moveFrom, w:pPr, bookmarks, proofing marks: nothing to show
        }
    }

    private void run(XmlNode r, Paragraph paragraph, Paragraph.Style parent) {
        final Paragraph.Style style = parent.copy();
        final XmlNode rPr = r.child("w:rPr");
        if (rPr != null) {
            final String charStyle = rPr.childAttr("w:rStyle", "w:val");
            if (charStyle != null) {
                if (Boolean.TRUE.equals(resolvedBold(charStyle))) {
                    style.bold = true;
                }
                if (Boolean.TRUE.equals(resolvedItalic(charStyle))) {
                    style.italic = true;
                }
            }
            final Boolean bold = toggle(rPr.child("w:b"));
            if (bold != null) {
                style.bold = bold;
            }
            final Boolean italic = toggle(rPr.child("w:i"));
            if (italic != null) {
                style.italic = italic;
            }
            final XmlNode u = rPr.child("w:u");
            if (u != null) {
                final String val = u.attr("w:val");
                style.underline = val == null || !"none".equals(val);
            }
            final Boolean strike = toggle(rPr.child("w:strike"));
            final Boolean dstrike = toggle(rPr.child("w:dstrike"));
            if (strike != null || dstrike != null) {
                style.strike = Boolean.TRUE.equals(strike) || Boolean.TRUE.equals(dstrike);
            }
            final String vert = rPr.childAttr("w:vertAlign", "w:val");
            if ("superscript".equals(vert)) {
                style.vert = Paragraph.VERT_SUPER;
            } else if ("subscript".equals(vert)) {
                style.vert = Paragraph.VERT_SUB;
            }
            if (toggle(rPr.child("w:vanish")) == Boolean.TRUE) {
                return; // hidden text
            }
        }
        for (XmlNode node : r.children()) {
            final String name = node.name;
            if ("w:t".equals(name)) {
                paragraph.text(node.text(), style);
            } else if ("w:tab".equals(name) || "w:ptab".equals(name)) {
                paragraph.tab();
            } else if ("w:br".equals(name) || "w:cr".equals(name)) {
                paragraph.lineBreak();
            } else if ("w:noBreakHyphen".equals(name)) {
                paragraph.text("‑", style);
            } else if ("w:softHyphen".equals(name)) {
                paragraph.text("­", style);
            } else if ("w:sym".equals(name)) {
                symbol(node, paragraph, style);
            } else if ("w:drawing".equals(name) || "w:pict".equals(name) || "w:object".equals(name)) {
                picture(node, paragraph);
            } else if ("mc:AlternateContent".equals(name)) {
                final XmlNode choice = node.child("mc:Choice");
                final XmlNode which = choice != null ? choice : node.child("mc:Fallback");
                if (which != null) {
                    for (XmlNode child : which.children()) {
                        if ("w:drawing".equals(child.name) || "w:pict".equals(child.name)) {
                            picture(child, paragraph);
                        }
                    }
                }
            } else if ("w:footnoteReference".equals(name)) {
                note(footnotes.get(node.attr("w:id")), paragraph);
            } else if ("w:endnoteReference".equals(name)) {
                note(endnotes.get(node.attr("w:id")), paragraph);
            }
            // w:instrText is a field's code, w:delText deleted text, w:fldChar a field's bounds
        }
    }

    private void symbol(XmlNode sym, Paragraph paragraph, Paragraph.Style style) {
        final String code = sym.attr("w:char");
        if (code == null) {
            return;
        }
        try {
            int c = Integer.parseInt(code, 16);
            // symbol fonts put their glyphs at F0xx; the low byte is the character in them
            if (c >= 0xF000) {
                c -= 0xF000;
            }
            if (c >= 0x20) {
                paragraph.text(String.valueOf((char) c), style);
            }
        } catch (NumberFormatException e) {
            // not a character code
        }
    }

    /** A picture, from DrawingML or VML; text in a text box is kept for after the paragraph. */
    private void picture(XmlNode node, Paragraph paragraph) {
        final XmlNode blip = node.find("a:blip");
        String id = blip == null ? null : blip.attr("r:embed");
        if (id == null) {
            final XmlNode imageData = node.find("v:imagedata");
            id = imageData == null ? null : imageData.attr("r:id");
        }
        if (id != null) {
            paragraph.image(image(id));
        }
        collectTextBoxes(node);
    }

    private void collectTextBoxes(XmlNode node) {
        for (XmlNode child : node.children()) {
            if ("w:txbxContent".equals(child.name)) {
                textBoxes.add(child);
            } else if (!"mc:Fallback".equals(child.name)) {
                collectTextBoxes(child);
            }
        }
    }

    private String image(String relationId) {
        final String target = relations.get(relationId);
        if (target == null || target.contains("://")) {
            return null; // a linked picture, not in the file
        }
        if (imageCache.containsKey(target)) {
            return imageCache.get(target);
        }
        String href = null;
        try {
            final ZipEntry entry = zip.getEntry(target);
            if (entry != null) {
                href = book.image(bytes(zip.getInputStream(entry)), target);
            }
        } catch (IOException e) {
            LOG.e(e, target);
        }
        imageCache.put(target, href);
        return href;
    }

    private void note(XmlNode note, Paragraph paragraph) {
        if (note == null) {
            return;
        }
        // text boxes met so far belong to the paragraph that calls the note, not to the note
        final List<XmlNode> before = new ArrayList<XmlNode>(textBoxes);
        textBoxes.clear();
        final StringBuilder content = new StringBuilder();
        for (XmlNode child : note.children()) {
            if ("w:p".equals(child.name)) {
                final Paragraph p = toParagraph(child);
                if (!p.isBlank()) {
                    content.append(p.toXhtml());
                }
            } else if ("w:tbl".equals(child.name)) {
                content.append(table(child));
            }
        }
        textBoxes.clear(); // a text box in a note stays out of the main text
        textBoxes.addAll(before);
        paragraph.markup(book.footnote(content.toString()));
    }

    private String table(XmlNode tbl) {
        final StringBuilder out = new StringBuilder("<table>");
        for (XmlNode row : tbl.children()) {
            if (!"w:tr".equals(row.name)) {
                continue;
            }
            out.append("<tr>");
            for (XmlNode cell : row.children()) {
                if ("w:sdt".equals(cell.name)) {
                    final XmlNode content = cell.child("w:sdtContent");
                    cell = content == null ? null : content.child("w:tc");
                }
                if (cell == null || !"w:tc".equals(cell.name)) {
                    continue;
                }
                final XmlNode tcPr = cell.child("w:tcPr");
                final int span = tcPr == null ? 1 : toInt(tcPr.childAttr("w:gridSpan", "w:val"), 1);
                out.append(span > 1 ? "<td colspan=\"" + span + "\">" : "<td>");
                for (XmlNode child : cell.children()) {
                    if ("w:p".equals(child.name)) {
                        final Paragraph p = toParagraph(child);
                        out.append(p.isBlank() ? "" : p.toXhtml());
                        // a text box in a cell goes into the cell
                        final List<XmlNode> boxes = new ArrayList<XmlNode>(textBoxes);
                        textBoxes.clear();
                        for (XmlNode box : boxes) {
                            for (XmlNode boxChild : box.children()) {
                                if ("w:p".equals(boxChild.name)) {
                                    out.append(toParagraph(boxChild).toXhtml());
                                }
                            }
                        }
                    } else if ("w:tbl".equals(child.name)) {
                        out.append(table(child));
                    }
                }
                out.append("</td>");
            }
            out.append("</tr>");
        }
        return out.append("</table>").toString();
    }

    private static String mathText(XmlNode node) {
        final StringBuilder text = new StringBuilder();
        collectMath(node, text);
        return text.toString();
    }

    private static void collectMath(XmlNode node, StringBuilder text) {
        if ("m:t".equals(node.name)) {
            text.append(node.text());
        }
        for (XmlNode child : node.children()) {
            collectMath(child, text);
        }
    }

    // ---- styles, numbering, notes, properties -----------------------------------------------

    private int headingLevel(String styleId, XmlNode pPr) {
        if (pPr != null) {
            final String outline = pPr.childAttr("w:outlineLvl", "w:val");
            if (outline != null) {
                final int level = toInt(outline, 9);
                if (level < 9) {
                    return Math.min(6, level + 1);
                }
            }
        }
        String id = styleId;
        for (int depth = 0; id != null && depth < 10; depth++) {
            final StyleInfo style = styles.get(id);
            if (style == null) {
                break;
            }
            final String name = style.name == null ? "" : style.name.toLowerCase(Locale.US);
            if (name.startsWith("heading ")) {
                return Math.max(1, Math.min(6, toInt(name.substring(8).trim(), 1)));
            }
            if ("title".equals(name)) {
                return 1;
            }
            if ("subtitle".equals(name)) {
                return 3;
            }
            if (style.outline >= 0 && style.outline < 9) {
                return Math.min(6, style.outline + 1);
            }
            id = style.basedOn;
        }
        return 0;
    }

    private StyleInfo styleWithList(String id) {
        for (int depth = 0; id != null && depth < 10; depth++) {
            final StyleInfo style = styles.get(id);
            if (style == null) {
                return null;
            }
            if (style.numId != null) {
                return style;
            }
            id = style.basedOn;
        }
        return null;
    }

    private StyleInfo style(String id) {
        return id == null ? null : styles.get(id);
    }

    private Boolean resolvedBold(String id) {
        for (int depth = 0; id != null && depth < 10; depth++) {
            final StyleInfo style = styles.get(id);
            if (style == null) {
                return null;
            }
            if (style.bold != null) {
                return style.bold;
            }
            id = style.basedOn;
        }
        return null;
    }

    private Boolean resolvedItalic(String id) {
        for (int depth = 0; id != null && depth < 10; depth++) {
            final StyleInfo style = styles.get(id);
            if (style == null) {
                return null;
            }
            if (style.italic != null) {
                return style.italic;
            }
            id = style.basedOn;
        }
        return null;
    }

    private void readStyles() throws Exception {
        final XmlNode root = readXml("word/styles.xml");
        if (root == null) {
            return;
        }
        for (XmlNode node : root.children()) {
            if (!"w:style".equals(node.name)) {
                continue;
            }
            final String id = node.attr("w:styleId");
            if (id == null) {
                continue;
            }
            final StyleInfo style = new StyleInfo();
            style.name = node.childAttr("w:name", "w:val");
            style.basedOn = node.childAttr("w:basedOn", "w:val");
            final XmlNode pPr = node.child("w:pPr");
            if (pPr != null) {
                style.outline = toInt(pPr.childAttr("w:outlineLvl", "w:val"), -1);
                final XmlNode numPr = pPr.child("w:numPr");
                if (numPr != null) {
                    style.numId = numPr.childAttr("w:numId", "w:val");
                    style.ilvl = toInt(numPr.childAttr("w:ilvl", "w:val"), 0);
                }
            }
            final XmlNode rPr = node.child("w:rPr");
            if (rPr != null) {
                style.bold = toggle(rPr.child("w:b"));
                style.italic = toggle(rPr.child("w:i"));
                final XmlNode u = rPr.child("w:u");
                if (u != null) {
                    style.underline = !"none".equals(u.attr("w:val"));
                }
            }
            styles.put(id, style);
        }
    }

    private void readNumbering() throws Exception {
        final XmlNode root = readXml("word/numbering.xml");
        if (root == null) {
            return;
        }
        final Map<String, Map<Integer, String>> abstracts = new HashMap<String, Map<Integer, String>>();
        for (XmlNode node : root.children()) {
            if ("w:abstractNum".equals(node.name)) {
                final Map<Integer, String> levels = new HashMap<Integer, String>();
                for (XmlNode lvl : node.children()) {
                    if ("w:lvl".equals(lvl.name)) {
                        levels.put(toInt(lvl.attr("w:ilvl"), 0), lvl.childAttr("w:numFmt", "w:val"));
                    }
                }
                abstracts.put(node.attr("w:abstractNumId"), levels);
            }
        }
        for (XmlNode node : root.children()) {
            if ("w:num".equals(node.name)) {
                final Map<Integer, String> levels = abstracts.get(node.childAttr("w:abstractNumId", "w:val"));
                if (levels != null) {
                    numbering.put(node.attr("w:numId"), levels);
                }
            }
        }
    }

    private Map<String, XmlNode> readNotes(String path, String element) throws Exception {
        final Map<String, XmlNode> notes = new HashMap<String, XmlNode>();
        final XmlNode root = readXml(path);
        if (root == null) {
            return notes;
        }
        for (XmlNode node : root.children()) {
            final String type = node.attr("w:type");
            if (element.equals(node.name) && (type == null || "normal".equals(type))) {
                notes.put(node.attr("w:id"), node);
            }
        }
        return notes;
    }

    private void readRelations(String main) throws Exception {
        final int slash = main.lastIndexOf('/');
        final String dir = slash < 0 ? "" : main.substring(0, slash + 1);
        final String name = main.substring(slash + 1);
        final XmlNode root = readXml(dir + "_rels/" + name + ".rels");
        if (root == null) {
            return;
        }
        for (XmlNode rel : root.children()) {
            final String target = rel.attr("Target");
            if (target == null) {
                continue;
            }
            final boolean external = "External".equals(rel.attr("TargetMode"));
            relations.put(rel.attr("Id"), external ? target : resolve(dir, target));
        }
    }

    private void readProperties() throws Exception {
        final XmlNode core = readXml("docProps/core.xml");
        if (core == null) {
            return;
        }
        final XmlNode title = core.child("dc:title");
        final XmlNode creator = core.child("dc:creator");
        final XmlNode language = core.child("dc:language");
        if (title != null) {
            book.setTitle(title.text());
        }
        if (creator != null) {
            book.setAuthor(creator.text());
        }
        if (language != null) {
            book.setLanguage(language.text());
        }
    }

    private XmlNode readXml(String path) throws Exception {
        final ZipEntry entry = zip.getEntry(path);
        if (entry == null) {
            return null;
        }
        final InputStream in = zip.getInputStream(entry);
        try {
            final XmlPullParser xpp = XmlParser.buildPullParser();
            xpp.setInput(in, null);
            int event = xpp.getEventType();
            while (event != XmlPullParser.START_TAG && event != XmlPullParser.END_DOCUMENT) {
                event = xpp.next();
            }
            return event == XmlPullParser.START_TAG ? XmlNode.read(xpp) : null;
        } finally {
            in.close();
        }
    }

    // ---- helpers -------------------------------------------------------------------------------

    /** A path in the package, from a relation's target relative to the folder of its part. */
    static String resolve(String dir, String target) {
        String path = target.startsWith("/") ? target.substring(1) : dir + target;
        final List<String> parts = new ArrayList<String>();
        for (String part : path.split("/")) {
            if (part.length() == 0 || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!parts.isEmpty()) {
                    parts.remove(parts.size() - 1);
                }
                continue;
            }
            parts.add(part);
        }
        final StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (out.length() > 0) {
                out.append('/');
            }
            out.append(part);
        }
        return out.toString();
    }

    /** An on/off property: present means on unless its value says off; absent is null. */
    private static Boolean toggle(XmlNode node) {
        if (node == null) {
            return null;
        }
        final String val = node.attr("w:val");
        return !("0".equals(val) || "false".equals(val) || "off".equals(val) || "none".equals(val));
    }

    private static int alignment(String jc) {
        if (jc == null) {
            return Paragraph.ALIGN_LEFT;
        }
        if ("center".equals(jc)) {
            return Paragraph.ALIGN_CENTER;
        }
        if ("right".equals(jc) || "end".equals(jc)) {
            return Paragraph.ALIGN_RIGHT;
        }
        if ("both".equals(jc) || "distribute".equals(jc) || "justify".equals(jc)) {
            return Paragraph.ALIGN_JUSTIFY;
        }
        return Paragraph.ALIGN_LEFT;
    }

    private static int toInt(String value, int otherwise) {
        if (value == null) {
            return otherwise;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return otherwise;
        }
    }

    static byte[] bytes(InputStream in) throws IOException {
        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final byte[] buffer = new byte[16 * 1024];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
