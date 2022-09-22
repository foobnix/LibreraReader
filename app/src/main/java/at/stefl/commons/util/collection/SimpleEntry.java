package at.stefl.commons.util.collection;

import java.util.Map.Entry;

public class SimpleEntry<K, V> extends AbstractEntry<K, V> {
    private final K key;
    private V value;
    
    public SimpleEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }
    
    public SimpleEntry(Entry<? extends K, ? extends V> entry) {
        this.key = entry.getKey();
        this.value = entry.getValue();
    }
    
    @Override
    public K getKey() {
        return key;
    }
    
    @Override
    public V getValue() {
        return value;
    }
    
    @Override
    public V setValue(V value) {
        V tmp = this.value;
        this.value = value;
        return tmp;
    }
    
}