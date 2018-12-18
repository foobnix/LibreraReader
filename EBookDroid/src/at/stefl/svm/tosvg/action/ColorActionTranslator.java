package at.stefl.svm.tosvg.action;

import at.stefl.svm.object.action.ColorAction;

public abstract class ColorActionTranslator<T extends ColorAction> extends
        SVGActionTranslator<T> {
    
    public ColorActionTranslator(Class<T> actionClass) {
        super(actionClass);
    }
    
}