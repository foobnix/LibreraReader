package at.stefl.opendocument.java.odf;

import java.io.IOException;
import java.io.InputStream;

import at.stefl.commons.lwxml.path.LWXMLPath;

// TODO: provide OpenDocumentType?
public abstract class OpenDocument {
    
    private static final String META = "meta.xml";
    private static final String STYLES = "styles.xml";
    private static final String CONTENT = "content.xml";
    
    protected static final LWXMLPath META_DOCUMENT_STATISTICS_PATH = new LWXMLPath(
            "office:document-meta/office:meta/meta:document-statistic");
    
    private OpenDocumentFile documentFile;
    
    OpenDocument(OpenDocumentFile documentFile) {
        this.documentFile = documentFile;
    }
    
    public abstract OpenDocumentType getDocumentType();
    
    public OpenDocumentFile getDocumentFile() {
        return documentFile;
    }
    
    public InputStream getMeta() throws IOException {
        return documentFile.getFileStream(META);
    }
    
    public long getMetaSize() {
        return documentFile.getFileSize(META);
    }
    
    public InputStream getStyles() throws IOException {
        return documentFile.getFileStream(STYLES);
    }
    
    public long getStylesSize() {
        return documentFile.getFileSize(STYLES);
    }
    
    public InputStream getContent() throws IOException {
        return documentFile.getFileStream(CONTENT);
    }
    
    public long getContentSize() {
        return documentFile.getFileSize(CONTENT);
    }
    
    public OpenDocumentText getAsText() {
        return (OpenDocumentText) this;
    }
    
    public OpenDocumentSpreadsheet getAsSpreadsheet() {
        return (OpenDocumentSpreadsheet) this;
    }
    
    public OpenDocumentPresentation getAsPresentation() {
        return (OpenDocumentPresentation) this;
    }
    
}