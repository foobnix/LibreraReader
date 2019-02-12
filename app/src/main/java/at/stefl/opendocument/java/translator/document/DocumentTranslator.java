package at.stefl.opendocument.java.translator.document;

import java.io.IOException;

import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.opendocument.java.odf.OpenDocument;
import at.stefl.opendocument.java.translator.context.TranslationContext;
import at.stefl.opendocument.java.translator.settings.TranslationSettings;

public abstract class DocumentTranslator {
    
    public abstract TranslationContext getCurrentContext();
    
    // TODO: kick me
    public double getCurrentProgress() {
        TranslationContext context = getCurrentContext();
        return (context == null) ? 0 : context.getProgress();
    }
    
    // TODO: kick me
    public boolean isCurrentOutputTruncated() {
        TranslationContext context = getCurrentContext();
        return (context == null) ? false : context.isOutputTruncated();
    }
    
    public abstract void translate(OpenDocument document, LWXMLWriter out,
            TranslationSettings settings) throws IOException;
    
}