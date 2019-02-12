package at.stefl.opendocument.java.translator.content;

import java.io.IOException;
import java.io.Writer;

import at.stefl.opendocument.java.translator.StyleScriptUtil;
import at.stefl.opendocument.java.translator.context.TranslationContext;

public abstract class DefaultContentTranslator<C extends TranslationContext>
        extends ContentTranslator<C> {
    
    public DefaultContentTranslator() {
        ParagraphTranslator paragraphTranslator = new ParagraphTranslator("p");
        addElementTranslator("text:p", paragraphTranslator);
        addElementTranslator("text:h", paragraphTranslator);
        
        addElementTranslator("text:span", new SpanTranslator());
        addElementTranslator("text:a", new LinkTranslator());
        
        addElementTranslator("text:s", new SpaceTranslator());
        addElementTranslator("text:tab", new TabTranslator());
        addElementTranslator("text:line-break", "br");
        
        addElementTranslator("draw:image", new ImageTranslator());
        addElementTranslator("draw:frame", new FrameTranslator());
    }
    
    @Override
    public void generateStyle(Writer out, C context) throws IOException {
        StyleScriptUtil.pipeStyleResource(DefaultContentTranslator.class, out);
        
        super.generateStyle(out, context);
    }
    
}