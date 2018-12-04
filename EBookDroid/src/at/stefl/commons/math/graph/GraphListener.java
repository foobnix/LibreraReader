package at.stefl.commons.math.graph;

public interface GraphListener {
    
    public void vertexAdded(Object vertex);
    
    public void edgeAdded(Edge edge);
    
    public void vertexRemoved(Object vertex);
    
    public void edgeRemoved(Edge edge);
    
}