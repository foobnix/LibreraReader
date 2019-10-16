package at.stefl.commons.util.collection;

import java.util.Map.Entry;

import at.stefl.commons.util.object.ObjectTransformer;

public class EntryTransformer<K1, V1, K2, V2> implements
        ObjectTransformer<Entry<K1, V1>, Entry<K2, V2>> {
    
    private final ObjectTransformer<? super K1, ? extends K2> keyTransformer;
    private final ObjectTransformer<? super V1, ? extends V2> valueTransformer;
    private final ObjectTransformer<? super OrderedPair<K2, V2>, ? extends Entry<K2, V2>> entryTransformer;
    
    public EntryTransformer(
            ObjectTransformer<? super K1, ? extends K2> keyTransformer,
            ObjectTransformer<? super V1, ? extends V2> valueTransformer,
            ObjectTransformer<? super OrderedPair<K2, V2>, ? extends Entry<K2, V2>> entryTransformer) {
        this.keyTransformer = keyTransformer;
        this.valueTransformer = valueTransformer;
        this.entryTransformer = entryTransformer;
    }
    
    @Override
    public Entry<K2, V2> transform(Entry<K1, V1> source) {
        K2 key = keyTransformer.transform(source.getKey());
        V2 value = valueTransformer.transform(source.getValue());
        Entry<K2, V2> entry = entryTransformer
                .transform(new OrderedPair<K2, V2>(key, value));
        return entry;
    }
    
}