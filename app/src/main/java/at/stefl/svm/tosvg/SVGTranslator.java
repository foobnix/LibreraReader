package at.stefl.svm.tosvg;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import at.stefl.commons.io.CloseableOutputStream;
import at.stefl.commons.lwxml.writer.LWXMLStreamWriter;
import at.stefl.svm.io.SVMReader;
import at.stefl.svm.object.SVMHeader;
import at.stefl.svm.object.action.SVMAction;
import at.stefl.svm.tosvg.action.FillColorActionTranslator;
import at.stefl.svm.tosvg.action.FontActionTranslator;
import at.stefl.svm.tosvg.action.LineColorActionTranslator;
import at.stefl.svm.tosvg.action.MapModeTranslator;
import at.stefl.svm.tosvg.action.PolyLineActionTranslator;
import at.stefl.svm.tosvg.action.PolyPolygonActionTranslator;
import at.stefl.svm.tosvg.action.PolygonActionTranslator;
import at.stefl.svm.tosvg.action.RectangleActionTranslator;
import at.stefl.svm.tosvg.action.SVGActionTranslator;
import at.stefl.svm.tosvg.action.TextActionTranslator;
import at.stefl.svm.tosvg.action.TextArrayActionTranslator;
import at.stefl.svm.tosvg.action.TextColorActionTranslator;

public class SVGTranslator {
    
    public static final SVGTranslator TRANSLATOR = new SVGTranslator();
    
    private final Map<Class<? extends SVMAction>, SVGActionTranslator<? extends SVMAction>> translatorMap = new HashMap<Class<? extends SVMAction>, SVGActionTranslator<? extends SVMAction>>();
    
    private SVGTranslator() {
        addTranslator(MapModeTranslator.TRANSLATOR);
        
        addTranslator(FillColorActionTranslator.TRANSLATOR);
        addTranslator(LineColorActionTranslator.TRANSLATOR);
        addTranslator(TextColorActionTranslator.TRANSLATOR);
        
        addTranslator(RectangleActionTranslator.TRANSLATOR);
        addTranslator(PolyLineActionTranslator.TRANSLATOR);
        addTranslator(PolygonActionTranslator.TRANSLATOR);
        addTranslator(PolyPolygonActionTranslator.TRANSLATOR);
        
        addTranslator(FontActionTranslator.TRANSLATOR);
        
        addTranslator(TextActionTranslator.TRANSLATOR);
        addTranslator(TextArrayActionTranslator.TRANSLATOR);
    }
    
    public void addTranslator(
            SVGActionTranslator<? extends SVMAction> translator) {
        translatorMap.put(translator.getActionClass(), translator);
    }
    
    public void removeTranslator(Class<? extends SVMAction> actionClass) {
        translatorMap.remove(actionClass);
    }
    
    public void removeTranslator(
            SVGActionTranslator<? extends SVMAction> translator) {
        removeTranslator(translator.getActionClass());
    }
    
    public void translate(InputStream in, OutputStream out) throws IOException {
        SVMReader reader = new SVMReader(in);
        // TODO: closeable?
        SVGStateWriter writer = new SVGStateWriter(new LWXMLStreamWriter(
                new CloseableOutputStream(out)));
        
        SVMHeader header = reader.readHeader();
        
        TranslationState state = new TranslationState();
        state.setMapMode(header.getMapMode());
        
        writer.writeHeader();
        writer.writeAttribute("viewBox", "0 0 " + header.getSize().getX() + " "
                + header.getSize().getY());
        
        SVMAction action;
        while ((action = reader.readAction()) != null) {
            Class<? extends SVMAction> actionClass = action.getClass();
            SVGActionTranslator<? extends SVMAction> translator = translatorMap
                    .get(actionClass);
            if (translator == null) continue;
            translator.translate(action, writer, state);
        }
        
        writer.writeFooter();
        writer.flush();
        writer.close();
    }
    
}