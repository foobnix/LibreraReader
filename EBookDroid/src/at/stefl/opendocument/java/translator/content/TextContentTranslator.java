package at.stefl.opendocument.java.translator.content;

import at.stefl.opendocument.java.translator.context.TextTranslationContext;

public class TextContentTranslator extends
        DefaultTextContentTranslator<TextTranslationContext> {
    
    public TextContentTranslator() {
        ParagraphTranslator paragraphTranslator = new ParagraphTranslator("p",
                true);
        addElementTranslator("text:p", paragraphTranslator);
        addElementTranslator("text:h", paragraphTranslator);
        
        BookmarkTranslator bookmarkTranslator = new BookmarkTranslator();
        addElementTranslator(BookmarkTranslator.START, bookmarkTranslator);
        addElementTranslator(BookmarkTranslator.END, bookmarkTranslator);
        
        addElementTranslator("draw:frame", new FrameTranslator(false));
    }
    
}