package at.stefl.svm.tosvg;

import at.stefl.svm.object.Color;
import at.stefl.svm.object.basic.FontDefinition;
import at.stefl.svm.object.basic.MapMode;

public class TranslationState {
    
    private MapMode mapMode;
    
    private FontDefinition fontDefinition;
    
    private Color lineColor;
    private Color fillColor;
    private Color textColor = new Color(0);
    
    public MapMode getMapMode() {
        return mapMode;
    }
    
    public FontDefinition getFontDefinition() {
        return fontDefinition;
    }
    
    public Color getLineColor() {
        return lineColor;
    }
    
    public Color getFillColor() {
        return fillColor;
    }
    
    public Color getTextColor() {
        return textColor;
    }
    
    public void setMapMode(MapMode mapMode) {
        this.mapMode = mapMode;
    }
    
    public void setFontDefinition(FontDefinition fontDefinition) {
        this.fontDefinition = fontDefinition;
    }
    
    public void setLineColor(Color lineColor) {
        this.lineColor = lineColor;
    }
    
    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }
    
    public void setTextColor(Color textColor) {
        if (textColor == null) throw new NullPointerException();
        
        this.textColor = textColor;
    }
    
}