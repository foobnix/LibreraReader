package at.stefl.commons.math.graph;

public class SimpleUndirectedEdge extends AbstractUndirectedEdge {
    
    private final Object vertexA;
    private final Object vertexB;
    
    public SimpleUndirectedEdge(Object vertexA, Object vertexB) {
        this.vertexA = vertexA;
        this.vertexB = vertexB;
    }
    
    @Override
    public Object getVertexA() {
        return vertexA;
    }
    
    @Override
    public Object getVertexB() {
        return vertexB;
    }
    
    @Override
    public boolean contains(Object vertex) {
        return vertex.equals(vertexA) || vertex.equals(vertexB);
    }
    
}