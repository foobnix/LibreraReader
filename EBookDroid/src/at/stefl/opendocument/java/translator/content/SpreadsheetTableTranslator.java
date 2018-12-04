package at.stefl.opendocument.java.translator.content;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import at.stefl.commons.lwxml.LWXMLEvent;
import at.stefl.commons.lwxml.LWXMLIllegalElementException;
import at.stefl.commons.lwxml.LWXMLIllegalEventException;
import at.stefl.commons.lwxml.LWXMLUtil;
import at.stefl.commons.lwxml.reader.LWXMLBranchReader;
import at.stefl.commons.lwxml.reader.LWXMLPushbackReader;
import at.stefl.commons.lwxml.reader.LWXMLReader;
import at.stefl.commons.lwxml.writer.LWXMLEventQueueWriter;
import at.stefl.commons.lwxml.writer.LWXMLFilterWriter;
import at.stefl.commons.lwxml.writer.LWXMLTeeWriter;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.commons.math.vector.Vector2i;
import at.stefl.commons.util.string.CharSequenceUtil;
import at.stefl.opendocument.java.translator.StyleScriptUtil;
import at.stefl.opendocument.java.translator.context.SpreadsheetTranslationContext;

// TODO: implement remove methods
// TODO: renew with js
public class SpreadsheetTableTranslator extends
        SpreadsheetTableElementTranslator {
    
    private static class CachedCell {
        private static CachedCell instance(
                SpreadsheetTableCellTranslator cellTranslator,
                LWXMLEventQueueWriter out) {
            return new CachedCell(cellTranslator.getCurrentRepeated(),
                    cellTranslator.getCurrentSpan(), out);
        }
        
        private final int repeat;
        private final int span;
        private final LWXMLEventQueueWriter cell;
        
        public CachedCell(int repeat, int span, LWXMLEventQueueWriter cell) {
            this.repeat = repeat;
            this.span = span;
            this.cell = cell;
        }
    }
    
    private static class StyleAlterFilter extends LWXMLFilterWriter {
        private static final String STYLE_ATTRIBUTE = "class";
        
        private static StyleAlterFilter instance(LWXMLWriter out,
                SpreadsheetTranslationContext context, String styleName) {
            String style = context.getStyle().getStyleReference(styleName);
            return new StyleAlterFilter(out, style);
        }
        
        private final String style;
        
        private int match;
        private boolean nomatch;
        private boolean styled;
        private boolean done;
        
        public StyleAlterFilter(LWXMLWriter out, String style) {
            super(out);
            
            this.style = style;
        }
        
        @Override
        public void writeEvent(LWXMLEvent event) throws IOException {
            if (!done) {
                switch (event) {
                case ATTRIBUTE_NAME:
                    nomatch = false;
                    break;
                case ATTRIBUTE_VALUE:
                    styled = (match >= STYLE_ATTRIBUTE.length());
                    break;
                case END_ATTRIBUTE_LIST:
                    if (!styled && (style != null)) out.writeAttribute(
                            STYLE_ATTRIBUTE, style);
                    done = true;
                    break;
                default:
                    break;
                }
            }
            
            out.writeEvent(event);
        }
        
        @Override
        public void write(int c) throws IOException {
            out.write(c);
            
            if (done | nomatch) return;
            
            if (getCurrentEvent() == LWXMLEvent.ATTRIBUTE_NAME) {
                if (((match + 1) <= STYLE_ATTRIBUTE.length())
                        && (c == STYLE_ATTRIBUTE.charAt(match))) {
                    match++;
                } else {
                    match = 0;
                    nomatch = true;
                }
            }
        }
        
        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            out.write(cbuf, off, len);
            
            if (done | nomatch) return;
            
            if (getCurrentEvent() == LWXMLEvent.ATTRIBUTE_NAME) {
                if (((match + len) <= STYLE_ATTRIBUTE.length())
                        && CharSequenceUtil.equals(STYLE_ATTRIBUTE, match,
                                cbuf, off, len)) {
                    match += len;
                } else {
                    match = 0;
                    nomatch = true;
                }
            }
        }
    }
    
    private static final String TABLE_ELEMENT_NAME = "table:table";
    private static final String TABLE_NAME_ATTRIBUTE_NAME = "table:name";
    private static final String SHAPES_ELEMENT_NAME = "table:shapes";
    private static final String COLUMN_ELEMENT_NAME = "table:table-column";
    private static final String ROW_ELEMENT_NAME = "table:table-row";
    private static final String CELL_ELEMENT_NAME = "table:table-cell";
    
    private final SpreadsheetTableColumnTranslator columnTranslation = new SpreadsheetTableColumnTranslator();
    private final SpreadsheetTableRowTranslator rowTranslation = new SpreadsheetTableRowTranslator();
    private final SpreadsheetTableCellTranslator cellTranslator = new SpreadsheetTableCellTranslator();
    
    private final ContentTranslator<SpreadsheetTranslationContext> contentTranslator;
    
    private Vector2i currentTableDimension;
    
    // TODO: implement collapsed list
    private final List<String> currentColumnDefaultStyles = new LinkedList<String>();
    private Iterator<String> currentColumnDefaultStylesIterator;
    
    private final LWXMLEventQueueWriter untilShapes = new LWXMLEventQueueWriter();
    
    private final LWXMLEventQueueWriter tmpRowHead = new LWXMLEventQueueWriter();
    
    public SpreadsheetTableTranslator(
            ContentTranslator<SpreadsheetTranslationContext> contentTranslator) {
        super("table");
        
        this.contentTranslator = contentTranslator;
        
        addParseAttribute(TABLE_NAME_ATTRIBUTE_NAME);
        
        addNewAttribute("border", "0");
        addNewAttribute("cellspacing", "0");
        addNewAttribute("cellpadding", "0");
    }
    
    @Override
    public void generateStyle(Writer out, SpreadsheetTranslationContext context)
            throws IOException {
        StyleScriptUtil
                .pipeStyleResource(SpreadsheetTableTranslator.class, out);
    }
    
    private void resetColumnDefaultStyle() {
        currentColumnDefaultStylesIterator = currentColumnDefaultStyles
                .iterator();
    }
    
    private String getCurrentColumnDefaultStyle() {
        String name;
        // TODO: log
        if (!currentColumnDefaultStylesIterator.hasNext()) name = null;
        else name = currentColumnDefaultStylesIterator.next();
        if (name == null) return null;
        return name;
    }
    
    private void spanCurrentColumnDefaultStyle(int span) {
        if (span < 0) throw new IllegalArgumentException();
        
        for (int i = 0; i < span; i++) {
            // TODO: log
            if (!currentColumnDefaultStylesIterator.hasNext()) return;
            if (currentColumnDefaultStylesIterator.hasNext()) currentColumnDefaultStylesIterator
                    .next();
        }
    }
    
    private void addCurrentColumnDefaultStyleName(String name, int span) {
        for (int i = 0; i < span; i++) {
            currentColumnDefaultStyles.add(name);
        }
    }
    
    private void writeRepeatCacheWriter(LinkedList<CachedCell> in,
            LWXMLWriter out, SpreadsheetTranslationContext context)
            throws IOException {
        for (CachedCell repeatIn : in) {
            for (int j = 0; j < repeatIn.repeat; j++) {
                String styleName = getCurrentColumnDefaultStyle();
                repeatIn.cell.writeTo(StyleAlterFilter.instance(out, context,
                        styleName));
                spanCurrentColumnDefaultStyle(repeatIn.span - 1);
            }
            
            resetColumnDefaultStyle();
        }
    }
    
    @Override
    public void translateStartElement(LWXMLPushbackReader in, LWXMLWriter out,
            SpreadsheetTranslationContext context) throws IOException {
        super.translateStartElement(in, untilShapes, context);
    }
    
    @Override
    public void translateAttributeList(LWXMLPushbackReader in, LWXMLWriter out,
            SpreadsheetTranslationContext context) throws IOException {
        super.translateAttributeList(in, untilShapes, context);
        
        currentTableDimension = context.getDocument().getTableDimensionMap()
                .get(getCurrentParsedAttribute(TABLE_NAME_ATTRIBUTE_NAME));
    }
    
    @Override
    public void translateEndAttributeList(LWXMLPushbackReader in,
            LWXMLWriter out, SpreadsheetTranslationContext context)
            throws IOException {
        super.translateEndAttributeList(in, untilShapes, context);
    }
    
    @Override
    public void translateChildren(LWXMLPushbackReader in, LWXMLWriter out,
            SpreadsheetTranslationContext context) throws IOException {
        translateShapes(in, out, context);
        
        untilShapes.writeTo(out);
        untilShapes.reset();
        
        // LWXMLUtil.flushUntilStartElement(in, COLUMN_ELEMENT_NAME);
        // in.unreadEvent(COLUMN_ELEMENT_NAME);
        
        // TODO: implement <table-source>
        translateColumns(in, out, context);
        translateRows(in, out, context);
        
        out.writeEndElement(elementName);
        
        currentColumnDefaultStyles.clear();
        currentColumnDefaultStylesIterator = null;
    }
    
    private void translateShapes(LWXMLPushbackReader in, LWXMLWriter out,
            SpreadsheetTranslationContext context) throws IOException {
        in.readEvent();
        String elementName = in.readValue();
        
        if (!elementName.equals(SHAPES_ELEMENT_NAME)) {
            in.unreadEvent(elementName);
            return;
        }
        
        LWXMLUtil.flushStartElement(in);
        LWXMLReader bin = new LWXMLBranchReader(in);
        
        contentTranslator.translate(bin, out, context);
    }
    
    private void translateColumns(LWXMLPushbackReader in, LWXMLWriter out,
            SpreadsheetTranslationContext context) throws IOException {
        out.writeStartElement("colgroup");
        
        loop:
        while (true) {
            LWXMLEvent event = in.readEvent();
            
            switch (event) {
            case START_ELEMENT:
                String elementName = in.readValue();
                
                if (elementName.equals(COLUMN_ELEMENT_NAME)) {
                    in.unreadEvent(elementName);
                    translateColumn(in, out, context);
                } else if (elementName.equals(ROW_ELEMENT_NAME)) {
                    in.unreadEvent(elementName);
                    break loop;
                } else {
                    LWXMLUtil.flushElement(in);
                }
                
                break;
            case END_ELEMENT:
                elementName = in.readValue();
                
                if (!elementName.equals(COLUMN_ELEMENT_NAME)) in
                        .unreadEvent(elementName);
            case END_EMPTY_ELEMENT:
                break loop;
            default:
                break;
            }
        }
        
        out.writeEndElement("colgroup");
    }
    
    private void translateColumn(LWXMLPushbackReader in, LWXMLWriter out,
            SpreadsheetTranslationContext context) throws IOException {
        columnTranslation.translate(in, out, context);
        
        addCurrentColumnDefaultStyleName(
                columnTranslation.getCurrentDefaultCellStyle(),
                columnTranslation.getCurrentSpan());
        
        if (!in.touchEvent().isEndElement()) throw new LWXMLIllegalEventException(
                in);
        columnTranslation.translate(in, out, context);
    }
    
    private void translateRows(LWXMLPushbackReader in, LWXMLWriter out,
            SpreadsheetTranslationContext context) throws IOException {
        int maxTableRows = context.getSettings().getMaxTableDimension().getY();
        int maxRows = Math.min(maxTableRows, currentTableDimension.getY());
        int rows = 0;
        
        loop:
        while (rows < currentTableDimension.getY()) {
            LWXMLEvent event = in.readEvent();
            
            switch (event) {
            case START_ELEMENT:
                String elementName = in.readValue();
                
                if (elementName.equals(ROW_ELEMENT_NAME)) {
                    if (rows >= maxTableRows) {
                        context.setOutputTruncated();
                        break loop;
                    }
                    
                    in.unreadEvent(elementName);
                    rows += translateRow(in, out, maxRows - rows, context);
                }
                
                break;
            case END_ELEMENT:
                elementName = in.readValue();
                if (elementName.equals(TABLE_ELEMENT_NAME)) return;
            default:
                break;
            }
        }
        
        LWXMLUtil.flushUntilEndElement(in, TABLE_ELEMENT_NAME);
    }
    
    private int translateRow(LWXMLPushbackReader in, LWXMLWriter out,
            int maxRepeated, SpreadsheetTranslationContext context)
            throws IOException {
        resetColumnDefaultStyle();
        
        rowTranslation.translate(in, tmpRowHead, context);
        tmpRowHead.flush();
        
        int repeat = rowTranslation.getCurrentRepeated();
        
        if (repeat == 1) {
            tmpRowHead.writeTo(out);
            translateCells(in, out, null, false, context);
            rowTranslation.translate(in, out, context);
        } else {
            LinkedList<CachedCell> tmpContent = new LinkedList<CachedCell>();
            LWXMLEventQueueWriter tmpBottom = new LWXMLEventQueueWriter();
            
            translateCells(in, null, tmpContent, true, context);
            rowTranslation.translate(in, tmpBottom, context);
            
            if (repeat > maxRepeated) {
                repeat = maxRepeated;
                context.setOutputTruncated();
            }
            
            if (context.getSettings().hasMaxRowRepetition()) {
                int maxRowRepetition = context.getSettings()
                        .getMaxRowRepetition();
                
                if (repeat > maxRowRepetition) {
                    repeat = maxRowRepetition;
                    context.setOutputTruncated();
                }
            }
            
            for (int i = 0; i < repeat; i++) {
                tmpRowHead.writeTo(out);
                writeRepeatCacheWriter(tmpContent, out, context);
                tmpBottom.writeTo(out);
            }
        }
        
        tmpRowHead.reset();
        return repeat;
    }
    
    private void translateCells(LWXMLPushbackReader in, LWXMLWriter directOut,
            LinkedList<CachedCell> cacheOut, boolean cache,
            SpreadsheetTranslationContext context) throws IOException {
        int maxTableColumns = context.getSettings().getMaxTableDimension()
                .getX();
        int maxColumns = currentTableDimension.getX();
        boolean setTruncate = false;
        
        if (maxColumns > maxTableColumns) {
            maxColumns = maxTableColumns;
            setTruncate = true;
        }
        
        int columns = 0;
        
        loop:
        while (columns < currentTableDimension.getX()) {
            LWXMLEvent event = in.readEvent();
            
            switch (event) {
            case START_ELEMENT:
                String startElementName = in.readValue();
                
                if (startElementName.equals(CELL_ELEMENT_NAME)) {
                    if (columns >= maxTableColumns) {
                        context.setOutputTruncated();
                        break loop;
                    }
                    
                    in.unreadEvent(startElementName);
                    
                    if (cache) {
                        columns += cacheCell(in, cacheOut,
                                maxColumns - columns, setTruncate, context);
                    } else {
                        columns += translateCell(in, directOut, maxColumns
                                - columns, setTruncate, context);
                    }
                } else {
                    LWXMLUtil.flushBranch(in);
                }
                
                break;
            case END_ELEMENT:
            case END_EMPTY_ELEMENT:
                String endElementName = in.readValue();
                
                if ((endElementName == null)
                        || (ROW_ELEMENT_NAME.equals(endElementName))) {
                    in.unreadEvent(endElementName);
                    return;
                } else {
                    throw new LWXMLIllegalElementException(endElementName);
                }
            default:
                // TODO: log
                break;
            }
        }
        
        LWXMLUtil.flushUntilEndElement(in, ROW_ELEMENT_NAME);
        in.unreadEvent(ROW_ELEMENT_NAME);
    }
    
    // TODO: return columns, not repeated
    // TODO: unite with cacheCell
    private int translateCell(LWXMLPushbackReader in, LWXMLWriter out,
            int maxRepeated, boolean setTruncate,
            SpreadsheetTranslationContext context) throws IOException {
        if (maxRepeated <= 0) throw new IllegalArgumentException();
        
        LWXMLEventQueueWriter tmpCellOut = new LWXMLEventQueueWriter();
        LWXMLWriter cellOut = new LWXMLTeeWriter(StyleAlterFilter.instance(out,
                context, getCurrentColumnDefaultStyle()), tmpCellOut);
        
        cellTranslator.translate(in, cellOut, context);
        int repeat = cellTranslator.getCurrentRepeated();
        
        if (repeat > maxRepeated) {
            repeat = maxRepeated;
            if (setTruncate) context.setOutputTruncated();
        }
        
        if (repeat == 1) cellOut = out;
        
        translateCellContent(in, cellOut, context);
        cellTranslator.translate(in, cellOut, context);
        
        spanCurrentColumnDefaultStyle(cellTranslator.getCurrentSpan() - 1);
        
        for (int i = 0; i < repeat - 1; i++) {
            tmpCellOut.writeTo(StyleAlterFilter.instance(out, context,
                    getCurrentColumnDefaultStyle()));
            spanCurrentColumnDefaultStyle(cellTranslator.getCurrentSpan() - 1);
        }
        
        return repeat;
    }
    
    // TODO: return columns, not repeated
    // TODO: unite with translateCell
    private int cacheCell(LWXMLPushbackReader in,
            LinkedList<CachedCell> tmpContent, int maxRepeated,
            boolean setTruncate, SpreadsheetTranslationContext context)
            throws IOException {
        if (maxRepeated <= 0) throw new IllegalArgumentException();
        
        LWXMLEventQueueWriter cellOut = new LWXMLEventQueueWriter();
        
        cellTranslator.translate(in, cellOut, context);
        int repeat = cellTranslator.getCurrentRepeated();
        
        if (repeat > maxRepeated) {
            repeat = maxRepeated;
            if (setTruncate) context.setOutputTruncated();
        }
        
        translateCellContent(in, cellOut, context);
        cellTranslator.translate(in, cellOut, context);
        
        tmpContent.add(CachedCell.instance(cellTranslator, cellOut));
        
        return repeat;
    }
    
    private void translateCellContent(LWXMLPushbackReader in, LWXMLWriter out,
            SpreadsheetTranslationContext context) throws IOException {
        if (in.touchEvent().isEndElement()) {
            out.writeStartElement("br");
            out.writeEndEmptyElement();
            return;
        }
        
        LWXMLBranchReader bin = new LWXMLBranchReader(in);
        contentTranslator.translate(bin, out, context);
        in.unreadEvent();
    }
    
    @Override
    public void translateEndElement(LWXMLPushbackReader in, LWXMLWriter out,
            SpreadsheetTranslationContext context) throws IOException {
        throw new LWXMLIllegalEventException(in);
    }
    
}