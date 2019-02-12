package at.stefl.commons.util.collection;

import java.util.Map.Entry;

import at.stefl.commons.util.object.ObjectTransformer;

public class SimpleImmutableEntry<K, V> extends SimpleEntry<K, V> {
    
    public static class Transformer<K, V> implements
            ObjectTransformer<OrderedPair<K, V>, SimpleImmutableEntry<K, V>> {
        
        @Override
        public SimpleImmutableEntry<K, V> transform(OrderedPair<K, V> source) {
            return new SimpleImmutableEntry<K, V>(source.getElement1(),
                    source.getElement2());
        }
    };
    
    public static <K, V> Transformer<K, V> getImmutableTransformer() {
        return new Transformer<K, V>();
    }
    
    public SimpleImmutableEntry(K key, V value) {
        super(key, value);
    }
    
    public SimpleImmutableEntry(Entry<? extends K, ? extends V> entry) {
        super(entry);
    }
    
    @Override
    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }
    
}