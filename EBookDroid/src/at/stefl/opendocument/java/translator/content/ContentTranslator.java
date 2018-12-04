package at.stefl.opendocument.java.translator.content;

import java.io.IOException;
import java.io.Writer;

import at.stefl.commons.lwxml.translator.LWXMLElementTranslator;
import at.stefl.commons.lwxml.translator.LWXMLHierarchyTranslator;
import at.stefl.opendocument.java.translator.ScriptGenerator;
import at.stefl.opendocument.java.translator.StyleGenerator;
import at.stefl.opendocument.java.translator.context.TranslationContext;

public abstract class ContentTranslator<C extends TranslationContext> extends
        LWXMLHierarchyTranslator<C> implements StyleGenerator<C>,
        ScriptGenerator<C> {
    
    @Override
    public void generateStyle(Writer out, C context) throws IOException {
        for (LWXMLElementTranslator<?, ?, ? super C> lwxmlTranslator : elementTranslators()) {
            if (!(lwxmlTranslator instanceof DefaultElementTranslator)) continue;
            @SuppressWarnings("unchecked")
            DefaultElementTranslator<C> translator = (DefaultElementTranslator<C>) lwxmlTranslator;
            translator.generateStyle(out, context);
        }
    }
    
    @Override
    public void generateScript(Writer out, C context) throws IOException {
        for (LWXMLElementTranslator<?, ?, ? super C> lwxmlTranslator : elementTranslators()) {
            if (!(lwxmlTranslator instanceof DefaultElementTranslator)) continue;
            @SuppressWarnings("unchecked")
            DefaultElementTranslator<C> translator = (DefaultElementTranslator<C>) lwxmlTranslator;
            translator.generateScript(out, context);
        }
    }
    
}