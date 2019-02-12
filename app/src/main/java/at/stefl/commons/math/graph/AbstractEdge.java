package at.stefl.commons.math.graph;

public abstract class AbstractEdge implements Edge {
    
    @Override
    public String toString() {
        return getVertices().toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        
        if (!(obj instanceof AbstractEdge)) return false;
        AbstractEdge edge = (AbstractEdge) obj;
        
        return getVertices().equals(edge.getVertices());
    }
    
    @Override
    public int hashCode() {
        return getVertices().hashCode();
    }
    
    @Override
    public int getVertexCount() {
        return getVertices().size();
    }
    
    @Override
    public boolean contains(Object vertex) {
        return getVertices().contains(vertex);
    }
    
}