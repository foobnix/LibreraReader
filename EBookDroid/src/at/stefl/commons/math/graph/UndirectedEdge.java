package at.stefl.commons.math.graph;

import at.stefl.commons.util.collection.Multiset;

public interface UndirectedEdge extends Edge {
    
    public Object getVertexA();
    
    public Object getVertexB();
    
    @Override
    public Multiset<? extends Object> getVertices();
    
    public boolean isLoop();
    
}