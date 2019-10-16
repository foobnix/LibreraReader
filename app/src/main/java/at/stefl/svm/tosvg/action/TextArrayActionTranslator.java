package at.stefl.svm.tosvg.action;

import java.io.IOException;

import at.stefl.commons.math.vector.Vector2d;
import at.stefl.svm.object.action.TextArrayAction;
import at.stefl.svm.tosvg.SVGStateWriter;
import at.stefl.svm.tosvg.TranslationState;
import at.stefl.svm.tosvg.TranslationUtil;

public class TextArrayActionTranslator extends
        SVGActionTranslator<TextArrayAction> {
    
    public static final TextArrayActionTranslator TRANSLATOR = new TextArrayActionTranslator();
    
    private TextArrayActionTranslator() {
        super(TextArrayAction.class);
    }
    
    // TODO: implement dx
    @Override
    protected void translateImpl(TextArrayAction action, SVGStateWriter out,
            TranslationState state) throws IOException {
        Vector2d point = action.getStartPoint().getAsVector2d();
        String text = action.getString().substring(action.getIndex(),
                action.getIndex() + action.getLength());
        
        point = TranslationUtil.transform(point, state);
        
        out.writeText(point, text, TranslationUtil.getTextStyle(state));
    }
    
}