package at.stefl.opendocument.java.odf;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import at.stefl.commons.io.StreamableStringSet;
import at.stefl.commons.lwxml.LWXMLEvent;
import at.stefl.commons.lwxml.LWXMLUtil;
import at.stefl.commons.lwxml.reader.LWXMLElementDelegationReader;
import at.stefl.commons.lwxml.reader.LWXMLReader;
import at.stefl.commons.math.vector.Vector2i;
import at.stefl.commons.util.NumberUtil;
import at.stefl.commons.util.array.ArrayUtil;

public class TableSizeUtil {
    
    private static class TableDimension {
        
        private boolean empty = true;
        
        private int columns;
        private int rows;
        
        private int collapsedColumns;
        private int collapsedRows;
        
        public Vector2i getTableSize() {
            return new Vector2i(columns, rows);
        }
        
        public void setEmpty(boolean empty) {
            this.empty = empty;
        }
        
        public void setColumns(int columns) {
            this.columns = columns;
        }
        
        public void setRows(int rows) {
            this.rows = rows;
        }
        
        public void addCell(TableDimension cell) {
            if (cell.empty) {
                collapsedColumns += cell.columns;
            } else {
                empty = false;
                
                columns += collapsedColumns + cell.columns;
                
                collapsedColumns = 0;
            }
        }
        
        public void addRow(TableDimension row) {
            if (row.empty) {
                collapsedColumns = Math.max(collapsedColumns, row.columns);
                collapsedRows += row.rows;
            } else {
                empty = false;
                
                columns = Math.max(columns, row.columns);
                rows += collapsedRows + row.rows;
                
                collapsedColumns = 0;
                collapsedRows = 0;
            }
        }
    }
    
    private static final String TABLE_ELEMENT_NAME = "table:table";
    private static final String TABLE_NAME_ATTRIBUTE = "table:name";
    
    private static final String ROW_ELEMENT_NAME = "table:table-row";
    private static final String ROWS_REPEATED_ATTRIBUTE_NAME = "table:number-rows-repeated";
    
    private static final String CELL_ELEMENT_NAME = "table:table-cell";
    private static final String COLUMNS_REPEATED_ATTRIBUTE_NAME = "table:number-columns-repeated";
    private static final String COLUMNS_SPANNED_ATTRIBUTE_NAME = "table:number-columns-spanned";
    private static final StreamableStringSet CELL_ATTRIBUTES = ArrayUtil
            .toCollection(new StreamableStringSet(2),
                    COLUMNS_REPEATED_ATTRIBUTE_NAME,
                    COLUMNS_SPANNED_ATTRIBUTE_NAME);
    
    public static LinkedHashMap<String, Vector2i> parseTableMap(LWXMLReader in)
            throws IOException {
        LinkedHashMap<String, Vector2i> result = new LinkedHashMap<String, Vector2i>();
        
        @SuppressWarnings("resource")
        LWXMLElementDelegationReader din = new LWXMLElementDelegationReader(in);
        
        while (true) {
            LWXMLEvent event = din.readEvent();
            
            switch (event) {
            case START_ELEMENT:
                if (in.readValue().equals(TABLE_ELEMENT_NAME)) parseTableDimension(
                        din.getElementReader(), result);
                break;
            case END_DOCUMENT:
                return result;
            default:
                break;
            }
        }
    }
    
    private static void parseTableDimension(LWXMLReader in,
            Map<String, Vector2i> tableMap) throws IOException {
        TableDimension result = new TableDimension();
        
        String name = LWXMLUtil.parseSingleAttribute(in, TABLE_NAME_ATTRIBUTE);
        
        @SuppressWarnings("resource")
        LWXMLElementDelegationReader din = new LWXMLElementDelegationReader(in);
        
        while (true) {
            LWXMLEvent event = din.readEvent();
            
            switch (event) {
            case START_ELEMENT:
                if (!din.readValue().equals(ROW_ELEMENT_NAME)) break;
                TableDimension row = parseRow(din.getElementReader());
                result.addRow(row);
                break;
            case END_DOCUMENT:
                tableMap.put(name, result.getTableSize());
                return;
            default:
                break;
            }
        }
    }
    
    private static TableDimension parseRow(LWXMLReader in) throws IOException {
        TableDimension result = new TableDimension();
        
        int repeated = NumberUtil.parseInt(LWXMLUtil.parseSingleAttribute(in,
                ROWS_REPEATED_ATTRIBUTE_NAME), 1);
        result.setRows(repeated);
        
        @SuppressWarnings("resource")
        LWXMLElementDelegationReader din = new LWXMLElementDelegationReader(in);
        
        while (true) {
            LWXMLEvent event = din.readEvent();
            
            switch (event) {
            case START_ELEMENT:
                if (!din.readValue().equals(CELL_ELEMENT_NAME)) break;
                TableDimension cell = parseCell(din.getElementReader());
                result.addCell(cell);
                break;
            case END_DOCUMENT:
                return result;
            default:
                break;
            }
        }
    }
    
    private static TableDimension parseCell(LWXMLReader in) throws IOException {
        TableDimension result = new TableDimension();
        
        Map<String, String> attributes = LWXMLUtil.parseAttributes(in,
                CELL_ATTRIBUTES);
        int repeated = NumberUtil.parseInt(
                attributes.get(COLUMNS_REPEATED_ATTRIBUTE_NAME), 1);
        int span = NumberUtil.parseInt(
                attributes.get(COLUMNS_SPANNED_ATTRIBUTE_NAME), 1);
        int columns = repeated * span;
        
        result.setColumns(columns);
        result.setEmpty(OpenDocumentUtil.isEmptyElement(in));
        
        return result;
    }
    
    private TableSizeUtil() {}
    
}