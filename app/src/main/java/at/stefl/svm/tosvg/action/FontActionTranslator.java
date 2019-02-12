package at.stefl.svm.tosvg.action;

import java.io.IOException;

import at.stefl.svm.object.action.FontAction;
import at.stefl.svm.tosvg.SVGStateWriter;
import at.stefl.svm.tosvg.TranslationState;

public class FontActionTranslator extends SVGActionTranslator<FontAction> {
    
    public static final FontActionTranslator TRANSLATOR = new FontActionTranslator();
    
    private FontActionTranslator() {
        super(FontAction.class);
    }
    
    @Override
    protected void translateImpl(FontAction action, SVGStateWriter out,
            TranslationState state) throws IOException {
        state.setFontDefinition(action.getFontDefinition());
    }
    
}