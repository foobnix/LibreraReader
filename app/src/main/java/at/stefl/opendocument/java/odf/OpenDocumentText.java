package at.stefl.opendocument.java.odf;

import java.io.EOFException;
import java.io.IOException;

import at.stefl.commons.lwxml.LWXMLUtil;

public final class OpenDocumentText extends OpenDocument {
    
    private static final String PAGE_COUNT_ATTRIBUTE = "meta:page-count";
    private static final String SOFT_PAGE_BREAKS_ATTRIBUTE = "text:use-soft-page-breaks";
    
    private int pageCount = -1;
    private Boolean softPageBreaks;
    
    OpenDocumentText(OpenDocumentFile documentFile) {
        super(documentFile);
    }
    
    @Override
    public OpenDocumentType getDocumentType() {
        return OpenDocumentType.TEXT;
    }
    
    public int getPageCount() throws IOException {
        if (pageCount == -1) {
            try {
                pageCount = Integer.parseInt(LWXMLUtil.parseAttributeValue(
                        getMeta(), META_DOCUMENT_STATISTICS_PATH,
                        PAGE_COUNT_ATTRIBUTE));
            } catch (ZipEntryNotFoundException e) {
                pageCount = 0;
            } catch (EOFException e) {
                pageCount = 0;
            }
        }
        
        return pageCount;
    }
    
    public boolean isSoftPageBreaks() throws IOException {
        if (softPageBreaks == null) {
            String tmp = LWXMLUtil.parseFirstAttributeValue(getContent(),
                    SOFT_PAGE_BREAKS_ATTRIBUTE);
            if (tmp == null) softPageBreaks = false;
            else softPageBreaks = Boolean.parseBoolean(tmp);
        }
        
        return softPageBreaks;
    }
    
}