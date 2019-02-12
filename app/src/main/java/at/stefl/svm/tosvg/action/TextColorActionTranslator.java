package at.stefl.svm.tosvg.action;

import java.io.IOException;

import at.stefl.svm.object.action.TextColorAction;
import at.stefl.svm.tosvg.SVGStateWriter;
import at.stefl.svm.tosvg.TranslationState;

public class TextColorActionTranslator extends
        ColorActionTranslator<TextColorAction> {
    
    public static final TextColorActionTranslator TRANSLATOR = new TextColorActionTranslator();
    
    private TextColorActionTranslator() {
        super(TextColorAction.class);
    }
    
    @Override
    protected void translateImpl(TextColorAction action, SVGStateWriter out,
            TranslationState state) throws IOException {
        state.setTextColor(action.getColor());
    }
    
}