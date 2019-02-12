package at.stefl.opendocument.java.translator.content;

import java.io.IOException;

import at.stefl.commons.lwxml.reader.LWXMLPushbackReader;
import at.stefl.commons.lwxml.translator.LWXMLSimpleAttributeTranslator;
import at.stefl.commons.lwxml.writer.LWXMLWriter;
import at.stefl.commons.util.string.StringUtil;
import at.stefl.opendocument.java.translator.context.PresentationTranslationContext;
import at.stefl.opendocument.java.translator.style.DocumentStyle;

// TODO: improve with complex attribute translator
public class PresentationPageTranslator extends
        DefaultElementTranslator<PresentationTranslationContext> {
    
    private static final String STYLE_ATTRIBUTE = StyleAttribute.DRAW.getName();
    private static final String MASTER_PAGE_ATTRIBUTE = StyleAttribute.MASTER_PAGE
            .getName();
    
    private static final String CLASS_ATTRIBUTE = "class";
    
    public PresentationPageTranslator() {
        super("div");
        
        addParseAttribute(STYLE_ATTRIBUTE);
        addParseAttribute(MASTER_PAGE_ATTRIBUTE);
    }
    
    @Override
    public boolean addAttributeTranslator(
            String attributeName,
            LWXMLSimpleAttributeTranslator<? super PresentationTranslationContext> translator) {
        return false;
    }
    
    @Override
    public void translateAttributeList(LWXMLPushbackReader in, LWXMLWriter out,
            PresentationTranslationContext context) throws IOException {
        super.translateAttributeList(in, out, context);
        
        DocumentStyle style = context.getStyle();
        String styleName = getCurrentParsedAttribute(STYLE_ATTRIBUTE);
        String masterPage = getCurrentParsedAttribute(MASTER_PAGE_ATTRIBUTE);
        
        String classValue = StringUtil.concateNotNull(" ",
                style.getStyleReference(masterPage),
                style.getStyleReference(styleName));
        out.writeAttribute(CLASS_ATTRIBUTE, classValue);
    }
}