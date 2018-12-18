package at.stefl.commons.util.collection;

import java.util.Map.Entry;

import at.stefl.commons.util.object.ObjectTransformer;

public class SimpleEntry<K, V> extends AbstractEntry<K, V> {
    
    public static class Transformer<K, V> implements
            ObjectTransformer<OrderedPair<K, V>, SimpleEntry<K, V>> {
        
        @Override
        public SimpleEntry<K, V> transform(OrderedPair<K, V> source) {
            return new SimpleEntry<K, V>(source.getElement1(),
                    source.getElement2());
        }
    };
    
    public static <K, V> Transformer<K, V> getTransformer() {
        return new Transformer<K, V>();
    }
    
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