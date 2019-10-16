package at.stefl.svm.tosvg;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import at.stefl.commons.math.RectangleD;
import at.stefl.commons.math.vector.Vector2d;
import at.stefl.svm.object.Fraction;
import at.stefl.svm.object.basic.MapMode;
import at.stefl.svm.tosvg.SVGStateWriter.StyleCallback;

// TODO: implement and use property writer
public class TranslationUtil {
    
    // TODO: improve
    public static Vector2d transform(Vector2d point, TranslationState state) {
        MapMode mapMode = state.getMapMode();
        Fraction scaleFractionX = mapMode.getScaleX();
        Fraction scaleFractionY = mapMode.getScaleY();
        
        double scaleX = (double) scaleFractionX.getNumeratior()
                / scaleFractionX.getDenominator();
        double scaleY = (double) scaleFractionY.getNumeratior()
                / scaleFractionY.getDenominator();
        
        Vector2d scale = new Vector2d(scaleX, scaleY);
        Vector2d origin = mapMode.getOrigin().getAsVector2d();
        
        return point.mul(scale).add(origin);
    }
    
    // TODO: improve
    public static RectangleD transform(RectangleD rectangle,
            TranslationState state) {
        Vector2d leftTop = transform(rectangle.getLeftTop(), state);
        Vector2d rightBottom = transform(rectangle.getRightBottom(), state);
        
        return new RectangleD(leftTop.getX(), leftTop.getY(),
                rightBottom.getX(), rightBottom.getY());
    }
    
    // TODO: improve
    public static ArrayList<Vector2d> transform(List<Vector2d> points,
            TranslationState state) {
        ArrayList<Vector2d> result = new ArrayList<Vector2d>(points.size());
        
        for (Vector2d point : points) {
            result.add(transform(point, state));
        }
        
        return result;
    }
    
    public static void writeLineStyle(TranslationState state, Writer out)
            throws IOException {
        ColorType.STROKE.writeProperty(state.getLineColor(), out);
        out.write("vector-effect:non-scaling-stroke;");
    }
    
    public static void writeFillStyle(TranslationState state, Writer out)
            throws IOException {
        ColorType.FILL.writeProperty(state.getFillColor(), out);
    }
    
    public static void writeTextStyle(TranslationState state, Writer out)
            throws IOException {
        ColorType.FILL.writeProperty(state.getTextColor(), out);
    }
    
    public static void writeFontStyle(TranslationState state, Writer out)
            throws IOException {
        out.write("font-family:");
        out.write(state.getFontDefinition().getFamilyName());
        out.write(";");
        
        out.write("font-size:");
        out.write("" + state.getFontDefinition().getSize().getY());
        out.write(";");
    }
    
    public static StyleCallback getLineStyle(final TranslationState state) {
        return new StyleCallback() {
            @Override
            public void writeStyle(Writer out) throws IOException {
                writeLineStyle(state, out);
            }
        };
    }
    
    public static StyleCallback getPolyLineStyle(final TranslationState state) {
        return new StyleCallback() {
            @Override
            public void writeStyle(Writer out) throws IOException {
                writeLineStyle(state, out);
                out.write("fill:none;");
            }
        };
    }
    
    public static StyleCallback getShapeStyle(final TranslationState state) {
        return new StyleCallback() {
            @Override
            public void writeStyle(Writer out) throws IOException {
                writeLineStyle(state, out);
                writeFillStyle(state, out);
            }
        };
    }
    
    public static StyleCallback getTextStyle(final TranslationState state) {
        return new StyleCallback() {
            @Override
            public void writeStyle(Writer out) throws IOException {
                writeTextStyle(state, out);
                writeFontStyle(state, out);
            }
        };
    }
    
    private TranslationUtil() {}
    
}