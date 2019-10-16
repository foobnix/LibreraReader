package at.stefl.commons.math.graph;

public abstract class GenericGraphAdapter<V, E extends Edge> extends
        GraphAdapter {
    
    protected void vertexAddedGeneric(V vertex) {}
    
    @Override
    @SuppressWarnings("unchecked")
    public final void vertexAdded(Object vertex) {
        V genericVertex = (V) vertex;
        vertexAddedGeneric(genericVertex);
    }
    
    protected void edgeAddedGeneric(E edge) {}
    
    @Override
    @SuppressWarnings("unchecked")
    public final void edgeAdded(Edge edge) {
        E genericEdge = (E) edge;
        edgeAddedGeneric(genericEdge);
    }
    
    protected void vertexRemovedGeneric(V vertex) {}
    
    @Override
    @SuppressWarnings("unchecked")
    public final void vertexRemoved(Object vertex) {
        V genericVertex = (V) vertex;
        vertexAddedGeneric(genericVertex);
    }
    
    protected void edgeRemovedGeneric(E edge) {}
    
    @Override
    @SuppressWarnings("unchecked")
    public final void edgeRemoved(Edge edge) {
        E genericEdge = (E) edge;
        edgeRemovedGeneric(genericEdge);
    }
    
}