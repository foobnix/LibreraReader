package at.stefl.svm.tosvg.action;

import java.io.IOException;
import java.util.List;

import at.stefl.commons.math.vector.Vector2d;
import at.stefl.commons.math.vector.Vector2i;
import at.stefl.svm.object.action.PolyPolygonAction;
import at.stefl.svm.tosvg.SVGStateWriter;
import at.stefl.svm.tosvg.SVGUtil;
import at.stefl.svm.tosvg.TranslationState;
import at.stefl.svm.tosvg.TranslationUtil;

public class PolyPolygonActionTranslator extends
        SVGActionTranslator<PolyPolygonAction> {
    
    public static final PolyPolygonActionTranslator TRANSLATOR = new PolyPolygonActionTranslator();
    
    private PolyPolygonActionTranslator() {
        super(PolyPolygonAction.class);
    }
    
    @Override
    protected void translateImpl(PolyPolygonAction action, SVGStateWriter out,
            TranslationState state) throws IOException {
        for (List<Vector2i> polygon : action.getSimplePolyPolygon()) {
            List<Vector2d> points = SVGUtil.getPoints(polygon);
            points = TranslationUtil.transform(points, state);
            
            out.writePolygon(points, TranslationUtil.getShapeStyle(state));
        }
    }
    
}