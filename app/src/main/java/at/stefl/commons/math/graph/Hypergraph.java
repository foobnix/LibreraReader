package at.stefl.commons.math.graph;

import java.util.Set;

public interface Hypergraph<V, E extends Hyperedge> extends Graph<V, E> {
    
    @Override
    public Set<E> getEdges();
    
}