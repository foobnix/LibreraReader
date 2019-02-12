package at.stefl.commons.util.collection;

import java.util.Map;
import java.util.Map.Entry;

import at.stefl.commons.util.object.ObjectUtil;

public abstract class AbstractEntry<K, V> implements Entry<K, V> {
    
    @Override
    public int hashCode() {
        return ObjectUtil.hashCode(getKey()) ^ ObjectUtil.hashCode(getValue());
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o == null) return false;
        
        if (!(o instanceof Map.Entry)) return false;
        Map.Entry<?, ?> other = (Map.Entry<?, ?>) o;
        
        return ObjectUtil.equals(getKey(), other.getKey())
                && ObjectUtil.equals(getValue(), other.getValue());
    }
    
    @Override
    public String toString() {
        return getKey() + "=" + getValue();
    }
    
    @Override
    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }
    
}