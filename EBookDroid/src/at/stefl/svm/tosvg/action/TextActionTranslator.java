package at.stefl.svm.tosvg.action;

import java.io.IOException;

import at.stefl.commons.math.vector.Vector2d;
import at.stefl.svm.object.action.TextAction;
import at.stefl.svm.tosvg.SVGStateWriter;
import at.stefl.svm.tosvg.TranslationState;
import at.stefl.svm.tosvg.TranslationUtil;

public class TextActionTranslator extends SVGActionTranslator<TextAction> {
    
    public static final TextActionTranslator TRANSLATOR = new TextActionTranslator();
    
    private TextActionTranslator() {
        super(TextAction.class);
    }
    
    @Override
    protected void translateImpl(TextAction action, SVGStateWriter out,
            TranslationState state) throws IOException {
        Vector2d point = action.getPoint().getAsVector2d();
        String text = action.getString().substring(action.getIndex(),
                action.getIndex() + action.getLength());
        
        point = TranslationUtil.transform(point, state);
        
        out.writeText(point, text, TranslationUtil.getTextStyle(state));
    }
    
}