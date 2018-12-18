package at.stefl.svm.tosvg.action;

import java.io.IOException;

import at.stefl.commons.math.RectangleD;
import at.stefl.svm.object.action.RectangleAction;
import at.stefl.svm.tosvg.SVGStateWriter;
import at.stefl.svm.tosvg.TranslationState;
import at.stefl.svm.tosvg.TranslationUtil;

public class RectangleActionTranslator extends
        SVGActionTranslator<RectangleAction> {
    
    public static final RectangleActionTranslator TRANSLATOR = new RectangleActionTranslator();
    
    private RectangleActionTranslator() {
        super(RectangleAction.class);
    }
    
    @Override
    protected void translateImpl(RectangleAction action, SVGStateWriter out,
            TranslationState state) throws IOException {
        RectangleD rectangle = action.getRectangle().getAsRectangleD();
        
        rectangle = TranslationUtil.transform(rectangle, state);
        
        out.writeRectange(rectangle, TranslationUtil.getShapeStyle(state));
    }
    
}