package at.stefl.svm.tosvg.action;

import java.io.IOException;

import at.stefl.svm.object.action.SVMAction;
import at.stefl.svm.tosvg.SVGStateWriter;
import at.stefl.svm.tosvg.TranslationState;

public abstract class SVGActionTranslator<T extends SVMAction> {
    
    private final Class<T> actionClass;
    
    public SVGActionTranslator(Class<T> actionClass) {
        this.actionClass = actionClass;
    }
    
    public Class<T> getActionClass() {
        return actionClass;
    }
    
    @SuppressWarnings("unchecked")
    public void translate(SVMAction action, SVGStateWriter out,
            TranslationState state) throws IOException {
        if (!actionClass.isAssignableFrom(action.getClass())) throw new IllegalArgumentException();
        translateImpl((T) action, out, state);
    }
    
    protected abstract void translateImpl(T action, SVGStateWriter out,
            TranslationState state) throws IOException;
    
}