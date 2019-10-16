package at.stefl.commons.util.object;

public interface ObjectPreserver<S, D> extends ObjectTransformer<S, D> {
    
    public void preserve(D object, S into);
    
}