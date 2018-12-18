package at.stefl.svm.tosvg.action;

import java.io.IOException;
import java.util.List;

import at.stefl.commons.math.vector.Vector2d;
import at.stefl.svm.object.action.PolygonAction;
import at.stefl.svm.tosvg.SVGStateWriter;
import at.stefl.svm.tosvg.SVGUtil;
import at.stefl.svm.tosvg.TranslationState;
import at.stefl.svm.tosvg.TranslationUtil;

public class PolygonActionTranslator extends SVGActionTranslator<PolygonAction> {
    
    public static final PolygonActionTranslator TRANSLATOR = new PolygonActionTranslator();
    
    private PolygonActionTranslator() {
        super(PolygonAction.class);
    }
    
    @Override
    protected void translateImpl(PolygonAction action, SVGStateWriter out,
            TranslationState state) throws IOException {
        List<Vector2d> points = SVGUtil.getPoints(action.getSimplePolygon());
        points = TranslationUtil.transform(points, state);
        
        out.writePolygon(points, TranslationUtil.getShapeStyle(state));
    }
    
}