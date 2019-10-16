package at.stefl.commons.math.graph;

public abstract class GraphAdapter implements GraphListener {
    
    @Override
    public void vertexAdded(Object vertex) {}
    
    @Override
    public void edgeAdded(Edge edge) {}
    
    @Override
    public void vertexRemoved(Object vertex) {}
    
    @Override
    public void edgeRemoved(Edge edge) {}
    
}