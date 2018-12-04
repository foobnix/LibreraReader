package at.stefl.svm.tosvg.action;

import java.io.IOException;

import at.stefl.svm.object.action.FillColorAction;
import at.stefl.svm.tosvg.SVGStateWriter;
import at.stefl.svm.tosvg.TranslationState;

public class FillColorActionTranslator extends
        ColorActionTranslator<FillColorAction> {
    
    public static final FillColorActionTranslator TRANSLATOR = new FillColorActionTranslator();
    
    private FillColorActionTranslator() {
        super(FillColorAction.class);
    }
    
    @Override
    protected void translateImpl(FillColorAction action, SVGStateWriter out,
            TranslationState state) throws IOException {
        state.setFillColor(action.isSet() ? action.getColor() : null);
    }
    
}