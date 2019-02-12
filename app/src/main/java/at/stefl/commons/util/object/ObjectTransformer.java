package at.stefl.commons.util.object;

public interface ObjectTransformer<S, D> {
    
    public D transform(S from);
    
}