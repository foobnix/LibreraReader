package at.stefl.commons.math.graph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractGraph<V, E extends AbstractEdge> implements
        Graph<V, E> {
    
    @Override
    public String toString() {
        return "V = " + getVertices() + " E = " + getEdges();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        
        if (!(obj instanceof AbstractGraph<?, ?>)) return false;
        AbstractGraph<?, ?> graph = (AbstractGraph<?, ?>) obj;
        
        return getVertices().equals(graph.getVertices())
                && getEdges().equals(graph.getEdges());
    }
    
    @Override
    public int hashCode() {
        int bits = getVertices().hashCode();
        bits += getEdges().hashCode() * 37;
        return bits;
    }
    
    @Override
    public int getVertexCount() {
        return getVertices().size();
    }
    
    @Override
    public int getEdgeCount() {
        return getEdges().size();
    }
    
    @Override
    public int getEdgeCount(E edge) {
        int result = 0;
        
        for (E e : getEdges()) {
            if (edge.equals(e)) result++;
        }
        
        return result;
    }
    
    @Override
    public E getEdge(Set<V> vertices) {
        for (E edge : getEdges()) {
            if (vertices.equals(edge.getVertices())) return edge;
        }
        
        return null;
    }
    
    @Override
    public Set<E> getConnectedEdges(V vertex) {
        Collection<E> edges = getEdges();
        Set<E> result = new HashSet<E>();
        
        for (E edge : edges) {
            Collection<? extends Object> connectedVerices = edge.getVertices();
            if (connectedVerices.contains(vertex)) result.add(edge);
        }
        
        return result;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Set<V> getConnectedVertices(V vertex) {
        Set<E> connectedEdges = getConnectedEdges(vertex);
        Set<V> result = new HashSet<V>();
        
        for (E edge : connectedEdges) {
            Collection<? extends Object> connectedVerices = edge.getVertices();
            result.addAll((Collection<? extends V>) connectedVerices);
        }
        
        result.remove(vertex);
        return result;
    }
    
    @Override
    public boolean containsVertex(V vertex) {
        return getVertices().contains(vertex);
    }
    
    @Override
    public boolean containsEdge(E edge) {
        return getEdges().contains(edge);
    }
    
    @Override
    public boolean removeAllEdges(E edge) {
        int edgeCount = getEdgeCount(edge);
        int removed = 0;
        
        for (int i = 0; i < edgeCount; i++) {
            if (removeEdge(edge)) removed++;
        }
        
        return removed > 0;
    }
    
}