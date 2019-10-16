package at.stefl.svm.tosvg.action;

import java.io.IOException;
import java.util.List;

import at.stefl.commons.math.vector.Vector2d;
import at.stefl.svm.object.action.PolyLineAction;
import at.stefl.svm.tosvg.SVGStateWriter;
import at.stefl.svm.tosvg.SVGUtil;
import at.stefl.svm.tosvg.TranslationState;
import at.stefl.svm.tosvg.TranslationUtil;

public class PolyLineActionTranslator extends
        SVGActionTranslator<PolyLineAction> {
    
    public static final PolyLineActionTranslator TRANSLATOR = new PolyLineActionTranslator();
    
    private PolyLineActionTranslator() {
        super(PolyLineAction.class);
    }
    
    @Override
    protected void translateImpl(PolyLineAction action, SVGStateWriter out,
            TranslationState state) throws IOException {
        List<Vector2d> points = SVGUtil.getPoints(action.getSimplePolygon());
        points = TranslationUtil.transform(points, state);
        
        out.writePolyLine(points, TranslationUtil.getPolyLineStyle(state));
    }
    
}