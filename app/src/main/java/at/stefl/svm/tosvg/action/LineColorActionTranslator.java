package at.stefl.svm.tosvg.action;

import java.io.IOException;

import at.stefl.svm.object.action.LineColorAction;
import at.stefl.svm.tosvg.SVGStateWriter;
import at.stefl.svm.tosvg.TranslationState;

public class LineColorActionTranslator extends
        ColorActionTranslator<LineColorAction> {
    
    public static final LineColorActionTranslator TRANSLATOR = new LineColorActionTranslator();
    
    private LineColorActionTranslator() {
        super(LineColorAction.class);
    }
    
    @Override
    protected void translateImpl(LineColorAction action, SVGStateWriter out,
            TranslationState state) throws IOException {
        state.setLineColor(action.isSet() ? action.getColor() : null);
    }
    
}