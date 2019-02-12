package at.stefl.commons.math.graph;

import java.util.Set;

public interface Hyperedge extends Edge {
    
    @Override
    public Set<? extends Object> getVertices();
    
}