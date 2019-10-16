package at.stefl.commons.util.object;

public interface ObjectTranslator<S, D> extends ObjectTransformer<S, D> {
	
	public ObjectTranslator<D, S> invert();
	
	@Override
	public D transform(S from);
	
	public S retransform(D from);
	
}