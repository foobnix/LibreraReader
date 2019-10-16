package at.stefl.commons.math.graph;

import java.util.Collection;
import java.util.Set;

public interface Graph<V, E extends Edge> {
    
    @Override
    public String toString();
    
    @Override
    public boolean equals(Object obj);
    
    @Override
    public int hashCode();
    
    public int getVertexCount();
    
    public int getEdgeCount();
    
    public int getEdgeCount(E edge);
    
    public Set<V> getVertices();
    
    public Collection<E> getEdges();
    
    public E getEdge(Set<V> vertices);
    
    public Set<E> getConnectedEdges(V vertex);
    
    public Set<V> getConnectedVertices(V vertex);
    
    public boolean containsVertex(V vertex);
    
    public boolean containsEdge(E edge);
    
    public boolean addVertex(V vertex);
    
    public boolean addEdge(E edge);
    
    public boolean removeVertex(V vertex);
    
    public boolean removeEdge(E edge);
    
    public boolean removeAllEdges(E edge);
    
}