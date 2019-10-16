package at.stefl.commons.math.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import at.stefl.commons.util.collection.HashMultiset;
import at.stefl.commons.util.collection.Multiset;

public class HashUndirectedGraph<V, E extends AbstractUndirectedEdge> extends
        AbstractUndirectedGraph<V, E> implements ListenableGraph<V, E> {
    
    private final Set<V> vertices = new HashSet<V>();
    private final Multiset<E> edges = new HashMultiset<E>();
    
    private final List<GraphListener> listeners = new ArrayList<GraphListener>();
    
    public HashUndirectedGraph() {}
    
    public HashUndirectedGraph(Graph<V, E> graph) {
        for (V vertex : graph.getVertices()) {
            addVertex(vertex);
        }
        
        for (E edge : graph.getEdges()) {
            addEdge(edge);
        }
    }
    
    @Override
    public synchronized int getVertexCount() {
        return vertices.size();
    }
    
    @Override
    public synchronized int getEdgeCount() {
        return edges.size();
    }
    
    @Override
    public synchronized int getEdgeCount(E edge) {
        return edges.uniqueCount(edge);
    }
    
    @Override
    public synchronized Set<V> getVertices() {
        return new HashSet<V>(vertices);
    }
    
    @Override
    public synchronized Multiset<E> getEdges() {
        return new HashMultiset<E>(edges);
    }
    
    @Override
    public boolean containsVertex(V vertex) {
        return vertices.contains(vertex);
    }
    
    @Override
    public boolean containsEdge(E edge) {
        return edges.contains(edge);
    }
    
    @Override
    public synchronized boolean addVertex(V vertex) {
        if (!vertices.add(vertex)) return false;
        fireVertexAdded(vertex);
        return true;
    }
    
    @Override
    public synchronized boolean addEdge(E edge) {
        if (!edges.add(edge)) return false;
        fireEdgeAdded(edge);
        return true;
    }
    
    @Override
    public void addListener(GraphListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    @Override
    public synchronized boolean removeVertex(V vertex) {
        if (!vertices.remove(vertex)) return false;
        
        for (E edge : new HashMultiset<E>(edges)) {
            if (edge.contains(vertex)) removeEdge(edge);
        }
        
        fireVertexRemoved(vertex);
        return true;
    }
    
    @Override
    public synchronized boolean removeEdge(E edge) {
        if (!edges.remove(edge)) return false;
        fireEdgeRemoved(edge);
        return true;
    }
    
    @Override
    public void removeListener(GraphListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    private void fireVertexAdded(V vertex) {
        synchronized (listeners) {
            for (GraphListener listener : listeners) {
                listener.vertexAdded(vertex);
            }
        }
    }
    
    private void fireEdgeAdded(E edge) {
        synchronized (listeners) {
            for (GraphListener listener : listeners) {
                listener.edgeAdded(edge);
            }
        }
    }
    
    private void fireVertexRemoved(V vertex) {
        synchronized (listeners) {
            for (GraphListener listener : listeners) {
                listener.vertexRemoved(vertex);
            }
        }
    }
    
    private void fireEdgeRemoved(E edge) {
        synchronized (listeners) {
            for (GraphListener listener : listeners) {
                listener.edgeRemoved(edge);
            }
        }
    }
    
}