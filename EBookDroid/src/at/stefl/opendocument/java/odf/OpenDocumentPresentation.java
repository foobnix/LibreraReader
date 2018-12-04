package at.stefl.opendocument.java.odf;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import at.stefl.commons.lwxml.LWXMLUtil;
import at.stefl.commons.lwxml.path.LWXMLPath;
import at.stefl.commons.lwxml.reader.LWXMLFlatReader;
import at.stefl.commons.lwxml.reader.LWXMLReader;
import at.stefl.commons.lwxml.reader.LWXMLStreamReader;

public final class OpenDocumentPresentation extends OpenDocument {
    
    private static final LWXMLPath PAGE_PATH = new LWXMLPath(
            "office:document-content/office:body/office:presentation");
    private static final String PAGE_NAME_ATTRIBUTE = "draw:name";
    
    private List<String> pageNames;
    
    public OpenDocumentPresentation(OpenDocumentFile documentFile) {
        super(documentFile);
    }
    
    @Override
    public OpenDocumentType getDocumentType() {
        return OpenDocumentType.PRESENTATION;
    }
    
    public int getPageCount() throws IOException {
        return getPageNames().size();
    }
    
    // TODO: improve with path/query (0.00000000001% necessary)
    public List<String> getPageNames() throws IOException {
        if (pageNames == null) {
            LWXMLReader in = new LWXMLStreamReader(getContent());
            LWXMLUtil.flushUntilPath(in, PAGE_PATH);
            LWXMLFlatReader fin = new LWXMLFlatReader(in);
            pageNames = Collections.unmodifiableList(LWXMLUtil
                    .parseAllAttributeValues(fin, PAGE_NAME_ATTRIBUTE));
        }
        
        return pageNames;
    }
    
}