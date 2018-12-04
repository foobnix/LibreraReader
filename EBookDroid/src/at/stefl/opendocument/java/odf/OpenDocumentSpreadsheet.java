package at.stefl.opendocument.java.odf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import at.stefl.commons.lwxml.reader.LWXMLStreamReader;
import at.stefl.commons.math.vector.Vector2i;

public final class OpenDocumentSpreadsheet extends OpenDocument {
    
    private Map<String, Vector2i> tableMap;
    private List<String> tableNames;
    
    public OpenDocumentSpreadsheet(OpenDocumentFile documentFile) {
        super(documentFile);
    }
    
    @Override
    public OpenDocumentType getDocumentType() {
        return OpenDocumentType.SPREADSHEET;
    }
    
    // TODO: use metrics?
    public int getTableCount() throws IOException {
        return getTableDimensionMap().size();
    }
    
    // TODO: improve with path/query (0.00000000001% necessary)
    public Map<String, Vector2i> getTableDimensionMap() throws IOException {
        if (tableMap == null) {
            tableMap = Collections.unmodifiableMap(TableSizeUtil
                    .parseTableMap(new LWXMLStreamReader(getContent())));
        }
        
        return tableMap;
    }
    
    public List<String> getTableNames() throws IOException {
        if (tableNames == null) {
            tableNames = Collections.unmodifiableList(new ArrayList<String>(
                    getTableDimensionMap().keySet()));
        }
        
        return tableNames;
    }
    
}