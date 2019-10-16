package at.stefl.commons.math.graph;

import java.util.Collection;

public interface Edge {
    
    @Override
    public String toString();
    
    @Override
    public boolean equals(Object obj);
    
    @Override
    public int hashCode();
    
    public int getVertexCount();
    
    public Collection<? extends Object> getVertices();
    
    public boolean contains(Object vertex);
    
}